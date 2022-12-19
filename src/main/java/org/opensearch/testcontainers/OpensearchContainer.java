/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.testcontainers;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.net.InetSocketAddress;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

/**
 * The Opensearch Docker container (single node cluster) which exposes by default ports 9200
 * (http/https) and 9300 (tcp, deprecated).
 */
public class OpensearchContainer extends GenericContainer<OpensearchContainer> {
    // Default username to connect to Opensearch instance
    private static final String DEFAULT_USER = "admin";
    // Default password to connect to Opensearch instance
    private static final String DEFAULT_PASSWORD = "admin";

    // Default HTTP port.
    private static final int DEFAULT_HTTP_PORT = 9200;

    // Default TCP port (deprecated and may be removed in future versions).
    private static final int DEFAULT_TCP_PORT = 9300;

    // Opensearch Docker base image.
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("opensearchproject/opensearch");

    // Opensearch Docker base image hosted on Amazon ECR
    private static final DockerImageName ECR_IMAGE_NAME =
            DockerImageName.parse("public.ecr.aws/opensearchproject/opensearch");

    // Disables (or enables) security plugin. If security is enabled, the communication protocol switches from HTTP to
    // HTTPs,
    // along with Basic Auth being used.
    private boolean disableSecurity = true;

    /**
     * Create an Opensearch Container by passing the full docker image name.
     *
     * @param dockerImageName Full docker image name as a {@link String}, like:
     *     opensearchproject/opensearch:1.2.4 opensearchproject/opensearch:1.3.1
     *     opensearchproject/opensearch:2.0.0 or public.ecr.aws/opensearchproject/opensearch:2.4.1
     */
    public OpensearchContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an Opensearch Container (with security plugin enabled) by passing the full docker image
     * name.
     *
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like:
     *
     *      DockerImageName.parse("opensearchproject/opensearch:1.2.4")
     *      DockerImageName.parse("opensearchproject/opensearch:1.3.1")
     *      DockerImageName.parse("opensearchproject/opensearch:2.0.0")
     *
     */
    public OpensearchContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, ECR_IMAGE_NAME);
    }

    /**
     * Should the security plugin be enabled or stay disabled (default value). If the security
     * plugin is enabled, HTTPS protocol is going to be used along with the default username / password.
     * @return this container instance
     */
    public OpensearchContainer withSecurityEnabled() {
        this.disableSecurity = false;
        return this;
    }

    @Override
    protected void configure() {
        super.configure();

        withNetworkAliases("opensearch-" + Base58.randomString(6));
        withEnv("discovery.type", "single-node");
        if (disableSecurity) {
            withEnv("DISABLE_SECURITY_PLUGIN", Boolean.toString(disableSecurity));
        }
        addExposedPorts(DEFAULT_HTTP_PORT, DEFAULT_TCP_PORT);

        final WaitStrategy waitStrategy;
        if (!disableSecurity) {
            // By default, Opensearch uses self-signed certificates for HTTPS, allowing insecure
            // connection in order to skip the certificate validation checks.
            waitStrategy = new HttpWaitStrategy()
                    .usingTls()
                    .allowInsecure()
                    .forPort(DEFAULT_HTTP_PORT)
                    .withBasicCredentials(DEFAULT_USER, DEFAULT_PASSWORD)
                    .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
                    .withReadTimeout(Duration.ofSeconds(10))
                    .withStartupTimeout(Duration.ofMinutes(5));
        } else {
            waitStrategy = new HttpWaitStrategy()
                    .forPort(DEFAULT_HTTP_PORT)
                    .forStatusCodeMatching(response -> response == HTTP_OK)
                    .withReadTimeout(Duration.ofSeconds(10))
                    .withStartupTimeout(Duration.ofMinutes(5));
        }

        setWaitStrategy(waitStrategy);
    }

    /**
     * Return HTTP(s) host and port to connect to Opensearch container.
     *
     * @return HTTP(s) host and port (in a form of "host:port")
     */
    public String getHttpHostAddress() {
        return (disableSecurity ? "http://" : "https://") + getHost() + ":" + getMappedPort(DEFAULT_HTTP_PORT);
    }

    /**
     * Check if security plugin was enabled or not for this container
     *
     * @return "true" if if security plugin was enabled for this container, "false" otherwise
     */
    public boolean isSecurityEnabled() {
        return !disableSecurity;
    }

    /**
     * Return socket address to connect to Opensearch over TCP. The TransportClient will is deprecated
     * and may be removed in future versions.
     *
     * @return TCP socket address
     */
    @Deprecated
    public InetSocketAddress getTcpHost() {
        return new InetSocketAddress(getHost(), getMappedPort(DEFAULT_TCP_PORT));
    }

    /**
     * Return user name to connect to Opensearch container (if security plugin is enabled)
     * @return user name to connect to Opensearch container
     */
    public String getUsername() {
        return DEFAULT_USER;
    }

    /**
     * Return password to connect to Opensearch container (if security plugin is enabled)
     * @return password to connect to Opensearch container
     */
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }
}
