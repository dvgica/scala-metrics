package com.pagerduty.metrics.pdstats

import com.pagerduty.metrics.{Event, Metrics, Stopwatch}
import com.timgroup.statsd.{NonBlockingStatsDClient, StatsDClient}

import scala.util.{Failure, Success, Try}

/**
  * This class binds the Metrics interface to the DogStatsd Java client
  * by Indeed. Note that this is basically a straight wrapping, as the API
  * was created with this implementation in mind. Note that the default
  * constructor here expects a StatsDClient instance - this is mostly done
  * to facilitate testing, you can for example inject a NoOpStatsDClient here
  * for tests to suppress actuall logging. The constructor you should call,
  * however, is the shorter one that just takes a prefix and a set of standard tags.
  */
class DogstatsdMetrics(client: StatsDClient, standardTags: (String, String)*) extends Metrics {

  def this(prefix: String, hostname: String, port: Int, standardTags: (String, String)*) =
    this(new NonBlockingStatsDClient(prefix, hostname, port), standardTags:_*)

  def this(prefix: String, hostname: String, standardTags: (String, String)*) =
    this(prefix, hostname, DogstatsdMetrics.STATSD_DEFAULT_PORT, standardTags:_*)

  /** This should be the constructor to call - it'll talk to a default statsd on
    * localhost.
    * @param prefix The prefix to send out to statsd
    * @param standardTags A standard set of tags added to every call
    * @return A configured instance of Metrics.
    */
  def this(prefix: String, standardTags: (String, String)*) =
    this(prefix, DogstatsdMetrics.STATSD_DEFAULT_HOST, standardTags:_*)

  import DogstatsdMetrics.{convertEvent, tagsToSeq, clean}

  private def mkTags(tags: Seq[(String,String)]) = tagsToSeq(standardTags ++ tags)

  def histogram(name: String, value: Int, tags: (String, String)*): Unit =
    client.histogram(clean(name), value, mkTags(tags):_*)

  def gauge(name: String, value: Long, tags: (String, String)*): Unit = {
    client.gauge(clean(name), value, mkTags(tags):_*)
  }

  def gauge(name: String, value: Double, tags: (String, String)*): Unit = {
    client.gauge(clean(name), value, mkTags(tags):_*)
  }

  def recordEvent(event: Event): Unit =
    client.recordEvent(convertEvent(event))

  def recordEvent(event: Event, tags: (String, String)*): Unit =
    client.recordEvent(convertEvent(event), mkTags(tags):_*)

  def count(name: String, count: Int, tags: (String, String)*): Unit =
    client.count(clean(name), count, mkTags(tags):_*)

  def time[T](name: String, tags: (String, String)*)(f: => T): T = {
    val stopwatch = Stopwatch.start()
    val tryResult = Try(f)
    val durationMillis = stopwatch.elapsed().toMillis.toInt

    val generatedTags = tryResult match {
      case Success(_) => tags ++ Map("success" -> "true")
      case Failure(t) =>
        tags ++ Map(
          "success" -> "false",
          "exceptionClass" -> t.getClass.getName
        )
    }

    histogram(name + "_msec", durationMillis, generatedTags: _*)

    tryResult.get
  }


  override def stop() = client.stop()
}

object DogstatsdMetrics {
  val STATSD_DEFAULT_HOST = "localhost"
  val STATSD_DEFAULT_PORT = 8125


  /** Convert a metrics-api Event to a Dogstatsd-client Event */
  def convertEvent(event: Event): com.timgroup.statsd.Event =
    com.timgroup.statsd.Event.builder()
      .withTitle(event.title)
      .withText(event.text)
      .withDate(event.timestamp)
      .withAlertType(convertAlertType(event.alertType))
      .withPriority(convertPriority(event.priority))
      .build()

  private def convertAlertType(alertType: com.pagerduty.metrics.Event.AlertType.Value): com.timgroup.statsd.Event.AlertType =
    com.timgroup.statsd.Event.AlertType.valueOf(alertType.toString)

  private def convertPriority(priority: com.pagerduty.metrics.Event.Priority.Value): com.timgroup.statsd.Event.Priority =
    com.timgroup.statsd.Event.Priority.valueOf(priority.toString)

  /** Convert tags in the format the Java API wants it. Note that we strip stuff that Dogstatsd doesn't
    * like, as tags may be auto-generated by library code, etcetera. */
  def tagsToSeq(tags: Seq[(String, String)]): Seq[String] = {
    def strip(s: String) = s.replaceAll("""[^\w\-]""", "")
    tags.map { case (key, value) => strip(key) + ":" + strip(value) }.toSeq
  }

  // Protocol reserved characters
  private val reserved = """[:\|@#,]""".r

  /** Clean the name passed in so it doesn't use Statsd special characters. */
  private def clean(name: String) = reserved.replaceAllIn(name, "_")

}
