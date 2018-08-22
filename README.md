# pz-idam

The Piazza Core pz-idam project is an internal component that provides REST endpoints for handling Authentication and Authorization. This is done by brokering the Authentication (AuthN) functionality to an external service (in this case, GEOAxIS); and using an internal series of interfaces for providing Authorization (AuthZ). This project is used by the Gateway in order to generate API Keys and provide full AuthN/AuthZ capabilities.

***
## Requirements
Before building and/or running the pz-search-query service, please ensure that the following components are available and/or installed, as necessary:
- [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (JDK for building/developing, otherwise JRE is fine)
- [Maven (v3 or later)](https://maven.apache.org/install.html)
- [Git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [PostgreSQL](https://www.postgresql.org/download)
- [RabbitMQ](https://www.rabbitmq.com/download.html)d
- Access to Nexus is required to build

Ensure that the nexus url environment variable `ARTIFACT_STORAGE_URL` is set:

	$ export ARTIFACT_STORAGE_URL={Artifact Storage URL}

For additional details on prerequisites, please refer to the Piazza Developer's Guide [Core Overview](http://pz-docs.int.dev.east.paas.geointservices.io/devguide/02-pz-core/) or [Piazza IDAM](http://pz-docs.int.dev.east.paas.geointservices.io/devguide/13-pz-idam/) sections. Also refer to the [prerequisites for using Piazza](http://pz-docs.int.dev.east.paas.geointservices.io/devguide/03-jobs/) section for additional details.

***
## Setup, Configuring, & Running
### Setup
Create the directory the repository must live in, and clone the git repository:

    $ mkdir -p {PROJECT_DIR}/src/github.com/venicegeo
	$ cd {PROJECT_DIR}/src/github.com/venicegeo
    $ git clone git@github.com:venicegeo/pz-idam.git
    $ cd pz-idam

>__Note:__ In the above commands, replace {PROJECT_DIR} with the local directory path for where the project source is to be installed.

### Configuring
As noted in the Requirements section, to build and run this project, RabbitMQ and PostgreSQL are required. The `src/main/resources/application.properties` file controls URL information for connection configurations.


### Running

pz-idam uses _Spring Profiles_ to invoke authentication models based on the required identity and access management approach. By default, pz-idam runs with _disable-authn_ Spring profile.
    
To run the pz-idam locally, run the following command:

	$ mvn spring-boot:run -Drun.profiles=disable-authn

When idam has initialized successfully, the following message will be displayed:

`2017-05-31 07:02:27.934  INFO 5104 --- [           main] org.venice.piazza.idam.Application       : Started Application in 4.671 seconds (JVM running for 7.033)`

By default Tomcat server listens on port 443.   To change port and other properties, update the **application.properties** file located in the _src/main/resources/_ directory.

### Running Unit Tests

To run the Piazza IDAM unit tests from the main directory, run the following command:

	$ mvn test

