
# API Platform Deskpro

Service for interfacing with Deskpro Horizon for use by API Platform.

It connects to Deskpro through the SQUID proxy.

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Tests

The tests include unit tests and integration tests.
In order to run them, use this command line:

```
./run_all_tests.sh
```

## Run the application

To run the application use the `run_local.sh` script to start the service.

Note that this service uses MongoDB, so MongoDB will have be set up locally.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").