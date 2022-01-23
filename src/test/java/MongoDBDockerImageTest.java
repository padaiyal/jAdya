import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;

public class MongoDBDockerImageTest extends ADockerImageTest {

  @Override
  public DockerImage[] getDockerImages() {
    return new DockerImage[]{
        DockerImage.MONGODB_LATEST
    };
  }

  @Override
  public String getContainerName() {
    return "mongo_db_container";
  }

  @Override
  public long getStartupWaitTimeInMs() {
    return 20_000;
  }

  @Override
  public void testServiceInDockerContainer(DockerImage dockerImage) {
    String password = dockerImage.getEnvironmentVariable("MONGO_INITDB_ROOT_PASSWORD");
    Objects.requireNonNull(password);
    MongoCredential credential = MongoCredential
        .createCredential("root", "admin", password.toCharArray());
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
}