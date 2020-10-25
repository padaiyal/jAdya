import com.splunk.Service;
import com.splunk.ServiceArgs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

public class SplunkDockerImageTest {

    /**
     * Connects to the specified Splunk server.
     * @param dockerImage Docker image to retrieve all connection parameters from
     */
    private void connectToSplunk(DockerImage dockerImage) {
        String password = Arrays.stream(dockerImage.getEnvironmentVariables())
                .filter(environmentVariable -> environmentVariable.startsWith("SPLUNK_PASSWORD="))
                .map(environmentVariable -> environmentVariable.substring(16))
                .findFirst()
                .orElse("");

        // Create a map of arguments and add login parameters
        ServiceArgs loginArgs = new ServiceArgs();
        loginArgs.setUsername("admin");
        loginArgs.setPassword(password);
        loginArgs.setHost("localhost");
        loginArgs.setPort(8089);

        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        // Create a Service instance and log in with the argument map
        Service service = Service.connect(loginArgs);

        System.out.println(service.getApplications());
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "SPLUNK_LATEST"
            }
    )
    public void testSplunkDockerImage(DockerImage dockerImage) throws InterruptedException {
        DockerUtility dockerUtility = new DockerUtility();
        String containerName = "splunk_container";
        try {
            // Create and run container
            dockerUtility.createAndRunContainer(dockerImage, containerName, true);

            // Wait 30s for the Splunk server to start up
            Thread.sleep(30_000);

            // Test if the connection to the database is successful
            Assertions.assertDoesNotThrow(() -> connectToSplunk(dockerImage));
        } finally {
            // Stop and remove container
            dockerUtility.removeContainerIfExists(containerName);
        }
    }
}