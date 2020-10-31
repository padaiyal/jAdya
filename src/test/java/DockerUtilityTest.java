import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

public class DockerUtilityTest {

    private final static Logger logger = LogManager.getLogger(DockerUtilityTest.class);

    private static List<DockerImage> imagesToPull;
    private static List<DockerImage> containersToCreate;
    private static List<DockerImage> containersToRun;
    private static Map<DockerImage, String> createdContainers;
    private static Map<DockerImage, String> runningContainers;
    private static DockerUtility dockerUtility;

    @BeforeAll
    public static void setUp() throws InterruptedException {
        imagesToPull = Arrays.asList(
                DockerImage.MICROSOFT_SQL_SERVER_2019_CU6_UBUNTU_16_04,
                DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST,
                DockerImage.MICROSOFT_SQL_SERVER_2017_CU21_UBUNTU_16_04,
                DockerImage.MICROSOFT_SQL_SERVER_2017_LATEST,
                DockerImage.MYSQL_LATEST,
                DockerImage.MONGODB_LATEST,
                DockerImage.SPLUNK_LATEST,
                DockerImage.POSTGRESQL_LATEST
        );

        createdContainers = new HashMap<>();
        containersToCreate = Arrays.asList(
                DockerImage.MICROSOFT_SQL_SERVER_2017_LATEST,
                DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST,
                DockerImage.MYSQL_LATEST,
                DockerImage.MONGODB_LATEST,
                DockerImage.SPLUNK_LATEST,
                DockerImage.POSTGRESQL_LATEST
        );

        // containersToRun should be a subset of containersToCreate, as only if the container is created can it be run.
        runningContainers = new HashMap<>();
        containersToRun = Arrays.asList(
                DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST,
                DockerImage.MYSQL_LATEST,
                DockerImage.MONGODB_LATEST,
                DockerImage.SPLUNK_LATEST,
                DockerImage.POSTGRESQL_LATEST
        );

        dockerUtility = new DockerUtility();
        for (DockerImage imageToPull : imagesToPull) {
            dockerUtility.pullImage(imageToPull);
        }
        for (DockerImage containerToCreate : containersToCreate) {
            String containerName = containerToCreate.name() + "_" + containerToCreate.toString() + "_" + Instant.now().getEpochSecond();
            createdContainers.put(containerToCreate, containerName);
            if (containersToRun.contains(containerToCreate)) {
                logger.info("Starting container: " + containerName);
                dockerUtility.createAndRunContainer(containerToCreate, containerName, true);
                runningContainers.put(containerToCreate, containerName);
            }
            else {
                dockerUtility.createContainer(containerToCreate, containerName, true);
            }
        }
    }

    @Test
    /*
     * Test if the docker service is up and running.
     */
    public void testDockerServiceAvailability() {
        Assertions.assertDoesNotThrow(() -> dockerUtility.getDockerClient()
                .pingCmd()
                .exec()
        );
    }

    @Test
    /*
     * Tests if all required docker images are present.
     */
    public void testIfDockerImageIsPresent() {
        for(DockerImage dockerImage: DockerImage.values()) {
            if(imagesToPull.contains(dockerImage)) {
                Assertions.assertTrue(
                        dockerUtility.doesImageExist(dockerImage)
                );
            }
            else {
                Assertions.assertFalse(
                        dockerUtility.doesImageExist(dockerImage)
                );
            }
        }
    }

    @Test
    /*
     * Test if all required docker containers are present.
     */
    public void testIfDockerContainerIsPresent() {
        for(DockerImage dockerImage: containersToCreate) {
            Assertions.assertTrue(
                dockerUtility.doesContainerExist(createdContainers.get(dockerImage), true)
            );
        }
    }

    @Test
    /*
     * Tests if only all desired containers are running.
     */
    public void testIfDockerContainerIsRunning() {
        for(DockerImage dockerImage: containersToRun) {
            Assertions.assertTrue(
                dockerUtility.doesContainerExist(runningContainers.get(dockerImage), false)
            );

        }
    }

    @AfterAll
    public static void destroy() {
        Set<String> containersToDestroy = new HashSet<>(createdContainers.values());

        // Redundant step as containersToRun should be a subset of containersToCreate, just adding it to cover the
        // an anomalous scenario
        containersToDestroy.addAll(runningContainers.values());

        containersToDestroy.stream()
                .map(Object::toString)
                .forEach(dockerUtility::removeContainerIfExists);
    }

}