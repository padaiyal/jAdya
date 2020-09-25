import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

public class DockerUtilityTest {

    private static List<DockerImage> imagesToPull;
    private static List<DockerImage> containersToCreate;
    private static List<DockerImage> containersToRun;
    private static DockerUtility dockerUtility;

    @BeforeAll
    public static void setUp() throws InterruptedException {
        imagesToPull = Arrays.asList(
                DockerImage.MICROSOFT_SQL_SERVER_2019_CU6_UBUNTU_16_04,
                DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST,
                DockerImage.MICROSOFT_SQL_SERVER_2017_CU21_UBUNTU_16_04,
                DockerImage.MICROSOFT_SQL_SERVER_2017_LATEST,
                DockerImage.MYSQL_LATEST
        );
        containersToCreate = Arrays.asList(
                DockerImage.MICROSOFT_SQL_SERVER_2017_LATEST,
                DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST,
                DockerImage.MYSQL_LATEST
        );
        // containersToRun should be a subset of containersToCreate, as only if the container is created can it be run.
        containersToRun = Arrays.asList(
                DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST,
                DockerImage.MYSQL_LATEST
        );

        dockerUtility = new DockerUtility();
        for (DockerImage imageToPull : imagesToPull) {
            dockerUtility.pullImage(imageToPull);
        }
        for (DockerImage containerToCreate : containersToCreate) {
            dockerUtility.createContainer(containerToCreate, containerToCreate.toString(), true);
        }
        for (DockerImage containerToRun : containersToRun) {
            dockerUtility.createAndRunContainer(containerToRun, containerToRun.toString(), true);
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
        for(DockerImage dockerImage: DockerImage.values()) {
            if(containersToCreate.contains(dockerImage)) {
                Assertions.assertTrue(
                        dockerUtility.doesContainerExist(dockerImage.toString(), true)
                );
            }
            else {
                Assertions.assertFalse(
                        dockerUtility.doesContainerExist(dockerImage.toString(), true)
                );
            }
        }
    }

    @Test
    /*
     * Tests if only all desired containers are running.
     */
    public void testIfDockerContainerIsRunning() {
        for(DockerImage dockerImage: DockerImage.values()) {
            if(containersToRun.contains(dockerImage)) {
                Assertions.assertTrue(
                        dockerUtility.doesContainerExist(dockerImage.toString(), false)
                );
            }
            else {
                Assertions.assertFalse(
                        dockerUtility.doesContainerExist(dockerImage.toString(), false)
                );
            }
        }
    }

    @AfterAll
    public static void destroy() {
        Set<DockerImage> containersToDestroy = new HashSet<>(containersToCreate);

        // Redundant step as containersToRun should be a subset of containersToCreate, just adding it to cover the
        // an anomalous scenario
        containersToDestroy.addAll(containersToRun);

        containersToDestroy.stream()
                .map(Object::toString)
                .forEach(dockerUtility::removeContainerIfExists);
    }

}