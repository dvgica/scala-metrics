package com.pagerduty.metrics

import java.lang.management.{ManagementFactory, MemoryPoolMXBean, GarbageCollectorMXBean}
import java.time.Duration
import java.util.Objects
import java.util.concurrent.{CopyOnWriteArrayList, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.logging.{Logger, Level => LogLevel};

import scala.collection.JavaConversions._

/**
 * Periodically polls a number of gauges and reports their values to a metrics
 * collector. Readings will not be collected until {@link #start} is invoked and
 * will not be collected after {@link #stop} is invoked.
 * <p>
 * New gauges may be added at any time by through the {@link #addGauge} method.
 * Values for these new gauges will be picked-up on the next tick (as specified
 * by the <code>delay</code> parameter.
 * <p>
 * Instances of GaugeReporter are thread safe and reusable.
 *
 * @param metrics The metric repo into which gauge readings will be sent.
 * @param delay The delay between gauge readings. Default of 60 seconds.
 * @param executor The scheduling executor to use.
 */
class GaugeReporter (
  private val metrics: Metrics,
  private val delay: Duration = Duration.ofSeconds(60),
  private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
) {
  import GaugeReporter._

  private val log: Logger = Logger.getLogger(classOf[GaugeReporter].getSimpleName())
  private val task: AtomicReference[ScheduledFuture[_]] = new AtomicReference()
  private val gauges: CopyOnWriteArrayList[Gauge] = new CopyOnWriteArrayList()

  private final class Run extends Runnable {
    override def run(): Unit = {
      gauges.forEach(new Consumer[Gauge] {
        def accept(gauge: Gauge) {
          report(gauge)
        }
      })
    }

    private def report(gauge: Gauge): Unit = {
      try {
        metrics.recordGauge(gauge._1, gauge._2(), gauge._3: _*)
      } catch {
        case ex: Exception => log.log(LogLevel.WARNING, "Caught unhandled exception collecting from gauge.", ex)
        case t: Throwable => {
          log.log(LogLevel.SEVERE, "Encountered an unhandled throwable. The system is in an unknown state. "
            + "This thread will die and no more gauges will be collected.", t)
          throw t
        }
      }
    }
  }

  /**
   * Add a new gauge to this reporter. These gauges will be picked-up on the
   * next tick.
   *
   * @param name The name of this gauge.
   * @param provider The gauge provider.
   * @param tags Optional listing of key/value tags.
   * @throws NullPointerException if name or tags are null.
   */
  def addGauge(name: String, provider: GaugeProvider, tags: (String, String)*): Unit = {
    this.gauges.add((
      Objects.requireNonNull(name, "A gauge name must be provided."),
      Objects.requireNonNull(provider, "A gauge reading provider must be provided."),
      tags.toArray
    ))
  }

  /**
   * Add all gauges to this reporter. These gauges will be picked-up on the next
   * tick.
   *
   * @param gauges A set of gauges.
   * @throws NullPointerException if name or tags of any gauge are null.
   */
  def addGauges(gauges: Seq[Gauge]): Unit = {
    gauges.foreach(g => addGauge(g._1, g._2, g._3:_*))
  }

  /**
   * Start the newly-created instance. This method may be invoked exactly once
   * at any time. Gauges that have already been added will be reported on the
   * first tick, and gauges added after starting will be reported on the next
   * tick.
   *
   * @throws IllegalArgumentException If this instance has already been started.
   */
  def start(): Unit = this.synchronized {
    if (this.task.get() != null) {
      throw new IllegalStateException("The reporter thread has already been started and may not be started again.")
    }
    this.task.lazySet(this.executor.scheduleAtFixedRate(
      new Run(),
      delay.toMillis,
      delay.toMillis,
      TimeUnit.MILLISECONDS
    ))
  }

  /**
   * Attempts to gracefully stop this instance and allow any currently-executing
   * reads to complete. If, after 1 minute, the executor has not gracefully shut
   * down, it is forcibly stopped, interrupting all in-progress threads.
   */
  def stop(): Unit = this.synchronized {
    val task: Option[ScheduledFuture[_]] = Option(this.task.get())
    task.map { _.cancel(false) }

    this.executor.shutdown()

    try {
      if (!this.executor.awaitTermination(1, TimeUnit.MINUTES)) {
        task.map { _.cancel(true) }
        this.executor.shutdownNow()
      }
    } catch {
      case ex: InterruptedException => {
        this.executor.shutdownNow()
        Thread.currentThread.interrupt()
      }
    }
  }
}

object GaugeReporter {
  type Gauge = (String, GaugeProvider, Array[(String, String)])
  type GaugeProvider = () => Long

  val formatter = java.util.regex.Pattern.compile("\\s+")

  def jvmGauges(tags: (String, String)*): Seq[Gauge] = {
    val prefix = "jvm"
    systemGauges("jvm", tags:_*) ++
      gcGauges("jvm", tags:_*) ++
      threadGauges("jvm", tags:_*) ++
      memoryGauges("jvm", tags:_*)
  }

  def systemGauges(prefix: String, tags: (String, String)*): Seq[Gauge] = {
    val tagArray = tags.toArray
    Seq(
      (prefix + ".system.uptime", () => ManagementFactory.getRuntimeMXBean.getUptime, tagArray),
      (prefix + ".system.processors", () => ManagementFactory.getOperatingSystemMXBean.getAvailableProcessors, tagArray)
    )
  }

  def gcGauges(prefix: String, tags: (String, String)*): Seq[Gauge] = {
    val tagArray = tags.toArray
    val gc: Seq[GarbageCollectorMXBean] = ManagementFactory.getGarbageCollectorMXBeans.toSeq
    gc.flatMap{ c =>
      val name = formatter.matcher(c.getName).replaceAll("-")
      Seq(
        (prefix + s".gc.${name}.count", () => c.getCollectionCount, tagArray),
        (prefix + s".gc.${name}.time", () => c.getCollectionTime, tagArray)
      )
    }
  }

  def threadGauges(prefix: String, tags: (String, String)*): Seq[Gauge] = {
    val tagArray = tags.toArray
    val threads = ManagementFactory.getThreadMXBean
    val states = threads.getThreadInfo(threads.getAllThreadIds, 0).toSeq
    Seq(
      (prefix + ".threads.count", () => threads.getThreadCount, tagArray),
      (prefix + ".threads.runnable", () => states.filter{ t => t.getThreadState == Thread.State.RUNNABLE }.size, tagArray),
      (prefix + ".threads.blocked", () => states.filter{ t => t.getThreadState == Thread.State.BLOCKED }.size, tagArray),
      (prefix + ".threads.waiting", () => states.filter{ t => t.getThreadState == Thread.State.WAITING }.size, tagArray),
      (prefix + ".threads.timed-waiting", () => states.filter{ t => t.getThreadState == Thread.State.TIMED_WAITING }.size, tagArray)
    )
  }

  def memoryGauges(prefix: String, tags: (String, String)*): Seq[Gauge] = {
    val tagArray = tags.toArray

    val poolGauges: Seq[Gauge] = ManagementFactory.getMemoryPoolMXBeans.toSeq.flatMap { pool =>
      val name = formatter.matcher(pool.getName).replaceAll("-")
      val usage = pool.getUsage
      val committed = usage.getCommitted
      val used = usage.getUsed
      val max = usage.getMax
      Seq(
        (prefix + s".pools.${name}.init", () => usage.getInit, tagArray),
        (prefix + s".pools.${name}.used", () => used, tagArray),
        (prefix + s".pools.${name}.usage", () => ratio(used, committed, max), tagArray),
        (prefix + s".pools.${name}.committed", () => committed, tagArray),
        (prefix + s".pools.${name}.max", () => max, tagArray)
      )
    }

    val memory = ManagementFactory.getMemoryMXBean
    val heap = memory.getHeapMemoryUsage
    val nonHeap = memory.getNonHeapMemoryUsage
    val memoryGauges: Seq[Gauge] = Seq(
      (prefix + ".memory.heap.init", () => heap.getInit, tagArray),
      (prefix + ".memory.heap.used", () => heap.getUsed, tagArray),
      (prefix + ".memory.heap.usage", () => ratio(heap.getUsed, heap.getCommitted, heap.getMax), tagArray),
      (prefix + ".memory.heap.committed", () => heap.getCommitted, tagArray),
      (prefix + ".memory.heap.max", () => heap.getMax, tagArray),

      (prefix + ".memory.non-heap.init", () => nonHeap.getInit, tagArray),
      (prefix + ".memory.non-heap.used", () => nonHeap.getUsed, tagArray),
      (prefix + ".memory.non-heap.usage", () => ratio(nonHeap.getUsed, nonHeap.getCommitted, nonHeap.getMax), tagArray),
      (prefix + ".memory.non-heap.committed", () => nonHeap.getCommitted, tagArray),
      (prefix + ".memory.non-heap.max", () => nonHeap.getMax, tagArray),

      (prefix + ".memory.finalizer-queue.count", () => memory.getObjectPendingFinalizationCount, tagArray)
    )

    poolGauges ++ memoryGauges
  }

  private def ratio(used: Long, committed: Long, max: Long): Long =
    (used.toDouble / Math.max(committed, max) * 100).toLong
}
