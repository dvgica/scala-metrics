package com.pagerduty.metrics

/**
  * In case you don't want to do metrics, wire up with this implementation which
  * is just a list of no-ops.
  */
object NullMetrics extends Metrics {
  override def histogram(name: String, value: Int, tags: (String, String)*): Unit = {}
  override def recordEvent(event: Event): Unit = {}
  override def count(name: String, count: Int, tags: (String, String)*): Unit = {}
}

/**
  * In case you don't want to do metrics, wire up with this implementation which
  * is just a list of no-ops.
  */
class NullMetrics extends Metrics {
  override def histogram(name: String, value: Int, tags: (String, String)*): Unit = {}
  override def recordEvent(event: Event): Unit = {}
  override def count(name: String, count: Int, tags: (String, String)*): Unit = {}
}
