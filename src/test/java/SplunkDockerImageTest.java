import com.splunk.Service;
import com.splunk.ServiceArgs;

import java.util.Objects;

public class SplunkDockerImageTest extends ADockerImageTest {

    @Override
    public DockerImage[] getDockerImages() {
        return new DockerImage[]{
            DockerImage.SPLUNK_LATEST
        };
    }

    @Override
    public String getContainerName() {
        return "splunk_container";
    }

    @Override
    public long getStartupWaitTimeInMs() {
        return 30_000;
    }

    /**
     * Test connectivity to a specified Splunk server.
     * @param host Hostname/IP address of the machine/container hosting the Splunk service.
     * @param port The port in which Splunk is accepting incoming connections.
     * @param username Username to log in to the Splunk server.
     * @param password Password to log in to the Splunk server.
     */
    public static void testSplunkConnection(String host, Integer port, String username, String password) {
        // Create a map of arguments and add login parameters
        ServiceArgs loginArgs = new ServiceArgs();
        loginArgs.setUsername(username);
        loginArgs.setPassword(password);
        loginArgs.setHost(host);
        loginArgs.setPort(port);

        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        // Create a Service instance by logging in with the argument map
        Service.connect(loginArgs);
    }

    @Override
    public void testServiceInDockerContainer(DockerImage dockerImage) {
        String password = dockerImage.getEnvironmentVariable("SPLUNK_PASSWORD");
        Objects.requireNonNull(password);

        testSplunkConnection("127.0.0.1", 8089, "admin", password);
    }
}