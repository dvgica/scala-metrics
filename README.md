[![CircleCI](https://circleci.com/gh/PagerDuty/scala-metrics.svg?style=svg)](https://circleci.com/gh/PagerDuty/scala-metrics)

# Metrics library

This library contains a metrics API and a default implementation (sending metrics to Dogstatsd). The
API is the only dependency that other PagerDuty library projects will use, so that people can
wrap their own libraries and use that.

Also, it's probably time for a generic metrics API to do to metrics what SLF4J's API did for
logging. For now, this API does not aspire to be it - it's very Dogstatsd-oriented, because
that's what we use, but we didn't want people using our libraries to get locked in.

## Sub-Modules

- metrics-api is the generic API artifact
- metrics-dogstatsd is the DataDog implementation of the API
- metrics-gauge provides `GaugeReporter`, a class which samples `Gauge`s and reports the result. It
  also provides `JvmGauges`, and object which can add gauges for JVM stats to a given `GaugeReporter`.

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

License
=======

Copyright 2016, PagerDuty, Inc.

This work is licensed under the [Apache Software License](https://www.apache.org/licenses/LICENSE-2.0).

Contributing
============

Fork, hack, submit pull request. We will get back to you as soon as possible. Promise :)

Release
=======

Follow these steps to release a new version:
- Update version.sbt in your PR
- When the PR is approved, merge it to master, and delete the branch
- CircleCI will run all tests, publish to Artifactory, and create a new version tag in Github
