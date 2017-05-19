package com.pagerduty.metrics

/**
  * A trait to describe something that can collect metrics. It should be minimal
  * enough to make building an implementation for any metrics collection system
  * (statsd, collectd, ...) trivial. At PagerDuty, we use Datadog, so the API
  * is pretty DD-centric.
  */
trait Metrics {

  /**
    * Add a measurement. It supports sending extra tags (like whether
    * an operation was succesful or not) for DogStatsD and other metrics
    * collectors that support tagging. The idea is that these get combined with
    * default tags that get setup in the constructor of implementing classes.
    *
    * @param name Name of the metric
    * @param value Value of the metric (time, ...)
    * @param tags Extra tags, e.g. for DogStatsD.
    */
  def histogram(name: String, value: Int, tags: (String, String)*): Unit

  /**
    * Increment a counter by `count`. It supports sending extra tags (like whether
    * an operation was succesful or not) for DogStatsD and other metrics
    * collectors that support tagging. The idea is that these get combined with
    * default tags that get setup in the constructor of implementing classes.
    *
    * @param name Name of the counter
    * @param count How much to increment the value by
    * @param tags Extra tags, e.g. for DogStatsD
    */
  def count(name: String, count: Int, tags: (String, String)*): Unit

  /**
    * Increment the counter by one.
    *
    * @see count(String,Int,(String,String)*)
    */
  def increment(name: String, tags: (String, String)*): Unit = count(name, 1, tags: _*)

  /**
    * Records the current reading of a gauge, which represents a point-in-time
    * reading of a measurable thing.
    *
    * @param name Name of the gauge
    * @param value The current gauge value
    * @param tags Extra tags
    */
  def gauge(name: String, value: Long, tags: (String, String)*): Unit
  def gauge(name: String, value: Double, tags: (String, String)*): Unit

  /**
    * Record an event. Events are things that happen that - in Datadog for example -
    * you can overlay over a graph.
    *
    * @param event
    */
  def recordEvent(event: Event): Unit

  /**
    * Time the provided function as the named metric, tagging success and failure automatically.
    *
    * @param name Name of the metric
    * @param tags Extra tags, e.g. for DogStatsD
    * @param f Function to time
    */
  def time[T](name: String, tags: (String, String)*)(f: => T): T

  /**
    * If your implementation needs it, you can override this to handle a signal
    * to stop collecting metrics (close sockets, stop threads collecting gauges,
    * etcetera. The default implementation is a no-op.
    */
  def stop(): Unit = {}
}

/**
  * An event. This is largely modelled after Datadog events and completely "borrowed" from
  * Indeed's client library for DogStatsD.
  *
  * @param title Title of the event
  * @param text  Description of the event
  * @param timestamp Time in milliseconds sinds the Unix Epoch the event occurred
  * @param alertType The kind of event
  * @param priority The priority of the event
  */
case class Event(
    title: String,
    text: String,
    timestamp: Long,
    alertType: Event.AlertType.Value,
    priority: Event.Priority.Value)

object Event {

  object Priority extends Enumeration {
    type Priority = Value
    val LOW, NORMAL = Value
  }

  object AlertType extends Enumeration {
    type AlertType = Value
    val ERROR, WARNING, INFO, SUCCESS = Value
  }
}
