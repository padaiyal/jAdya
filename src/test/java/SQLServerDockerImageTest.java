import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Objects;

public class SQLServerDockerImageTest extends ADockerImageTest {

    private final static Logger logger = LogManager.getLogger(SQLServerDockerImageTest.class);

    @Override
    public DockerImage[] getDockerImages() {
        return new DockerImage[] {
            DockerImage.MICROSOFT_SQL_SERVER_2019_LATEST,
            DockerImage.MICROSOFT_SQL_SERVER_2019_CU6_UBUNTU_16_04,
            DockerImage.MICROSOFT_SQL_SERVER_2017_LATEST,
            DockerImage.MICROSOFT_SQL_SERVER_2017_CU21_UBUNTU_16_04
        };
    }

    @Override
    public String getContainerName() {
        return "sql_server_container";
    }

    @Override
    public long getStartupWaitTimeInMs() {
        return 20_000;
    }

    @Override
    public void testServiceInDockerContainer(DockerImage dockerImage) {
        String password = dockerImage.getEnvironmentVariable("SA_PASSWORD");
        Objects.requireNonNull(password);

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

        try {
            Connection connection = DriverManager.getConnection(connectionUrl);
            Statement statement = connection.createStatement();
            @SuppressWarnings("SqlNoDataSourceInspection")
            String query = "SELECT name, database_id, create_date FROM sys.databases ;";
            logger.info("Executing query: " + query);
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                logger.info("Query Result: " + resultSet.getString(1) + ", " + resultSet.getString(2) + ", " + resultSet.getString(3));
            }
        } catch (SQLException e) {
            logger.error("SQLException thrown while trying to connect to the DB.", e);
            throw new RuntimeException(e);
        }
    }
}