/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.testcontainers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.opensearch.client.RestClient;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

class OpenSearchContainerClusterTest {
    @DisplayName("Create default OpenSearchContainer with security enabled")
    @ParameterizedTest(name = "Running OpenSearch version={0} (security enabled)")
    @MethodSource("containers")
    public void defaultWithSecurity(final String version, final Map<String, String> env, final DockerImageName image)
            throws Exception {
        final Network network = Network.newNetwork();
        final Collection<OpenSearchContainer<?>> nodes = new ArrayList<>();
        try (OpenSearchContainer<?> master = new OpenSearchContainer<>(image)
                .withEnv(env)
                .withNetwork(network)
                .withSecurityEnabled()
                .withNetworkAliases("master")) {
            master.start();

            final Map<String, String> newEnv = new HashMap<>();
            newEnv.putAll(env);
            newEnv.put("cluster.initial_master_nodes", "master");
            newEnv.put("discovery.seed_hosts", "master");
            for (int node = 1; node < 3; ++node) {
                newEnv.put("node.name", "node" + node);
                @SuppressWarnings("resource")
                final OpenSearchContainer<?> container = new OpenSearchContainer<>(image)
                        .withEnv(newEnv)
                        .withNetwork(network)
                        .withSecurityEnabled()
                        .withNetworkAliases("node" + node);
                nodes.add(container);
            }

            // Start both containers at once
            nodes.parallelStream().forEach(OpenSearchContainer::start);

            try (RestClient client = getClient(master)) {
                Response response = client.performRequest(new Request("GET", "/"));
                assertThat(response.getStatusLine().getStatusCode(), is(200));
                assertThat(EntityUtils.toString(response.getEntity()), containsString(version));
                response = client.performRequest(new Request("GET", "/_cluster/health"));
                assertThat(EntityUtils.toString(response.getEntity()), containsString("\"number_of_nodes\":3"));
            }
        } finally {
            nodes.parallelStream().forEach(OpenSearchContainer::close);
            network.close();
        }
    }

    @DisplayName("Create OpenSearchContainer with security disabled")
    @ParameterizedTest(name = "Running OpenSearch version={0} (security disabled)")
    @MethodSource("containers")
    public void cluster(final String version, final Map<String, String> env, final DockerImageName image)
            throws Exception {
        final Network network = Network.newNetwork();
        final Collection<OpenSearchContainer<?>> nodes = new ArrayList<>();
        try (OpenSearchContainer<?> master = new OpenSearchContainer<>(image)
                .withEnv(env)
                .withNetwork(network)
                .withNetworkAliases("master")) {
            master.start();

            final Map<String, String> newEnv = new HashMap<>();
            newEnv.putAll(env);
            newEnv.put("cluster.initial_master_nodes", "master");
            newEnv.put("discovery.seed_hosts", "master");
            for (int node = 1; node < 3; ++node) {
                newEnv.put("node.name", "node" + node);
                @SuppressWarnings("resource")
                final OpenSearchContainer<?> container = new OpenSearchContainer<>(image)
                        .withEnv(newEnv)
                        .withNetwork(network)
                        .withNetworkAliases("node" + node);
                nodes.add(container);
            }

            // Start both containers at once
            nodes.parallelStream().forEach(OpenSearchContainer::start);

            try (RestClient client = getClient(master)) {
                Response response = client.performRequest(new Request("GET", "/"));
                assertThat(response.getStatusLine().getStatusCode(), is(200));
                assertThat(EntityUtils.toString(response.getEntity()), containsString(version));
                response = client.performRequest(new Request("GET", "/_cluster/health"));
                assertThat(EntityUtils.toString(response.getEntity()), containsString("\"number_of_nodes\":3"));
            }
        } finally {
            nodes.parallelStream().forEach(OpenSearchContainer::close);
            network.close();
        }
    }

    private static Stream<Arguments> containers() {
        return Stream.of(Arguments.of(
                "3.1.0",
                Map.of(
                        "discovery.type",
                        "zen",
                        "cluster.initial_cluster_manager_nodes",
                        "master",
                        "node.name",
                        "master"),
                OpenSearchDockerImage.ofVersion("3.1.0")));
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
