import java.io.IOException;
import java.util.Arrays;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ElasticsearchDockerImageTest {

    private static final Logger logger = LogManager.getLogger(ElasticsearchDockerImageTest.class);
    private static final RequestOptions COMMON_OPTIONS;
    private static RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200, "http"),
            new HttpHost("localhost", 9300, "http")).build();
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        Base64 base64 = new Base64();
        String userName = Arrays.stream(DockerImage.ELASTICSEARCH_7_9_2.getEnvironmentVariables())
                .filter(environmentVariable -> environmentVariable.startsWith("ELASTIC_USERNAME="))
                .map(environmentVariable -> environmentVariable.substring("ELASTIC_USERNAME=".length()))
                .findFirst()
                .orElse("");
        String password = Arrays.stream(DockerImage.ELASTICSEARCH_7_9_2.getEnvironmentVariables())
                .filter(environmentVariable -> environmentVariable.startsWith("ELASTIC_PASSWORD="))
                .map(environmentVariable -> environmentVariable.substring("ELASTIC_PASSWORD=".length()))
                .findFirst()
                .orElse("");
        String encoding = base64.encodeAsString((userName + ":" + password).getBytes());
        builder.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + encoding);
        COMMON_OPTIONS = builder.build();
    }

    @ParameterizedTest
    @CsvSource(
            "ELASTICSEARCH_7_9_2"
    )
    public void testElasticsearchDockerImage(DockerImage dockerImage) throws InterruptedException {
        DockerUtility dockerUtility = new DockerUtility();
        String containerName = "elasticsearch";
        try {
            // Create and run container
            dockerUtility.createAndRunContainer(dockerImage, containerName, false);
            // Wait 20s for the ES container to start up
            Thread.sleep(20_000);
            // Index and delete a test document
            Assertions.assertDoesNotThrow(() -> connectToElasticsearch());
        } finally {
            dockerUtility.removeContainerIfExists(containerName);
        }
    }

    private void connectToElasticsearch() {
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
        }
    }
}
