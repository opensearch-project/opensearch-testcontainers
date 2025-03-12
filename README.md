opensearch-testcontainers
======================
A [Testcontainers](https://www.testcontainers.org/) integration for [OpenSearch](https://opensearch.org/).

## Overview

The [opensearch-testcontainers](https://github.com/opensearch-project/opensearch-testcontainers) delivers idiomatic integration of the [OpenSearch](https://opensearch.org/) Docker containers with [Testcontainers](https://www.testcontainers.org/) test scaffolding, since the out-of-the box support has been [ruled out](https://github.com/testcontainers/testcontainers-java/issues/4782) by maintainers for the time being.

## Testcontainers and OpenSearch Compatibility

|opensearch-testcontainers|OpenSearch|testcontainers|JDK|
|---|---|---|---|
|2.1.3-SNAPSHOT|2.0.0+|1.20.0+|11+|
|2.1.2|2.0.0+|1.20.0+|11+|
|2.1.1|2.0.0+|1.20.0+|11+|
|2.1.0|2.0.0+|1.20.0+|11+|
|2.0.2|2.0.0+|1.19.2+|11+|
|2.0.1|2.0.0+|1.19.2+|11+|
|2.0.0|2.0.0+|1.17.2+|11+|
|2.0.0|1.3.2+|1.17.2+|11+|
|1.0.1-SNAPSHOT|1.3.2+|1.17.2+|8+|
|1.0.0|1.3.2+|1.17.2+|8+|

## Usage

```xml
<dependency>
    <groupId>org.opensearch</groupId>
    <artifactId>opensearch-testcontainers</artifactId>
    <version>2.1.2</version>
    <scope>test</scope>
</dependency>
```

The [opensearch-testcontainers](https://github.com/opensearch-project/opensearch-testcontainers) could be used with [JUnit 4](https://junit.org/junit4/) or [JUnit 5](https://junit.org/junit5/).

## JUnit 4 Integration

Follow the [JUnit 4 Quickstart](https://www.testcontainers.org/quickstart/junit_4_quickstart/) to customize the container to your needs.

```java
@Rule
public OpenSearchContainer<?> opensearch = new OpenSearchContainer<>(DockerImageName.parse("opensearchproject/opensearch:2.11.0"));

```

## JUnit 5 Integration

Follow the [JUnit 5 Quickstart](https://www.testcontainers.org/quickstart/junit_5_quickstart/) to activate `@Testcontainers` extension and to customize the container to your needs.

```java
@Container
public OpenSearchContainer<?> opensearch = new OpenSearchContainer<>(DockerImageName.parse("opensearchproject/opensearch:2.11.0"));

```

Please note that at the moment [testcontainers](https://www.testcontainers.org/) brings in [JUnit 4.x dependencies](https://github.com/testcontainers/testcontainers-java/issues/970). If it conflicts with your project configuration, please follow linked Github issue for available mitigation strategies.

## Usage Examples

By default, [OpenSearch](https://opensearch.org/) Docker containers run with [security plugin](https://github.com/opensearch-project/security) activated, however `OpenSearchContainer` deactivates it. Use `withSecurityEnabled()` to enable security, please notice that in this case in order to connect to the running container the HTTPS protocol should be used along with username / password credentials.

```java

import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;

private static final DockerImageName OPENSEARCH_IMAGE = DockerImageName.parse("opensearchproject/opensearch:2.11.0");

// Create the Opensearch container.
try (OpenSearchContainer<?> container = new OpenSearchContainer<>(OPENSEARCH_IMAGE).withSecurityEnabled()) {
    // Start the container. This step might take some time...
    container.start();

    // Do whatever you want with the rest client ...
    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(container.getUsername(), container.getPassword())
    );

    // Allow self-signed certificates
    final SSLContext sslcontext = SSLContextBuilder.create()
            .loadTrustMaterial(null, new TrustAllStrategy())
            .build();

    try (RestClient client = RestClient
            .builder(HttpHost.create(container.getHttpHostAddress()))
            .setHttpClientConfigCallback(httpClientBuilder -> {
                return httpClientBuilder
                        .setSSLContext(sslcontext)
                        .setDefaultCredentialsProvider(credentialsProvider);
            })
            .build()) {

        final Response response = client.performRequest(new Request("GET", "/_cluster/health"));
        ...
    }
}
```

When [security plugin](https://github.com/opensearch-project/security) is not required (not recommended for production), just use `OpenSearchContainer` default constructor.

```java

import org.apache.http.HttpHost;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;

private static final DockerImageName OPENSEARCH_IMAGE = DockerImageName.parse("opensearchproject/opensearch:2.11.0");

// Create the OpenSearch container.
try (OpenSearchContainer<?> container = new OpenSearchContainer<>(OPENSEARCH_IMAGE)) {
    // Start the container. This step might take some time...
    container.start();

    try (RestClient client = RestClient
            .builder(HttpHost.create(container.getHttpHostAddress()))
            .build()) {

        final Response response = client.performRequest(new Request("GET", "/_cluster/health"));
        ...
    }
}
```

## Code of Conduct

This project has adopted the [Amazon Open Source Code of Conduct](CODE_OF_CONDUCT.md). For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq), or contact [opensource-codeofconduct@amazon.com](mailto:opensource-codeofconduct@amazon.com) with any additional questions or comments.

## License
`opensearch-testcontainers` is licensed under the Apache license, version 2.0. Full license text is available in the [LICENSE](LICENSE) file.

Please note that the project explicitly does not require a CLA (Contributor License Agreement) from its contributors.

## Copyright

Copyright OpenSearch Contributors. See [NOTICE](NOTICE.txt) for details.

## Security

To report any possible vulnerabilities or other serious issues please see our [security](SECURITY.md) policy or refer to [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## Contributing

Bug reports and patches are very welcome, please post them as GitHub issues and pull requests at `https://github.com/opensearch-project/opensearch-testcontainers`. Please check out [CONTRIBUTING](CONTRIBUTING.md) for more information.
