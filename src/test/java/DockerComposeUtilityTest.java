import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DockerComposeUtilityTest {

  /**
   * Tests the docker compose wrapper with invalid inputs.
   *
   * @param composeCommand                         Docker compose command to test.
   * @param composeFilePathString                  Path of the compose yaml file to laod.
   * @param additionalArgs                         Any additional arguments needxed for the compose
   *                                               command.
   * @param timeoutDurationInSeconds               Timeout in seconds for executing the docker
   *                                               compose command.
   * @param resolveComposeFilePathAsAClassResource If true, it tries to resolve the specified
   *                                               compose yaml file path in the test resources
   *                                               directory, else it tries the specified path as if
   *                                               it's a valid path.
   * @param expectedExceptionClassString           Exception that is expected to be thrown.
   */
  @ParameterizedTest
  @CsvSource(
      {
          // All null inputs
          ",,,, false, NullPointerException",

          // Null compose file paths
          "up, , -d, 5, false, NullPointerException",
          "down, , -d, 5, false, NullPointerException",

          // Null additional args
          "up, splunk_postgres_compose, , 5, false, NullPointerException",
          "down, splunk_postgres_compose, , 5, false, NullPointerException",

          // Null timeout duration
          "up, splunk_postgres_compose, -d, , false, NullPointerException",
          "down, splunk_postgres_compose, -d, , false, NullPointerException",

          // Unsupported docker compose commands
          "start, splunk_postgres_compose.yml, -d, 5, false, IllegalArgumentException",
          "reertretre, splunk_postgres_compose.yml, -d, 30, false, IllegalArgumentException",

          // Invalid compose file paths
          "up, splunk_postgres_compose, -d, 5, false, IllegalArgumentException",
          "down, splunk_postgres_compose, -d, 5, false, IllegalArgumentException",

          // Non existent compose yaml file
          "up, splunk_postgres_compose_lol.yml, -d, 5, false, FileNotFoundException",

          // Invalid additional arguments
          "up, splunk_postgres_compose.yml, -ddooodd, 5, true, RuntimeException",
          "down, splunk_postgres_compose.yml, -dddd, 5, true, RuntimeException"
      }
  )
  public void testDockerComposeWithInvalidInputs(
      String composeCommand,
      String composeFilePathString,
      String additionalArgs,
      Integer timeoutDurationInSeconds,
      boolean resolveComposeFilePathAsAClassResource,
      String expectedExceptionClassString
  ) throws URISyntaxException {

    Class<? extends Exception> expectedException = switch (expectedExceptionClassString) {
      case "NullPointerException" -> NullPointerException.class;
      case "IllegalArgumentException" -> IllegalArgumentException.class;
      case "RuntimeException" -> RuntimeException.class;
      case "FileNotFoundException" -> FileNotFoundException.class;
      default -> throw new IllegalArgumentException(
          "Unsupported expected Exception - " + expectedExceptionClassString
      );
    };

    Path composeFilePath = null;
    if (composeFilePathString != null) {
      if (resolveComposeFilePathAsAClassResource) {
        composeFilePath = Paths.get(
            DockerComposeUtilityTest.class
                .getResource(composeFilePathString)
                .toURI()
        );
      } else {
        composeFilePath = Paths.get(composeFilePathString);
      }
    }
    Path finalComposeFilePath = composeFilePath;

    Duration timeoutDuration = (timeoutDurationInSeconds == null) ? null
        : Duration.of(timeoutDurationInSeconds, ChronoUnit.SECONDS);

    Assertions.assertThrows(
        expectedException,
        () -> DockerComposeUtility.dockerCompose(
            composeCommand,
            finalComposeFilePath,
            additionalArgs,
            timeoutDuration
        ),
        String.format(
            "Running test testDockerComposeWithInvalidInputs(\n" +
                "\tcomposeCommand='%s', \n" +
                "\tcomposeFilePathString='%s', \n" +
                "\tadditionalArgs='%s', \n" +
                "\ttimeoutDurationInSeconds=%d, \n" +
                "\tresolveComposeFilePathAsAClassResource=%b, \n" +
                "\texpectedExceptionClassString=%s\n" +
                ")",
            composeCommand,
            composeFilePathString,
            additionalArgs,
            timeoutDurationInSeconds,
            resolveComposeFilePathAsAClassResource,
            expectedExceptionClassString
        )
    );
  }

  /**
   * Test the docker-compose up and down commands via the wrapper.
   *
   * @throws InterruptedException When the compose command is interrupted.
   * @throws URISyntaxException   When an invalid compose yaml file path is specified.
   * @throws IOException          When the docker compose yaml file is not found or if the docker
   *                              compose execution fails.
   * @throws TimeoutException     If the docker compose command takes more time than the specifed
   *                              timeout.
   */
  @Test
  public void testDockerComposeUpAndDown()
      throws InterruptedException, URISyntaxException, IOException, TimeoutException {
    Path composeFilePath = Paths.get(
        this.getClass()
            .getResource("splunk_postgres_compose.yml")
            .toURI()
    );

    try {
      DockerComposeUtility.dockerCompose(
          "up",
          composeFilePath,
          "-d",
          Duration.of(600, ChronoUnit.SECONDS)
      );

      // Wait for environmnent to deploy
      Thread.sleep(30_000L);

      PostgreSQLDockerImageTest.testPostgresSQLConnection(
          "127.0.0.1",
          5432,
          "postgres",
          "initialPassword12345!"
      );

      SplunkDockerImageTest.testSplunkConnection(
          "127.0.0.1",
          8089,
          "admin",
          "initialPassword12345!"
      );

    } finally {
      DockerComposeUtility.dockerCompose(
          "down",
          composeFilePath,
          "",
          Duration.of(30, ChronoUnit.SECONDS)
      );
    }
  }

}
