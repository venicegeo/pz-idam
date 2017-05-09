To run the pz-idam REST endpoints locally, run the following commands to retrieve the project from git and start a Tomcat server hosting the web service:

git clone git@github.com:venicegeo/pz-idam.git
mvn clean install -U spring-boot:run

The Tomcat server will be listening on port 8080. The roles are stored in a file: src/main/resources/roles.txt. To prevent this file from being overwritten with the default values every time the pz-idam application runs, do the following:

    Copy the file to a readable and writable location on the filesystem, e.g., /data/roles.txt

    Update the pz.idam.fileurl property in the src/main/resources/application.properties file with this new location.

