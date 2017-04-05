package com.pagerduty.metrics.gauge

trait Gauge[SampleType] {
  def sample(): SampleType
}
