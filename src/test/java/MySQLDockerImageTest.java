import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.*;
import java.util.Arrays;

public class MySQLDockerImageTest {

    private final static Logger logger = LogManager.getLogger(MySQLDockerImageTest.class);

    /**
     * Connects to the specified MySQL database.
     * @param dockerImage Docker image to retrieve all connection parameters from
     * @throws SQLException Thrown when an issue occurs while connecting to the database
     */
    private void connectToMySQLDatabase(DockerImage dockerImage) throws SQLException {
        String password = Arrays.stream(dockerImage.getEnvironmentVariables())
                .filter(environmentVariable -> environmentVariable.startsWith("MYSQL_ROOT_PASSWORD="))
                .map(environmentVariable -> environmentVariable.substring(20))
                .findFirst()
                .orElse("");

        String connectionUrl =
                "jdbc:mysql://127.0.0.1:3306";

        Connection connection = DriverManager.getConnection(connectionUrl, "root", password);
        Statement statement = connection.createStatement();
        @SuppressWarnings("SqlNoDataSourceInspection")
        String query = "show databases;";
        logger.info("Executing query: " + query);
        ResultSet resultSet = statement.executeQuery(query);
        Assertions.assertEquals(0, resultSet.getFetchSize());
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "MYSQL_LATEST"
            }
    )
    public void testMySQLDockerImage(DockerImage dockerImage) throws InterruptedException {
        DockerUtility dockerUtility = new DockerUtility();
        String containerName = "mysql_container";

        try {
            // Create and run container
            dockerUtility.createAndRunContainer(dockerImage, containerName, true);

            // Wait 20s for the SQL server to start up
            Thread.sleep(20_000);

            // Test if the connection to the database is successful
            Assertions.assertDoesNotThrow(() -> connectToMySQLDatabase(dockerImage));
        }
        finally {
            // Stop and remove container
            dockerUtility.removeContainerIfExists(containerName);
        }
    }
}