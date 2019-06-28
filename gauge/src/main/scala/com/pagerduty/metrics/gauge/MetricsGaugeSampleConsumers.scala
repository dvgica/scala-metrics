package com.pagerduty.metrics.gauge

import com.pagerduty.metrics.Metrics

/**
  * Some very simple glue code to make GaugeReporter report to Metrics.
  */
object MetricsGaugeSampleConsumers {
  def long(metrics: Metrics, name: String, tags: (String, String)*): (Long) => Unit = { (sample: Long) =>
    metrics.gauge(name, sample, tags: _*)
  }

  def double(metrics: Metrics, name: String, tags: (String, String)*): (Double) => Unit = { (sample: Double) =>
    metrics.gauge(name, sample, tags: _*)
  }
}
