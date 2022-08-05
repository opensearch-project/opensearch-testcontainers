/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.testcontainers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.testcontainers.utility.DockerImageName;

class OpensearchContainerTest {
    @DisplayName("Create default OpensearchContainer with security enabled")
    @ParameterizedTest(name = "Running Opensearch version={0} (security enabled)")
    @MethodSource("containers")
    public void defaultWithSecurity(final String version, final DockerImageName image) throws Exception {
        try (OpensearchContainer container = new OpensearchContainer(image).withSecurityEnabled()) {
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

    @DisplayName("Create OpensearchContainer with security disabled")
    @ParameterizedTest(name = "Running Opensearch version={0} (security disabled)")
    @MethodSource("containers")
    public void defaultNoSecurity(final String version, final DockerImageName image) throws Exception {
        try (OpensearchContainer container = new OpensearchContainer(image)) {
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
                        DockerImageName.parse("opensearchproject/opensearch").withTag("1.3.4")),
                Arguments.of(
                        "2.0.1",
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.0.1")),
                Arguments.of(
                        "2.1.0",
                        DockerImageName.parse("opensearchproject/opensearch").withTag("2.1.0")));
    }

    private RestClient getClient(OpensearchContainer container)
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        final CredentialsProvider credentialsProvider =
                getCredentialsProvider(container).orElse(null);

        final SSLContext sslcontext = SSLContextBuilder.create()
                .loadTrustMaterial(null, new TrustAllStrategy())
                .build();

        return RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    return httpClientBuilder
                            .setSSLContext(sslcontext)
                            .setDefaultCredentialsProvider(credentialsProvider);
                })
                .build();
    }

    private Optional<CredentialsProvider> getCredentialsProvider(OpensearchContainer container) {
        if (!container.isSecurityEnabled()) {
            return Optional.empty();
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY, new UsernamePasswordCredentials(container.getUsername(), container.getPassword()));

        return Optional.of(credentialsProvider);
    }
}
