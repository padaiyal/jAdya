import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum DockerImage {
    // Databases
    MICROSOFT_SQL_SERVER_2017_CU21_UBUNTU_16_04(
            "mcr.microsoft.com/mssql/server",
            "2017-CU21-ubuntu-16.04",
            "1433:1433",
            "ACCEPT_EULA=Y;SA_PASSWORD=initialPassword12345!"
    ),
    MICROSOFT_SQL_SERVER_2017_LATEST(
            "mcr.microsoft.com/mssql/server",
            "2017-latest",
            "1433:1433",
            "ACCEPT_EULA=Y;SA_PASSWORD=initialPassword12345!"
    ),
    MICROSOFT_SQL_SERVER_2019_CU6_UBUNTU_16_04(
            "mcr.microsoft.com/mssql/server",
            "2019-CU6-ubuntu-16.04",
            "1433:1433",
            "ACCEPT_EULA=Y;SA_PASSWORD=initialPassword12345!"
    ),
    MICROSOFT_SQL_SERVER_2019_LATEST(
            "mcr.microsoft.com/mssql/server",
            "2019-latest",
            "1433:1433",
            "ACCEPT_EULA=Y;SA_PASSWORD=initialPassword12345!"
    ),
    MYSQL_LATEST(
            "mysql",
            "latest",
            "3306:3306",
            "MYSQL_ROOT_PASSWORD=initialPassword12345!"
    ),
    MONGODB_LATEST(
            "mongo",
            "latest",
            "27017:27017",
            "MONGO_INITDB_ROOT_USERNAME=root;MONGO_INITDB_ROOT_PASSWORD=example"
    ),
    SPLUNK_LATEST(
            "splunk/splunk",
            "latest",
            "8000:8000 8089:8089",
            "SPLUNK_START_ARGS=--accept-license;SPLUNK_PASSWORD=initialPassword12345!"
    );

    private final String repository;
    private final String imageTag;
    private final Map<Integer, Integer> portBindings;
    private final String[] environmentVariables;

    DockerImage(String repository, String imageTag, String portBindingsString, String environmentVariablesString) {
        this.repository = repository;
        this.imageTag = imageTag;
        this.portBindings = Arrays.stream(portBindingsString.split(" "))
                .filter(portBindingString -> !portBindingString.isBlank())
                .map(portBindingString -> portBindingString.split(":"))
                .collect(Collectors.toUnmodifiableMap(portBinding -> Integer.parseInt(portBinding[0]), portBinding -> Integer.parseInt(portBinding[1])));
        this.environmentVariables = environmentVariablesString.split(";");
    }

    /**
     * Returns the docker image tag.
     * @return Docker image tag
     */
    public String getImageTag() {
        return this.imageTag;
    }

    /**
     * Returns the docker repository.
     * @return Docker repository
     */
    public String getRepository() {
        return this.repository;
    }

    /**
     * Returns the port bindings to be configured for this docker image
     * @return Port bindings for the docker image
     */
    public Map<Integer, Integer> getPortBindings() {
        return this.portBindings;
    }

    /**
     * Returns the environment variables to be configured for this docker image
     * @return Environment variables for the docker image
     */
    public String[] getEnvironmentVariables() {
        return this.environmentVariables;
    }

    @Override
    public String toString() {
        return this.imageTag;
    }

}
