package com.pagerduty.metrics

/**
  * In case you don't want to do metrics, wire up with this implementation which
  * is just a list of no-ops.
  */
trait NoopMetrics extends Metrics {
  def histogram(name: String, value: Int, tags: (String, String)*): Unit = {}
  def gauge(name: String, value: Long, tags: (String, String)*): Unit = {}
  def gauge(name: String, value: Double, tags: (String, String)*): Unit = {}
  def recordEvent(event: Event): Unit = {}
  def count(name: String, count: Int, tags: (String, String)*): Unit = {}
  def time[T](name: String, tags: (String, String)*)(f: => T): T = f
}

object NullMetrics extends Metrics with NoopMetrics

class NullMetrics extends Metrics with NoopMetrics
