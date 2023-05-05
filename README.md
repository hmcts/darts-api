# darts-api

# Building and deploying the application

## Prerequisites

- [Java 17](https://www.oracle.com/java)

### Environment variables
To run the functional tests locally, you will need to set an environment variable on your machine.
To do this, first we need to retrieve the key from the azure vault either my running this command in the terminal:-
```
az keyvault secret show --name GovukNotifyTestApiKey --vault-name darts-stg
```

or by logging onto the azure home page, and navigating to darts-stg and secrets etc
https://portal.azure.com/#home
Once you have the key, then run this command in the mac terminal replacing <<apikey>> with the relevant one:-
```
launchctl setenv GOVUK_NOTIFY_API_KEY <<apikey>>
```
this should set the GOVUK_NOTIFY_API_KEY environment variable. you will then need to restart intellij/terminal windows for it to take effect.

## Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application in docker

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose -f docker-compose-local.yml build
```

Run the distribution (created in `build/install/darts-api` directory)
by executing the following command:

```bash
  docker-compose -f docker-compose-local.yml up
```

This will start the API container exposing the application's port
(set to `4550` in this template app).  It will also start a postgres container
and run any new flyway migrations.  If you need to start from a clean database
you will need to delete the docker volume `darts-api_darts-db`

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4550/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

