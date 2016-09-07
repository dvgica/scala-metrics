[![Build Status](https://travis-ci.org/PagerDuty/scala-metrics.svg?branch=master)](https://travis-ci.org/PagerDuty/scala-metrics)

Metrics library
===============

This library contains a metrics API and a default implementation (sending metrics to Dogstatsd). The
API is the only dependency that other PagerDuty library projects will use, so that people can
wrap their own libraries and use that.

Also, it's probably time for a generic metrics API to do to metrics what log4j's API did for
logging. For now, this API does not aspire to be it - it's very Dogstatsd-oriented, because
that's what we use, but we didn't want people using our libraries to get locked in.

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
- Update CHANGELOG.md in your PR
- When the PR is approved, merge it to master, and delete the branch
- Travis will run all tests, publish to Artifactory, and create a new version tag in Github
