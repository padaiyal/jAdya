import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MongoDBDockerImageTest {

    /**
     * Connects to the specified MongoDB database.
     * @param dockerImage Docker image to retrieve all connection parameters from
     */
    private void connectToMongoDBDatabase(DockerImage dockerImage) {
        String password = Arrays.stream(dockerImage.getEnvironmentVariables())
                .filter(environmentVariable -> environmentVariable.startsWith("MONGO_INITDB_ROOT_PASSWORD="))
                .map(environmentVariable -> environmentVariable.substring(27))
                .findFirst()
                .orElse("");

        MongoCredential credential = MongoCredential.createCredential("root", "admin", password.toCharArray());
        MongoClient mongoClient = new MongoClient(
                new ServerAddress("localhost", 27017),
                Collections.singletonList(credential)
        );
        Assertions.assertEquals(
                3,
                mongoClient.listDatabaseNames()
                        .into(new ArrayList<>())
                        .size()
        );
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "MONGODB_LATEST"
            }
    )
    public void testMongoDBDockerImage(DockerImage dockerImage) throws InterruptedException {
        DockerUtility dockerUtility = new DockerUtility();
        String containerName = "mongodb_container";
        try {
            // Create and run container
            dockerUtility.createAndRunContainer(dockerImage, containerName, true);

            // Wait 20s for the MongoDB server to start up
            Thread.sleep(20_000);

            // Test if the connection to the database is successful
            Assertions.assertDoesNotThrow(() -> connectToMongoDBDatabase(dockerImage));
        }
        finally {
            // Stop and remove container
            dockerUtility.removeContainerIfExists(containerName);
        }
    }
}