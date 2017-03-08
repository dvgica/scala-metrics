[![Build Status](https://travis-ci.org/PagerDuty/scala-metrics.svg?branch=master)](https://travis-ci.org/PagerDuty/scala-metrics)

# Metrics library

This library contains a metrics API and a default implementation (sending metrics to Dogstatsd). The
API is the only dependency that other PagerDuty library projects will use, so that people can
wrap their own libraries and use that.

Also, it's probably time for a generic metrics API to do to metrics what SLF4J's API did for
logging. For now, this API does not aspire to be it - it's very Dogstatsd-oriented, because
that's what we use, but we didn't want people using our libraries to get locked in.

## Installation

- This set of libraries is published to PagerDuty Bintray OSS Maven repository. Add it to your resolvers (PD developers can skip this step):

```scala
resolvers += "bintray-pagerduty-oss-maven" at "https://dl.bintray.com/pagerduty/oss-maven"
```

- Libraries, especially open-source ones, should use the generic API artifact:

```scala
libraryDependencies += "com.pagerduty" %% "metrics-api" % VersionString
```

- Applications should depend on a specific implementation (e.g. for DataDog):

```scala
libraryDependencies += "com.pagerduty" %% "metrics-dogstatsd" % VersionString
```

#Usage




## Scheduled Gauge Checks

The `GaugeReporter` may be used to automatically strobe gauges and report values back to the metric
implementation. An application will generally only require one instance, but this is not a
restriction.  The `GaugeReporter` is thread safe and can be modified at any point of time and
shared across threads.

Gauges may be added to a reporter using the `addGauge` method, which takes a name and reading
provider. The reading provider must be thread safe and should not block for long periods of time, as
this will hold up the reporter thread.

```
val metrics: Metrics = new ...
val gauges: GaugeReporter = new GaugeReporter(metrics)
gauges.addGauges(GaugeReporter.jvmGauges)
gauges.addGauge("login.failure.count", () => loginFailureLongAdder.get())
gauges.addGauge("login.success.count", () => loginSuccessLongAdder.get())
gauges.start()

...

gauges.stop()

```

When a `GaugeProider` throws an `Exception`, it will be logged and execution will continue. In the case of a
`Throwable`, the reporting thread will die and no further readings will be performed.


### Provided Gauges

Several canned gauges are provided and may be passed to `addGauges`:

* jvmGauges - All JVM metrics listed below
* gcGauges - All garbage collector statistics
* systemGauges - Process uptime and processor count
* memoryGauges - Usage statistics of heap, non-heap, and individual pools
* threadsGauges - Thread counts and states


License
=======

Copyright 2016-2017, PagerDuty, Inc.

This work is licensed under the [Apache Software License](https://www.apache.org/licenses/LICENSE-2.0).

Contributing
============

Fork, hack, submit pull request. We will get back to you as soon as possible. Promise :)

Release
=======

Follow these steps to release a new version:
- Update version.sbt in your PR
- When the PR is approved, merge it to master, and delete the branch
- Travis will run all tests, publish to Artifactory, and create a new version tag in Github
