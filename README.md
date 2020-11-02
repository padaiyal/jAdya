# Adya

`Adya` means `first power and unparalleled`, emphasizing the importance of deploying dependent frameworks and core technology to any application.
This library lets you deploy and manage docker images/containers via Java.

### Supported Docker Images
- Databases
  - MS-SQL 2017
  - MS-SQL 2019
  - MySQL
  - MongoDB
  - Splunk

# Usage
Here's an example code to deploy/terminate a MS-SQL docker container via Adya:
```
  ...
  DockerUtility dockerUtility = new DockerUtility();

  DockerImage dockerImage = DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST;

  String containerName = dockerImage.toString();

  // Create and run container
  dockerUtility.createAndRunContainer(dockerImage, containerName, true);

  // Stop the container and remove it if it exists
  removeContainerIfExists(containerName);
  ...
```
