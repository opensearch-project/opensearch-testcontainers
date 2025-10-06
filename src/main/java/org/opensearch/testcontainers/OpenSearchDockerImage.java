/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.testcontainers;

import org.testcontainers.utility.DockerImageName;

/**
 * Official Docker images of the OpenSearch project: https://hub.docker.com/r/opensearchproject/opensearch
 */
public final class OpenSearchDockerImage {
    public static DockerImageName ofVersion(String version) {
        return ofTag(version);
    }

    public static DockerImageName ofTag(String tag) {
        return DockerImageName.parse("opensearchproject/opensearch:" + tag);
    }
}
