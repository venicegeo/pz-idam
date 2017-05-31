**pz-idam** provides identiy and access management support for Piazza. 

To build and run pz-idam, software such as Maven and MongoDB are required.  For details on these prerequisites, see the Developer's Guide https://pz-docs.geointservices.io/devguide/index.html#_piazza_core_overview

pz-idam uses _Spring Profiles_ to invoke authentication models based on the required identity and access management approach.   By default, pz-idam runs with disable-authn spring profile.

To clone the pz-idam repostioriy, run the following command:

`> mvn git clone git@github.com:venicegeo/pz-idam.git`
    
To run the pz-idam locally, run the following command:

`> mvn spring-boot:run -Drun.profiles=disable-authn`

When idam has initialized successfully, the following message will be displayed:

`2017-05-31 07:02:27.934  INFO 5104 --- [           main] org.venice.piazza.idam.Application       : Started Application in 4.671 seconds (JVM running for 7.033)`

By default Tomcat server listens on port 443.   To change port and other properties, update the **application.properties** file located in the _src/main/resources/_ directory.
