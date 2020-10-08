import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.*;
import java.util.Arrays;

public class SQLServerDockerImageTest {

    private final static Logger logger = LogManager.getLogger(SQLServerDockerImageTest.class);

    /**
     * Connects to the specified MSSQL database.
     * @param dockerImage Docker image to retrieve all connection parameters from
     * @throws SQLException Thrown when an issue occurs while connecting to the database
     */
    private void connectToMSSQLDatabase(DockerImage dockerImage) throws SQLException {
        String password = Arrays.stream(dockerImage.getEnvironmentVariables())
                .filter(environmentVariable -> environmentVariable.startsWith("SA_PASSWORD="))
                .map(environmentVariable -> environmentVariable.substring(12))
                .findFirst()
                .orElse("");

        String connectionUrl = String.format(
                "jdbc:sqlserver://127.0.0.1:1433;"
                        + "database=;"
                        + "user=sa;"
                        + "password=%s;"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=30;",
                        password
        );

        Connection connection = DriverManager.getConnection(connectionUrl);
        Statement statement = connection.createStatement();
        @SuppressWarnings("SqlNoDataSourceInspection")
        String query = "SELECT name, database_id, create_date FROM sys.databases ;";
        logger.info("Executing query: " + query);
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet.next()) {
            logger.info("Query Result: " + resultSet.getString(1) + ", " + resultSet.getString(2) + ", " + resultSet.getString(3));
        }
    }

    @ParameterizedTest
    @CsvSource(
        {
            "MICROSOFT_SQL_SERVER_2017_CU21_UBUNTU_16_04",
            "MICROSOFT_SQL_SERVER_2017_LATEST",
            "MICROSOFT_SQL_SERVER_2019_CU6_UBUNTU_16_04",
            "MICROSOFT_SQL_SERVER_2019_LATEST"
        }
    )
    public void testMSSQLDockerImage(DockerImage dockerImage) throws InterruptedException {
        DockerUtility dockerUtility = new DockerUtility();
        String containerName = "mssql_container";

        try {

            logger.info("Starting container: " + containerName);

            // Create and run container
            dockerUtility.createAndRunContainer(dockerImage, containerName, true);

            // Wait 20s for the SQL server to start up
            Thread.sleep(20_000);

            // Test if the connection to the database is successful
            Assertions.assertDoesNotThrow(() -> connectToMSSQLDatabase(dockerImage));
        }
        finally {
            // Stop and remove container
            dockerUtility.removeContainerIfExists(containerName);
        }
    }
}