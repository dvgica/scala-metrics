package com.pagerduty.metrics

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FreeSpecLike, Matchers => ScalaTestMatchers}
import org.mockito.Mockito
import org.mockito.Mockito._
import java.time.Duration
import java.util.concurrent.{CountDownLatch, ThreadLocalRandom, TimeUnit}

class GaugeReporterSpec extends FreeSpecLike with MockitoSugar with ScalaTestMatchers {
  val metrics: Metrics = mock[Metrics]

  "GaugeReporterSpec" - {
    val name = "yay-metrics"
    val tags = Seq(("name", "value"), ("key", "value"))

    "delegates to the gauge 3 times" in {
      val result = ThreadLocalRandom.current.nextLong

      // Ew, nasty thread tests. But it's gotta happen :(
      val reporter: GaugeReporter = new GaugeReporter(metrics, Duration.ofMillis(5))

      val latch: CountDownLatch = new CountDownLatch(3)
      val provider = () => {
        latch.countDown()
        result
      }

      reporter.addGauge(name, provider, tags:_*)

      try {
        reporter.start()
        latch.await(1, TimeUnit.SECONDS)
      } finally {
        reporter.stop()
      }

      verify(metrics, Mockito.atLeast(3)).recordGauge(name, result, tags: _*)
    }
  }
}
