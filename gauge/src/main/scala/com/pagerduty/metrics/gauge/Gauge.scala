package com.pagerduty.metrics.gauge

trait Gauge[SampleType] {
  /**
    * NOTE: If this `Gauge` will be `sample`d by a `GaugeReporter`, ensure that the `sample`
    * implementation is thread-safe, since it will be called from an arbitrary thread.
    */
  def sample(): SampleType
}
