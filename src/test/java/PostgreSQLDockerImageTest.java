import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.*;
import java.util.Arrays;

public class PostgreSQLDockerImageTest {

    private final static Logger logger = LogManager.getLogger(PostgreSQLDockerImageTest.class);

    /**
     * Connects to the specified PostgreSQL database.
     * @param dockerImage Docker image to retrieve all connection parameters from
     * @throws SQLException Thrown when an issue occurs while connecting to the database
     */
    private void connectToPostgreSQLDatabase(DockerImage dockerImage) throws SQLException {
        String password = Arrays.stream(dockerImage.getEnvironmentVariables())
                .filter(environmentVariable -> environmentVariable.startsWith("POSTGRES_PASSWORD="))
                .map(environmentVariable -> environmentVariable.substring(18))
                .findFirst()
                .orElse("");

        String connectionUrl =
                "jdbc:postgresql://127.0.0.1:5432/";

        Connection connection = DriverManager.getConnection(connectionUrl, "postgres", password);

        String query = "select datname from pg_database";
        logger.info("Executing query: " + query + " against PostgreSQL");
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);
        int actualRowCount = 0;
        while (resultSet.next()) {
            logger.info("Query Result from PostgreSQL: " + resultSet.getString(1));
            actualRowCount++;
        }
        // PostgreSQL have three databases created by default: postgres, template0, and template1
        Assertions.assertEquals(3, actualRowCount);
    }

    @ParameterizedTest
    @CsvSource(
            {
                    "POSTGRESQL_LATEST"
            }
    )
    public void tesPostgreSQLDockerImage(DockerImage dockerImage) throws InterruptedException {
        DockerUtility dockerUtility = new DockerUtility();
        String containerName = "postgresql_container";

        try {
            // Create and run container
            dockerUtility.createAndRunContainer(dockerImage, containerName, true);

            // Wait 20s for the PostgresSQL server to start up
            Thread.sleep(20_000);

            // Test if the connection to the database is successful
            Assertions.assertDoesNotThrow(() -> connectToPostgreSQLDatabase(dockerImage));
        }
        finally {
            // Stop and remove container
            dockerUtility.removeContainerIfExists(containerName);
        }
    }
}
