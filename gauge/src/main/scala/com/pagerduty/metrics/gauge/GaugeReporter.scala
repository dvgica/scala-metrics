package com.pagerduty.metrics.gauge

import org.slf4j.LoggerFactory

import java.util.concurrent.{ Executors, ScheduledExecutorService, ThreadFactory, TimeUnit }

import scala.concurrent.duration._
import scala.util.control.NonFatal

/**
  * The GaugeReporter samples Gauges and reports the result to user-provided consumers.
  *
  * Depending on the number of gauges that will be added to this Reporter, the samplePeriod of those gauges,
  * and the runtime duration of the gauges' consumers, threadPoolSize should be adjusted.
  */
class GaugeReporter(threadPoolSize: Int = 1) {
  private val log = LoggerFactory.getLogger(getClass)

  private val executor = buildGaugeExecutorService(threadPoolSize)

  /**
    * Add a gauge to be sampled at the given period. The sample returned from the gauge will be
    * provided to the given sampleConsumers.
    *
    * The gauge will be sampled immediately when this method is called.
    *
    * If running the sampleConsumers takes longer than the samplePeriod, the next sample may start late,
    * but will not run concurrently.
    */
  def addGauge[T](gauge: Gauge[T], sampleConsumers: Set[(T) => Unit], samplePeriod: FiniteDuration = 1.minute): Unit = {
    val gaugeRunnable = new Runnable {
      override def run(): Unit = {
        try {
          val sample = gauge.sample
          sampleConsumers foreach { sc =>
            try {
              sc.apply(sample)
            } catch {
              case NonFatal(e) => log.error("Error consuming gauge sample: ", e)
            }
          }
        } catch {
          case NonFatal(e) => log.error("Error sampling gauge: ", e)
        }
      }
    }

    executor.scheduleWithFixedDelay(gaugeRunnable, 0, samplePeriod.toMillis, TimeUnit.MILLISECONDS)
  }

  /**
    * Stop sampling all gauges and shutdown underlying resources.
    */
  def stop(): Unit = {
    executor.shutdownNow()
  }

  private def buildGaugeExecutorService(size: Int): ScheduledExecutorService = {
    val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(
      size,
      new ThreadFactory() {
        override def newThread(runnable: Runnable): Thread = {
          val thread = new Thread(runnable, "gauge-reporter-worker")
          thread.setDaemon(true)
          thread
        }
      }
    )
    executor
  }
}
