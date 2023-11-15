# darts-api

# Modernised DARTS SwaggerUI

* To View the embedded Swagger UI in Github Pages: https://hmcts.github.io/darts-api/
* The settings for the repository uses the master branch for Github Pages.
* Use the top bar to "Select a definition" e.g. Transcriptions
* The `darts-api/index.html` file should be updated to include any new OAS definitions in the urls parameter under
  SwaggerUIBundle.
* The application [swagger-ui](http://localhost:4550/swagger-ui/index.html) can be used for "Try it out" functionality
  using the Authorization Button Padlock and presenting a valid access token (JWT).

# Building and deploying the application

## Prerequisites

- [Java 17](https://www.oracle.com/java)

### Environment variables

To run the functional tests locally, you must set the following environment variables on your machine.
The required value of each variable is stored in Azure Key Vault as a Secret.

| Environment Variable Name        | Corresponding Azure Key Vault Secret Name |
|----------------------------------|-------------------------------------------|
| GOVUK_NOTIFY_API_KEY             | GovukNotifyTestApiKey                     |
| FUNC_TEST_ROPC_USERNAME          | api-FUNC-TEST-ROPC-USERNAME               |
| FUNC_TEST_ROPC_PASSWORD          | api-FUNC-TEST-ROPC-PASSWORD               |
| AAD_B2C_TENANT_ID_KEY            | AzureAdB2CTenantIdKey                     |
| AAD_B2C_CLIENT_ID_KEY            | AzureAdB2CClientIdKey                     |
| AAD_B2C_CLIENT_SECRET_KEY        | AzureAdB2CClientSecretKey                 |
| AAD_B2C_ROPC_CLIENT_ID_KEY       | AzureAdB2CFuncTestROPCClientIdKey         |
| AAD_B2C_ROPC_CLIENT_SECRET_KEY   | AzureAdB2CFuncTestROPCClientSecretKey     |
| AZURE_STORAGE_CONNECTION_STRING  | AzureStorageConnectionString              |
| AAD_TENANT_ID                    | AzureADTenantId                           |
| AAD_CLIENT_ID                    | AzureADClientId                           |
| AAD_CLIENT_SECRET                | AzureADClientSecret                       |
| XHIBIT_USER_NAME                 | XhibitUserName                            |
| XHIBIT_PASSWORD                  | XhibitPassword                            |
| CPP_USER_NAME                    | CppUserName                               |
| CPP_PASSWORD                     | CppPassword                               |
| DARPC_USER_NAME                  | DarPcUserName                             |
| DARPC_PASSWORD                   | DarPcPassword                             |
| SYSTEM_USER_EMAIL                | DartsSystemUserEmail                      |
| DAR_MIDTIER_USER_NAME            | DarMidTierUserName                        |
| DAR_MIDTIER_PASSWORD             | DarMidTierPassword                        |
| AZURE_AD_FUNCTIONAL_TEST_GLOBAL_USERNAME  | AzureAdB2CFuncTestROPCGlobalUsername      |
| AZURE_AD_FUNCTIONAL_TEST_GLOBAL_PASSWORD  | AzureAdB2CFuncTestROPCGlobalPassword      |

To obtain the secret value, you may retrieve the keys from the Azure Vault by running the `az keyvault secret show`
command in the terminal. E.g. to obtain the value for `GOVUK_NOTIFY_API_KEY`, you should run:

```
az keyvault secret show --name GovukNotifyTestApiKey --vault-name darts-stg
```

and inspect the `"value"` field of the response.

Alternatively, you can log into the [Azure home page](https://portal.azure.com/#home), and navigate to
`Key Vault -> darts-stg -> Secrets`. Note in your Portal Settings you must have the `CJS Common Platform` directory
active for the secrets to be visible.

Once you have obtained the values, set the environment variables on your system. E.g. On Mac, you may run this command
in the terminal, replacing `<<env var name>>` and `<<secret value>>` as necessary:

```
launchctl setenv <<env var name>> <<secret value>>
```

You will then need to restart intellij/terminal windows for it to take effect.

to make the changes permanent, make a `.zshrc` file in your users folder and populate it with this and their values:

```
export GOVUK_NOTIFY_API_KEY=
export FUNC_TEST_ROPC_USERNAME=
export FUNC_TEST_ROPC_PASSWORD=
export AAD_B2C_TENANT_ID_KEY=
export AAD_B2C_CLIENT_ID_KEY=
export AAD_B2C_CLIENT_SECRET_KEY=
export AAD_B2C_ROPC_CLIENT_ID_KEY=
export AAD_B2C_ROPC_CLIENT_SECRET_KEY=
export AAD_TENANT_ID=
export AAD_CLIENT_ID=
export AAD_CLIENT_SECRET=
export XHIBIT_USER_NAME=
export XHIBIT_PASSWORD=
export CPP_USER_NAME=
export CPP_PASSWORD=
export DARPC_USER_NAME=
export DARPC_PASSWORD=
export SYSTEM_USER_EMAIL=
export DAR_MIDTIER_USER_NAME=
export DAR_MIDTIER_PASSWORD=
export AZURE_AD_FUNCTIONAL_TEST_GLOBAL_USERNAME=
export AZURE_AD_FUNCTIONAL_TEST_GLOBAL_PASSWORD=
```

### Storage Account

Some functional tests require a storage account to complete. Locally, this can be achieved by installing and running the
Azurite open-source emulator which provides a free local environment for testing any Azure Blob, Queue or Table storage.

#### Install Azurite

Use DockerHub to pull the latest Azurite image by using the following command:

```
docker pull mcr.microsoft.com/azure-storage/azurite
```

#### Run Azurite

The following command runs the Azurite Docker image. The -p 10000:10000 parameter redirects requests from host machine's
port 10000 to the Docker instance.

```
docker run -p 10000:10000 -p 10001:10001 -p 10002:10002 \
    mcr.microsoft.com/azure-storage/azurite
```

#### Connection String Configuration

The application obtains the connection string from the key vault. However, locally the default Azurite account details
are required. Therefore, you will need to add its connection string as an environment variable .

Environment Variable Name: AZURE_STORAGE_CONNECTION_STRING

Environment Variable Value:

```
DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey={DEFAULT_ACCOUNT_KEY};BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
```

Replace the {DEFAULT_ACCOUNT_KEY} with the value provided in the following
link: https://github.com/Azure/Azurite#default-storage-account

## Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Jacoco Coverage Report

A local jacoco coverage report can be generated using the following command:-

```bash
  ./gradlew jacocoTestReport
```

The report will be available under ./build/jacocoHtml/index.html. The report incorporates both unit test
and integration test coverage

### Running the application in docker (without darts-gateway & darts-stub-services)

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose -f docker-compose-local.yml build
```

Run the distribution (created in `build/install/darts-api` directory) by executing the following command:

```bash
  docker-compose -f docker-compose-local.yml up darts-api darts-db darts-redis
```

This will start the API container exposing the application's port
(set to `4550` in this template app). It will also start a postgres container
and run any new flyway migrations. If you need to start from a clean database
you will need to delete the docker volume `darts-api_darts-db`

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:4550/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Running the application in docker with darts-gateway & darts-stub-services

Currently, to run the full suite of services we need to check out and build the darts-gateway
and darts-stub-services. A convenience script has been added `./bin/dcup` to automate this.

To run all services use:

```bash
./bin/dcup
```

If you want to run the darts-api outside of docker and run only the dependant services in docker use:

```bash
./bin/dcup noapi
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

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or
any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by
this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.

## Spring Profiles

The following Spring Profiles are defined. "External Components" are defined as any service upon which the application
is dependent, such as database servers, web services etc.

| Profile          | Config Location                                                | Purpose                                                                                        | External Components                                                                                                                                                                                                             |
|------------------|----------------------------------------------------------------|------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `local`          | `src/main/resources/application-local.yaml`                    | For running the application locally as a docker compose stack with `docker-compose-local.yml`. | Provided as needed by `docker-compose-local.yml`. No external connectivity permitted outside the network boundary of the stack.                                                                                                 |
| `intTest`        | `src/integrationTest/resources/application-intTest.yaml`       | For running integration tests under `src/integrationTest`.                                     | No interaction required or permitted, all external calls are mocked via embedded wiremock (for HTTP requests), an embedded database (for db queries) or `@MockBeans` for anything else. Spring Security is explicitly disabled. |
| `functionalTest` | `src/functionalTest/resources/application-functionalTest.yaml` | For running functional tests under `src/functionalTest`.                                       | Functional tests execute API calls against the application deployed in the PR environment. That application is deployed with the `dev` profile (see below).                                                                     |
| `dev`            | `src/main/resources/application-dev.yaml`                      | For running the application in the Pull Request (dev) environment.                             | Interaction permitted with "real" components, which may be services deployed to a test environment.                                                                                                                             |

## Functional testing

The functional tests module is run by default in the dev and staging environments. Unlike the integration tests the
functional tests will hit the deployed darts-api and postgres database. This requires some management of the data
created by these tests. To this end the following conventions should be used:

- If a courthouse needs to pre-exist for a functional test it can be created from within the tests
  using `/functional-tests/courthouse/{courthouse_name}/courtroom/{courtroom_name}`. The courthouse_name must be
  prefixed with `func-`. This data will be cleaned after the test has executed.
- If a case needs to pre-exist for a functional test then however it is created the case_number should also pre-fixed
  with `func-`. There is a random case_number generator that will provide case_numbers with this prefix. These cases and
  their associated hearings and events will be cleaned up automatically after the test has executed.

## Caching

Redis has been configured as the default caching provider. When running docker-compose with the local configuration a
Redis container will be started. If starting the darts-api from Intellij or the command line you have the following
options:

1. Start the Redis service as follows:

 ```bash
 docker compose -f docker-compose-local.yml up darts-redis
 ```

Then start the darts-api ensuring that the `local` profile is activated.

2. Alternatively the darts-api can be run using a simple in-memory cache by starting the application with the
   profile `in-memory-caching`.

To view the cache - when running against local Redis - Intellij has a free plugin called `Redis Helper`. However, if you
want to view the cache in staging the plugin doesn't support SSL. Instead, install:

```bash
brew install --cask another-redis-desktop-manager
sudo xattr -rd com.apple.quarantine /Applications/Another\ Redis\ Desktop\ Manager.app
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
