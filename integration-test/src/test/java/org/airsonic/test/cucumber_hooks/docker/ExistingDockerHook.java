package org.airsonic.test.cucumber_hooks.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.AttachedNetwork;
import com.spotify.docker.client.messages.ContainerInfo;
import org.airsonic.test.cucumber.server.AirsonicServer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Component
@Profile("existing")
public class ExistingDockerHook implements AirsonicServer, EnvironmentAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ExistingDockerHook.class);
    public static final String AIRSONIC_DOCKER_CONTAINER = "airsonic.docker.container";
    public static final String AIRSONIC_DOCKER_PORT = "airsonic.docker.port";

    private String serverUri = null;
    private final DockerClient docker;
    private String containerId;
    private Integer dockerPort;

    public ExistingDockerHook() {
        logger.debug("Using hook for existing docker container");
        docker = new DefaultDockerClient("unix:///var/run/docker.sock");
        testDockerIsAvail();
    }

    private void testDockerIsAvail() {
        try {
            logger.trace("Trying to ping docker daemon");
            docker.ping();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBaseUri() {
        if (serverUri == null) {
            throw new IllegalStateException("Server is not yet ready");
        } else {
            return serverUri;
        }
    }

    @Override
    public void uploadToDefaultMusicFolder(Path localDir, String relativePath) {
        try {
            // TODO ensure localDir is a directory
            docker.copyToContainer(localDir, containerId, "/airsonic/music/" + relativePath);
        } catch (DockerException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void connectToServer() {
        logger.debug("Connecting to server");

        try {
            ContainerInfo containerInfo = docker.inspectContainer(containerId);
            if(!containerInfo.state().running()) {
                throw new IllegalStateException("Container is not running " + containerId);
            }
            Map.Entry<String, AttachedNetwork> next = containerInfo.networkSettings().networks().entrySet().iterator().next();
            String ipAddress = next.getValue().ipAddress();
            if(StringUtils.isBlank(ipAddress)) {
                throw new IllegalStateException("No address found for container " + containerId);
            }
            serverUri = "http://" + ipAddress + ":" + dockerPort;
        } catch (DockerException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        containerId = environment.getRequiredProperty(AIRSONIC_DOCKER_CONTAINER);
        dockerPort = Integer.parseInt(environment.getRequiredProperty(AIRSONIC_DOCKER_PORT));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connectToServer();
    }
}
