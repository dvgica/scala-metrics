package com.pagerduty.metrics.pdstats

import com.timgroup.statsd.StatsDClient
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FreeSpecLike, Matchers => ScalaTestMatchers}
import org.mockito.Mockito._
import org.mockito.Matchers

class DogstatsdMetricsSpec extends FreeSpecLike with MockitoSugar with ScalaTestMatchers {

  val mockStatsd = mock[StatsDClient]

  "DogstatsdMetrics" - {
    val metrics = new DogstatsdMetrics(mockStatsd)

    val name = "some-metric"
    val tags = ("tag-name", "tag-value")

    "times a method that returns a result" in {
      val result = 123

      metrics.time(name, tags) { result } should equal(result)

      verify(mockStatsd).histogram(
        Matchers.eq(s"${name}_msec"),
        Matchers.anyLong(),
        Matchers.eq(s"${tags._1}:${tags._2}"),
        Matchers.eq("success:true")
      )
    }

    "times a method that throws an exception" in {
      val exception = new RuntimeException("test exception")

      val thrown = the[RuntimeException] thrownBy metrics.time(name, tags) { throw exception }
      thrown should equal(exception)

      verify(mockStatsd).histogram(
        Matchers.eq(s"${name}_msec"),
        Matchers.anyLong(),
        Matchers.eq(s"${tags._1}:${tags._2}"),
        Matchers.eq("success:false"),
        Matchers.eq("exceptionClass:javalangRuntimeException")
      )

    }
  }

}
