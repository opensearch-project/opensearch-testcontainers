/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.testcontainers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        final Collection<OpensearchContainer<?>> nodes = new ArrayList<>();
        try (OpensearchContainer<?> master = new OpensearchContainer<>(image)
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
                final OpensearchContainer<?> container = new OpensearchContainer<>(image)
                        .withEnv(newEnv)
                        .withNetwork(network)
                        .withSecurityEnabled()
                        .withNetworkAliases("node" + node);
                nodes.add(container);
            }

            // Start both containers at once
            nodes.parallelStream().forEach(OpensearchContainer::start);

            try (RestClient client = getClient(master)) {
                Response response = client.performRequest(new Request("GET", "/"));
                assertThat(response.getStatusLine().getStatusCode(), is(200));
                assertThat(EntityUtils.toString(response.getEntity()), containsString(version));
                response = client.performRequest(new Request("GET", "/_cluster/health"));
                assertThat(EntityUtils.toString(response.getEntity()), containsString("\"number_of_nodes\":3"));
            }
        } finally {
            nodes.parallelStream().forEach(OpensearchContainer::close);
            network.close();
        }
    }

    @DisplayName("Create OpenSearchContainer with security disabled")
    @ParameterizedTest(name = "Running OpenSearch version={0} (security disabled)")
    @MethodSource("containers")
    public void cluster(final String version, final Map<String, String> env, final DockerImageName image)
            throws Exception {
        final Network network = Network.newNetwork();
        final Collection<OpensearchContainer<?>> nodes = new ArrayList<>();
        try (OpensearchContainer<?> master = new OpensearchContainer<>(image)
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
                final OpensearchContainer<?> container = new OpensearchContainer<>(image)
                        .withEnv(newEnv)
                        .withNetwork(network)
                        .withNetworkAliases("node" + node);
                nodes.add(container);
            }

            // Start both containers at once
            nodes.parallelStream().forEach(OpensearchContainer::start);

            try (RestClient client = getClient(master)) {
                Response response = client.performRequest(new Request("GET", "/"));
                assertThat(response.getStatusLine().getStatusCode(), is(200));
                assertThat(EntityUtils.toString(response.getEntity()), containsString(version));
                response = client.performRequest(new Request("GET", "/_cluster/health"));
                assertThat(EntityUtils.toString(response.getEntity()), containsString("\"number_of_nodes\":3"));
            }
        } finally {
            nodes.parallelStream().forEach(OpensearchContainer::close);
            network.close();
        }
    }

    private static Stream<Arguments> containers() {
        return Stream.of(Arguments.of(
                "2.19.2",
                Map.of(
                        "discovery.type",
                        "zen",
                        "cluster.initial_cluster_manager_nodes",
                        "master",
                        "node.name",
                        "master"),
                DockerImageName.parse("opensearchproject/opensearch").withTag("2.19.2")));
    }

    private RestClient getClient(OpensearchContainer<?> container)
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

    private Optional<CredentialsProvider> getCredentialsProvider(OpensearchContainer<?> container) {
        if (!container.isSecurityEnabled()) {
            return Optional.empty();
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY, new UsernamePasswordCredentials(container.getUsername(), container.getPassword()));

        return Optional.of(credentialsProvider);
    }
}
