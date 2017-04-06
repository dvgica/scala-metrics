package com.pagerduty.metrics.gauge

import org.scalatest.Matchers
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpecLike
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._

class GaugeReporterSpec extends FreeSpecLike with MockFactory with Eventually with Matchers {

  val sampleException = new Exception("test exception")
  val testSample = 12345

  class TestGauge extends Gauge[Int] {
    var numSamples = 0

    def sample(): Int = {
      numSamples += 1
      if (numSamples == 2) {
        throw sampleException
      } else {
        testSample
      }
    }
  }

  "A GaugeReporter" - {
    val gaugeReporter = new GaugeReporter

    "periodically sample a gauge, reporting results and ignoring failures" in {
      val gauge = new TestGauge
      val consumer1 = mockFunction[Int, Unit]
      val consumer2 = mockFunction[Int, Unit]
      val consumer3 = mockFunction[Int, Unit]

      // there's a bit of variability on the number of times the gauge is actually sampled
      consumer1.expects(testSample).repeat(4 to 5)
      consumer2.expects(testSample).repeat(4 to 5).throws(new RuntimeException("simulated exception"))
      consumer3.expects(testSample).repeat(4 to 5)

      gaugeReporter.addGauge[Int](gauge, Set(consumer1, consumer2, consumer3), 200.milliseconds)

      eventually(timeout(1000.milliseconds)) {
        gauge.numSamples should be >= 5 // again, could be 6 in some cases
      }

      gaugeReporter.stop
    }
  }

}
