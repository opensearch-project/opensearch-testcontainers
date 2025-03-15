/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.testcontainers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.testcontainers.utility.DockerImageName;

class OpenSearchContainerTest {
    @DisplayName("Create default OpenSearchContainer with security enabled")
    @ParameterizedTest(name = "Running OpenSearch version={0} (security enabled)")
    @MethodSource("containers")
    public void defaultWithSecurity(final String version, final Map<String, String> env, final DockerImageName image)
            throws Exception {
        try (OpenSearchContainer<?> container =
                new OpenSearchContainer<>(image).withEnv(env).withSecurityEnabled()) {
            container.start();

            try (RestClient client = getClient(container)) {
                Response response = client.performRequest(new Request("GET", "/"));

                assertThat(response.getStatusLine().getStatusCode(), is(200));
                assertThat(EntityUtils.toString(response.getEntity()), containsString(version));

                response = client.performRequest(new Request("GET", "/_plugins/_security/health"));
                assertThat(response.getStatusLine().getStatusCode(), is(200));
                // For now we test that we have the monitoring feature available
                assertThat(EntityUtils.toString(response.getEntity()), containsString("strict"));
            }
        }
    }

    @DisplayName("Create OpenSearchContainer with security disabled")
    @ParameterizedTest(name = "Running OpenSearch version={0} (security disabled)")
    @MethodSource("containers")
    public void defaultNoSecurity(final String version, final Map<String, String> env, final DockerImageName image)
            throws Exception {
        try (OpenSearchContainer<?> container = new OpenSearchContainer<>(image).withEnv(env)) {
            container.start();

            try (RestClient client = getClient(container)) {
                Response response = client.performRequest(new Request("GET", "/"));

                assertThat(response.getStatusLine().getStatusCode(), is(200));
                assertThat(EntityUtils.toString(response.getEntity()), containsString(version));

                // No such handler should be thrown by the server
                assertThrows(
                        ResponseException.class,
                        () -> client.performRequest(new Request("GET", "/_plugins/_security/health")));
            }
        }
    }

    private static Stream<Arguments> containers() {
        return Stream.of(
                Arguments.of(
                        "1.3.4",
                        Map.of(), /* empty env */
                        DockerImageName.parse("opensearchproject/opensearch").withTag("1.3.4")),
                Arguments.of(
                        "2.0.1",
                        Map.of(), /* empty env */
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.0.1")),
                Arguments.of(
                        "2.1.0",
                        Map.of(), /* empty env */
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.1.0")),
                Arguments.of(
                        "2.11.0",
                        Map.of(), /* empty env */
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.11.0")),
                Arguments.of(
                        "2.15.0",
                        Map.of("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "_oop0m#NsR_"),
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.15.0")),
                Arguments.of(
                        "2.17.0",
                        Map.of(), /* empty env */
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.17.0")),
                Arguments.of(
                        "2.19.1",
                        Map.of(), /* empty env */
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.19.1")));
    }

    private RestClient getClient(OpenSearchContainer<?> container)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException {
        final HttpHost host = HttpHost.create(container.getHttpHostAddress());

        final CredentialsProvider credentialsProvider =
                getCredentialsProvider(container, host).orElse(null);

        final SSLContext sslcontext = SSLContextBuilder.create()
                .loadTrustMaterial(null, new TrustAllStrategy())
                .build();

        return RestClient.builder(host)
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                            .setSslContext(sslcontext)
                            .build();
                    final PoolingAsyncClientConnectionManager connectionManager =
                            PoolingAsyncClientConnectionManagerBuilder.create()
                                    .setTlsStrategy(tlsStrategy)
                                    .build();
                    return httpClientBuilder
                            .setConnectionManager(connectionManager)
                            .setDefaultCredentialsProvider(credentialsProvider);
                })
                .build();
    }

    private Optional<CredentialsProvider> getCredentialsProvider(OpenSearchContainer<?> container, HttpHost host) {
        if (!container.isSecurityEnabled()) {
            return Optional.empty();
        }

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(host),
                new UsernamePasswordCredentials(
                        container.getUsername(), container.getPassword().toCharArray()));

        return Optional.of(credentialsProvider);
    }
}
