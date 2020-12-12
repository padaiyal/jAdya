import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.Response;
import utilities.ShellUtility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class DockerComposeUtility {

    private static final Logger logger = LogManager.getLogger(DockerComposeUtility.class);
    private static final List<String> supportedDockerComposeCommands = List.of("up", "down");

    /**
     * Wrapper for the docker-compose command.
     * @param dockerComposeCommand Command to issue to docker-compose. Currently it only supports "up" and "down".
     * @param composeFilePath Path to the compose yaml file.
     * @param additionalArgs Additional arguments to pass to docker-compose.
     * @param timeoutDuration Timeout duration for running the docker-compose command.
     * @throws IOException When there is an issue executing the docker-compose command or if the compose
     * yaml file is not found.
     * @throws InterruptedException When the method is interrupted at runtime.
     * @throws TimeoutException When the docker-compose command takes more time than the specified timeout duration.
     */
    public static void dockerCompose(
        String dockerComposeCommand,
        Path composeFilePath,
        String additionalArgs,
        Duration timeoutDuration
    ) throws IOException, InterruptedException, TimeoutException {

        // Input validation
        Objects.requireNonNull(dockerComposeCommand);
        Objects.requireNonNull(composeFilePath);
        Objects.requireNonNull(additionalArgs);
        Objects.requireNonNull(timeoutDuration);
        if(!supportedDockerComposeCommands.contains(dockerComposeCommand)) {
            throw new IllegalArgumentException("Unsupported docker compose command - " + dockerComposeCommand
                + "\nSupported commands are: " + supportedDockerComposeCommands);
        } else if(
            !composeFilePath.toAbsolutePath()
                .toString()
                .endsWith(".yml")
        ) {
            throw new IllegalArgumentException(
                "Only a .yml file is considered a valid compose file. Specified file is not one - " + composeFilePath
            );
        } else if(!Files.exists(composeFilePath)) {
            throw new FileNotFoundException("Compose file not found in specified path - " + composeFilePath);
        }

        String dockerComposeCommandString = String.format(
            "docker-compose -f %s %s %s",
            composeFilePath,
            dockerComposeCommand,
            additionalArgs
        );
        Response response = ShellUtility.executeCommand(dockerComposeCommandString, timeoutDuration);
        if(response.getReturnCode() != 0) {
            String stderr_output = response.getOutput(ShellUtility.TypeOfOutput.STDERR);
            logger.error(stderr_output);
            throw new RuntimeException("Non zero return code - " + response.getReturnCode() + "\n" + stderr_output);
        }

    }

}
