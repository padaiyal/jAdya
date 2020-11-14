import java.io.IOException;
import java.util.Objects;

import javax.ws.rs.HttpMethod;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Assertions;

public class ElasticsearchDockerImageTest extends ADockerImageTest{

    private static final Logger logger = LogManager.getLogger(ElasticsearchDockerImageTest.class);

    @Override
    public DockerImage[] getDockerImages() {
        return new DockerImage[] {
            DockerImage.ELASTICSEARCH_7_9_2
        };
    }

    @Override
    public String getContainerName() {
        return "elastic_search_container";
    }

    @Override
    public long getStartupWaitTimeInMs() {
        return 30_000;
    }

    @Override
    public void testServiceInDockerContainer(DockerImage dockerImage) {
        final RequestOptions COMMON_OPTIONS;
        RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http"),
            new HttpHost("localhost", 9300, "http")
        ).build();
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        Base64 base64 = new Base64();
        String userName = dockerImage.getEnvironmentVariable("ELASTIC_USERNAME");
        Objects.requireNonNull(userName);
        String password = dockerImage.getEnvironmentVariable("ELASTIC_PASSWORD");
        Objects.requireNonNull(password);
        String encoding = base64.encodeAsString((userName + ":" + password).getBytes());
        builder.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + encoding);
        COMMON_OPTIONS = builder.build();

        try {
            String testIndex = "test_index";
            Request insertRequest = new Request(HttpMethod.POST, "/" + testIndex + "/_doc");
            insertRequest.setJsonEntity("{\"k1\":\"v1\"}");
            insertRequest.setOptions(COMMON_OPTIONS);
            Response response = restClient.performRequest(insertRequest);
            Assertions.assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
            Assertions.assertEquals("Created", response.getStatusLine().getReasonPhrase());

            Request deleteRequest = new Request(HttpMethod.DELETE, testIndex);
            deleteRequest.setOptions(COMMON_OPTIONS);
            Response deleteResponse = restClient.performRequest(deleteRequest);
            Assertions.assertEquals(HttpStatus.SC_OK, deleteResponse.getStatusLine().getStatusCode());
            Assertions.assertEquals("OK", deleteResponse.getStatusLine().getReasonPhrase());
        } catch (IOException e) {
            logger.error("Something went wrong while connecting to Elasticsearch - " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
