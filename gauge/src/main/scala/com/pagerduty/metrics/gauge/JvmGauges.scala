package com.pagerduty.metrics.gauge

import java.lang.management.{ManagementFactory, GarbageCollectorMXBean}
import com.pagerduty.metrics.Metrics

import scala.collection.JavaConversions._

object JvmGauges {
  private val metricPattern = "\\s+".r

  def addAllGauges(implicit reporter: GaugeReporter, metrics: Metrics): Unit = {
    addSystemGauges
    addGcGauges
    addThreadGauges
    addMemoryPoolGauges
    addHeapGauges
    addNonHeapGauges
  }

  def addSystemGauges(implicit reporter: GaugeReporter, metrics: Metrics): Unit = {
    addLongGauge("jvm.system.uptime", () => ManagementFactory.getRuntimeMXBean.getUptime)
    addLongGauge("jvm.system.processors", () => ManagementFactory.getOperatingSystemMXBean.getAvailableProcessors)

    addLongGauge("jvm.compiler.time", () => ManagementFactory.getCompilationMXBean.getTotalCompilationTime)

    addLongGauge("jvm.classloader.unloaded", () => ManagementFactory.getClassLoadingMXBean.getUnloadedClassCount)
    addLongGauge("jvm.classloader.loaded", () => ManagementFactory.getClassLoadingMXBean.getLoadedClassCount)
    addLongGauge("jvm.classloader.total", () => ManagementFactory.getClassLoadingMXBean.getTotalLoadedClassCount)
  }

  def addGcGauges(implicit reporter: GaugeReporter, metrics: Metrics): Unit = {
    ManagementFactory.getGarbageCollectorMXBeans.toSeq.foreach { c: GarbageCollectorMXBean =>
      val name = metricPattern.replaceAllIn(c.getName, "-")
      addLongGauge(s"jvm.gc.${name}.count", () => c.getCollectionCount)
      addLongGauge(s"jvm.gc.${name}.time", () => c.getCollectionTime)
    }
    addLongGauge("jvm.memory.finalizer-queue.count",
                 () => ManagementFactory.getMemoryMXBean.getObjectPendingFinalizationCount)
  }

  def addThreadGauges(implicit reporter: GaugeReporter, metrics: Metrics): Unit = {
    val threads = ManagementFactory.getThreadMXBean
    addLongGauge("jvm.threads.count", () => threads.getThreadCount)
    addLongGauge("jvm.threads.peak", () => threads.getPeakThreadCount)

    addLongGauge("jvm.threads.daemon", () => threads.getDaemonThreadCount)
    addLongGauge("jvm.threads.deadlocked", () => {
      val deadlocked: Array[Long] = threads.findDeadlockedThreads
      if (deadlocked == null) 0 else deadlocked.length
    })
    addLongGauge("jvm.threads.deadlocked-monitor", () => {
      val deadlocked: Array[Long] = threads.findMonitorDeadlockedThreads
      if (deadlocked == null) 0 else deadlocked.length
    })

    val count = (state: Thread.State) =>
      threads.getThreadInfo(threads.getAllThreadIds, 0).toSeq.filter { t =>
        t.getThreadState == state
    }
    addLongGauge("jvm.threads.new", () => count(Thread.State.NEW).size)
    addLongGauge("jvm.threads.runnable", () => count(Thread.State.RUNNABLE).size)
    addLongGauge("jvm.threads.blocked", () => count(Thread.State.BLOCKED).size)
    addLongGauge("jvm.threads.waiting", () => count(Thread.State.WAITING).size)
    addLongGauge("jvm.threads.timed-waiting", () => count(Thread.State.TIMED_WAITING).size)
    addLongGauge("jvm.threads.terminated", () => count(Thread.State.TERMINATED).size)
  }

  def addMemoryPoolGauges(implicit reporter: GaugeReporter, metrics: Metrics): Unit = {
    ManagementFactory.getMemoryPoolMXBeans.toSeq.foreach { pool =>
      val name = metricPattern.replaceAllIn(pool.getName, "-")
      val usage = () => pool.getUsage
      addLongGauge(s"jvm.pools.${name}.init", () => usage().getInit)
      addLongGauge(s"jvm.pools.${name}.used", () => usage().getUsed)
      addLongGauge(s"jvm.pools.${name}.committed", () => usage().getCommitted)
      addLongGauge(s"jvm.pools.${name}.max", () => usage().getMax)
      addDoubleGauge(s"jvm.pools.${name}.usage", () => {
        val currentUsage = usage()
        ratio(currentUsage.getUsed, currentUsage.getCommitted, currentUsage.getMax)
      })
    }
  }

  def addHeapGauges(implicit reporter: GaugeReporter, metrics: Metrics): Unit = {
    val heap = () => ManagementFactory.getMemoryMXBean.getHeapMemoryUsage
    addLongGauge("jvm.memory.heap.init", () => heap().getInit)
    addLongGauge("jvm.memory.heap.used", () => heap().getUsed)
    addLongGauge("jvm.memory.heap.committed", () => heap().getCommitted)
    addLongGauge("jvm.memory.heap.max", () => heap().getMax)
    addDoubleGauge("jvm.memory.heap.usage", () => {
      val currentHeap = heap()
      ratio(currentHeap.getUsed, currentHeap.getCommitted, currentHeap.getMax)
    })
  }

  def addNonHeapGauges(implicit reporter: GaugeReporter, metrics: Metrics): Unit = {
    val nonHeap = () => ManagementFactory.getMemoryMXBean.getNonHeapMemoryUsage
    addLongGauge("jvm.memory.non-heap.init", () => nonHeap().getInit)
    addLongGauge("jvm.memory.non-heap.used", () => nonHeap().getUsed)
    addLongGauge("jvm.memory.non-heap.committed", () => nonHeap().getCommitted)
    addLongGauge("jvm.memory.non-heap.max", () => nonHeap().getMax)
    addDoubleGauge("jvm.memory.non-heap.usage", () => {
      val currentNonHeap = nonHeap()
      ratio(currentNonHeap.getUsed, currentNonHeap.getCommitted, currentNonHeap.getMax)
    })
  }

  private def ratio(used: Long, committed: Long, max: Long): Double =
    used.toDouble / Math.max(committed, max)

  private class ProviderGauge[SampleType](provider: () => SampleType) extends Gauge[SampleType] {
    def sample(): SampleType = provider()
  }

  private def addLongGauge(
      name: String,
      provider: () => Long
    )(implicit reporter: GaugeReporter,
      metrics: Metrics
    ): Unit = {
    addGauge(provider, Set(MetricsGaugeSampleConsumers.long(metrics, name)))
  }

  private def addDoubleGauge(
      name: String,
      provider: () => Double
    )(implicit reporter: GaugeReporter,
      metrics: Metrics
    ): Unit = {
    addGauge(provider, Set(MetricsGaugeSampleConsumers.double(metrics, name)))
  }

  private def addGauge[SampleType](
      provider: () => SampleType,
      consumers: Set[(SampleType) => Unit]
    )(implicit reporter: GaugeReporter
    ): Unit = {
    val gauge = new Gauge[SampleType] {
      def sample(): SampleType = provider()
    }

    reporter.addGauge(gauge, consumers)
  }
}
