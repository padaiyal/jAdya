import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

public class PostgreSQLDockerImageTest extends ADockerImageTest {

  private final static Logger logger = LogManager.getLogger(PostgreSQLDockerImageTest.class);

  /**
   * Test connectivity to a specified Postgres SQL DB.
   *
   * @param host     Hostname/IP address of the machine/container hosting the Postgres SQL DB.
   * @param port     The port in which the Postgres SQL DB is accepting incoming connections.
   * @param username Username to log in to the Postgres SQL DB.
   * @param password Password to log in to the Postgres SQL DB.
   */
  public static void testPostgresSQLConnection(String host, Integer port, String username,
      String password) {
    String connectionUrl = String.format("jdbc:postgresql://%s:%d/", host, port);

    try {
      Connection connection = DriverManager.getConnection(connectionUrl, username, password);

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

  @Override
  public DockerImage[] getDockerImages() {
    return new DockerImage[]{
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
    testPostgresSQLConnection("127.0.0.1", 5432, "postgres", password);
  }
}
