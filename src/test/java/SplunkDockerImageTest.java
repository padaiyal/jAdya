import com.splunk.Service;
import com.splunk.ServiceArgs;

import java.util.Objects;

public class SplunkDockerImageTest extends ADockerImageTest {

    @Override
    public DockerImage[] getDockerImages() {
        return new DockerImage[] {
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

    @Override
    public void testServiceInDockerContainer(DockerImage dockerImage) {
        String password = dockerImage.getEnvironmentVariable("SPLUNK_PASSWORD");
        Objects.requireNonNull(password);

        // Create a map of arguments and add login parameters
        ServiceArgs loginArgs = new ServiceArgs();
        loginArgs.setUsername("admin");
        loginArgs.setPassword(password);
        loginArgs.setHost("localhost");
        loginArgs.setPort(8089);

        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

        // Create a Service instance by logging in with the argument map
        Service.connect(loginArgs);
    }
}