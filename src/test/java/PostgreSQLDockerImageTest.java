import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Objects;

public class PostgreSQLDockerImageTest extends ADockerImageTest {

    private final static Logger logger = LogManager.getLogger(PostgreSQLDockerImageTest.class);

    @Override
    public DockerImage[] getDockerImages() {
        return new DockerImage[] {
            DockerImage.POSTGRESQL_LATEST
        };
    }

    @Override
    public String getContainerName() {
        return "postgresql_container";
    }

    @Override
    public long getStartupWaitTimeInMs() {
        return 20_000;
    }

    @Override
    public void testServiceInDockerContainer(DockerImage dockerImage) {
        String password = dockerImage.getEnvironmentVariable("POSTGRES_PASSWORD");
        Objects.requireNonNull(password);
        String connectionUrl = "jdbc:postgresql://127.0.0.1:5432/";

        try {
            Connection connection = DriverManager.getConnection(connectionUrl, "postgres", password);

            @SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
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
        } catch (SQLException e) {
            logger.error("SQLException thrown while trying to connect to the DB.", e);
            throw new RuntimeException(e);
        }
    }
}
