import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class ADockerImageTest {

  /**
   * Returns a list of docker images to deploy as containers and test.
   *
   * @return A list of docker images to deploy as containers and test.
   */
  public abstract DockerImage[] getDockerImages();

  /**
   * Returns the name to use for the test container.
   *
   * @return The name to use for the test container.
   */
  public abstract String getContainerName();

  /**
   * Returns the time to wait for the container to start up in milliseconds.
   *
   * @return The time to wait for the container to start up in milliseconds.
   */
  public abstract long getStartupWaitTimeInMs();

  /**
   * Test the service running in the docker container.
   *
   * @param dockerImage Docker image that has been deployed to a container.
   */
  public abstract void testServiceInDockerContainer(DockerImage dockerImage);

  /**
   * Deploy and test the pre-specified list of docker images as containers.
   *
   * @throws InterruptedException If the container deployment is interrupted.
   */
  @Test
  public void testDockerContainerDeployment() throws InterruptedException {
    DockerImage[] dockerImages = getDockerImages();
    for (DockerImage dockerImage : dockerImages) {
      String containerName = getContainerName();
      long startupWaitTimeInMs = getStartupWaitTimeInMs();
      DockerUtility dockerUtility = new DockerUtility();
      try {
        // Create and run container
        dockerUtility.createAndRunContainer(dockerImage, containerName, false);
        // Wait for the container to start up
        Thread.sleep(startupWaitTimeInMs);
        // Test the service running in the container
        Assertions.assertDoesNotThrow(() -> testServiceInDockerContainer(dockerImage));
      } finally {
        dockerUtility.removeContainerIfExists(containerName);
      }
    }
  }
}
