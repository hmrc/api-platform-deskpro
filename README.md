
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

## Running locally

Note that this service now uses internal auth to secure it's endpoints.

Use this to add an example token of '9614' when running locally (use run_local_with_dependencies.sh):

```
curl -X POST --location "http://localhost:8470/test-only/token" \
-H "content-type: application/json" \
-d "{

\"token\": \"9614\",

\"principal\": \"api-gatekeeper-frontend\",

\"permissions\": [

{ \"resourceType\": \"api-platform-deskpro\", \"resourceLocation\": \"*\", \"actions\": [ \"READ\", \"WRITE\" ] }
]
}"
```

And then call the get organisation endpoint like so:

```
curl -X GET --location "http://localhost:9614/organisation/12" -H "Authorization: 9614"
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").