import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

public class MySQLDockerImageTest extends ADockerImageTest {

  private final static Logger logger = LogManager.getLogger(MySQLDockerImageTest.class);

  @Override
  public DockerImage[] getDockerImages() {
    return new DockerImage[]{
        DockerImage.MYSQL_LATEST
    };
  }

  @Override
  public String getContainerName() {
    return "mysql_container";
  }

  @Override
  public long getStartupWaitTimeInMs() {
    return 20_000;
  }

  @Override
  public void testServiceInDockerContainer(DockerImage dockerImage) {
    String password = dockerImage.getEnvironmentVariable("MYSQL_ROOT_PASSWORD");
    Objects.requireNonNull(password);
    String connectionUrl = "jdbc:mysql://127.0.0.1:3306";

    try {
      Connection connection = DriverManager.getConnection(connectionUrl, "root", password);
      Statement statement = connection.createStatement();
      @SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
      String query = "show databases;";
      logger.info("Executing query: " + query);
      ResultSet resultSet = statement.executeQuery(query);
      Assertions.assertEquals(0, resultSet.getFetchSize());
    } catch (SQLException e) {
      logger.error("SQLException thrown while trying to connect to the DB.", e);
      throw new RuntimeException(e);
    }
  }
}