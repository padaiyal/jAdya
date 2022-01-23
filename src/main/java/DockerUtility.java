import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DockerUtility {

  private static final Logger logger = LogManager.getLogger(DockerUtility.class);
  private static DockerClientConfig defaultDockerClientConfig = DefaultDockerClientConfig
      .createDefaultConfigBuilder()
      .build();
  private static DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
      .dockerHost(defaultDockerClientConfig.getDockerHost())
      .sslConfig(defaultDockerClientConfig.getSSLConfig())
      .maxConnections(100)
      .connectionTimeout(Duration.ofSeconds(30))
      .responseTimeout(Duration.ofSeconds(45))
      .build();
  private static final DockerClient dockerClient = DockerClientBuilder.getInstance()
      .withDockerHttpClient(dockerHttpClient)
      .build();

  /**
   * Checks if a specified docker image exists.
   *
   * @param dockerImage Image to check
   * @return true if image exists, else false
   */
  public boolean doesImageExist(DockerImage dockerImage) {
    logger.info("Checking if docker image '" + dockerImage + "' exists");
    boolean result = false;
    if (dockerImage != null) {
      result = dockerClient.listImagesCmd()
          .exec()
          .stream()
          .map(Image::getRepoTags)
          .filter(Objects::nonNull)
          .map(Arrays::asList)
          .anyMatch(repoTags -> repoTags
              .contains(dockerImage.getRepository() + ":" + dockerImage.getImageTag()));
    }
    return result;
  }

  /**
   * Checks if a specified docker container exists.
   *
   * @param containerName      Name of container to check
   * @param checkAllContainers If true, checks are container names irrespective of whether it is
   *                           running or not
   * @return true if container exists, else false
   */
  public boolean doesContainerExist(String containerName, boolean checkAllContainers) {
    logger.info("Checking if docker container '" + containerName + "' exists");
    boolean result = false;
    if (containerName != null) {
      result = dockerClient.listContainersCmd()
          .withShowAll(checkAllContainers)
          .exec()
          .stream()
          .anyMatch(container -> Arrays.asList(container.getNames()).contains("/" + containerName));
    }
    return result;
  }

  /**
   * Removes a specified container if it exists.
   *
   * @param containerName Name of container to remove
   */
  public void removeContainerIfExists(String containerName) {
    // Input validation
    Objects.requireNonNull(containerName);

    //Check if container exists
    if (doesContainerExist(containerName, true)) {

      // If the container is running, stop it.
      if (doesContainerExist(containerName, false)) {
        logger.info("Stopping container '" + containerName + "'");
        dockerClient.stopContainerCmd(containerName)
            .exec();
      }

      // Remove container.
      logger.info("Removing container '" + containerName + "'");
      dockerClient.removeContainerCmd(containerName)
          .withForce(true)
          .exec();
      logger.info("Removed pre-existing container '" + containerName + "'");
    }
  }

  /**
   * Pulls the specified docker image from the configured sources.
   *
   * @param dockerImage Docker image to pull
   * @throws InterruptedException When the image pull task is interrupted
   */
  public void pullImage(DockerImage dockerImage) throws InterruptedException {
    // Input validation
    Objects.requireNonNull(dockerImage);

    // Pull the docker image from a pre-configured source
    logger.info("Pulling docker image '" + dockerImage.name() + "'");
    dockerClient.pullImageCmd("")
        .withRepository(dockerImage.getRepository())
        .withTag(dockerImage.getImageTag())
        .start()
        .awaitCompletion();
  }

  /**
   * Creates  docker container from a specified image with the specified container name.
   *
   * @param dockerImage             Image from which the container has to be created
   * @param containerName           Name of container to create
   * @param removeContainerIfExists If true, it removes any container with the same name before
   *                                creating a new one
   */
  public void createContainer(DockerImage dockerImage, String containerName,
      boolean removeContainerIfExists) {
    // Input validation
    Objects.requireNonNull(dockerImage);
    Objects.requireNonNull(containerName);

    if (removeContainerIfExists) {
      removeContainerIfExists(containerName);
    }

    // Create host config with all required port bindings
    logger.info(
        "Retrieving port bindings '" + dockerImage.getPortBindings() + "' to add to container '"
            + containerName + "'");
    Ports portBindings = new Ports();
    dockerImage.getPortBindings()
        .forEach(
            (containerPort, hostPort) -> portBindings.bind(
                ExposedPort.tcp(containerPort),
                Ports.Binding.bindPort(hostPort)
            )
        );
    HostConfig hostConfig = new HostConfig()
        .withPortBindings(portBindings);

    // Create a docker container from the pulled image
    logger.info("Creating container '" + containerName + "' from image '" + dockerImage + "'");
    dockerClient.createContainerCmd(containerName)
        .withImage(dockerImage.getRepository() + ":" + dockerImage.getImageTag())
        .withName(containerName)
        .withHostConfig(hostConfig)
        .withEnv(dockerImage.getEnvironmentVariables())
        .exec();
  }

  /**
   * Creates and run a docker container from a specified Docker image name with a specified
   * container name. If the image is not present locally, it tries to pull it.
   *
   * @param dockerImage             Image to create a container from
   * @param containerName           Name of container to create
   * @param removeContainerIfExists If true, it removes any container with the same name before
   *                                creating a new one
   */
  public void createAndRunContainer(DockerImage dockerImage, String containerName,
      boolean removeContainerIfExists) throws InterruptedException {
    // Input validation
    Objects.requireNonNull(dockerImage);
    Objects.requireNonNull(containerName);

    // Pull the docker image from a pre-configured source
    pullImage(dockerImage);

    // Create docker container
    createContainer(dockerImage, containerName, removeContainerIfExists);

    try {
      // Start the container
      logger.info("Starting container '" + containerName + "'");
      dockerClient.startContainerCmd(containerName)
          .exec();

      // Attach standard input, output and error streams to container
      logger.info("Attaching STDIO to container '" + containerName + "'");
      dockerClient.attachContainerCmd(containerName)
          .withFollowStream(true)
          .withStdOut(true)
          .withStdErr(true)
          .start()
          .awaitCompletion(5, TimeUnit.SECONDS);
    } catch (RuntimeException e) {
      removeContainerIfExists(containerName);
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the Docker client object.
   *
   * @return Docker client object
   */
  @SuppressWarnings("unused")
  public DockerClient getDockerClient() {
    return dockerClient;
  }
}
