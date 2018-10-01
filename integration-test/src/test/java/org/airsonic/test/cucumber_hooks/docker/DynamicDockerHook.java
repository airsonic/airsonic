package org.airsonic.test.cucumber_hooks.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.airsonic.test.cucumber.server.AirsonicServer;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static com.spotify.docker.client.DockerClient.RemoveContainerParam.*;

@Component
@Profile("dynamic")
public class DynamicDockerHook implements AirsonicServer, EnvironmentAware, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDockerHook.class);
    public static final String AIRSONIC_DOCKER_IMAGE = "airsonic.docker.image";
    public static final String AIRSONIC_DOCKER_PORT = "airsonic.docker.port";
    public static final String AIRSONIC_READY_MAX_WAIT = "airsonic.ready.max_wait";
    public static final String AIRSONIC_READY_SLEEP_TIME = "airsonic.ready.sleep_time";

    private String serverUri = null;
    private final DockerClient docker;
    private String containerId;
    private String dockerImage;
    private Integer dockerPort;
    private Long readyMaxWaitTime;
    private Long readySleepTime;

    public DynamicDockerHook() {
        logger.debug("Using hook for dynamically creating containers");
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

    public void startServer() {
        logger.debug("Starting server");
        final ContainerConfig config = ContainerConfig.builder()
                .image(dockerImage)
                .build();

        final String name = "airsonic-it-" + RandomStringUtils.randomAlphabetic(10);
        try {
            final ContainerCreation containerCreate = docker.createContainer(config, name);
            containerId = containerCreate.id();
            docker.startContainer(containerId);
            Long waitTime = readyMaxWaitTime;
            while(true) {
                ContainerInfo containerInfo = docker.inspectContainer(containerId);
                ContainerState.Health health = containerInfo.state().health();
                if (health != null && StringUtils.equalsIgnoreCase(health.status(), "healthy")) {
                    logger.trace("Container started early. Yay!");
                    break;
                } else if(waitTime > readySleepTime) {
                    if(logger.isTraceEnabled()) {
                        String message;
                        if(health != null) {
                            message = "Container ("+name+") not yet ready. State was: " + health.status();
                        } else {
                            message = "Container ("+name+") state unknown. Waiting";
                        }
                        logger.trace(message);
                    }
                    waitTime -= readySleepTime;
                    Thread.sleep(readySleepTime);
                } else if(health == null) {
                    logger.trace("Max wait time with unknown container state. Hoping container is ready");
                    break;
                } else {
                    logger.trace("Container ("+name+") never became ready within max wait time");
                    throw new RuntimeException("Container ("+name+") not ready");
                }
            }
            ContainerInfo containerInfo = docker.inspectContainer(containerId);
            try {
                Map.Entry<String, AttachedNetwork> next = containerInfo.networkSettings().networks().entrySet().iterator().next();
                String ipAddress = next.getValue().ipAddress();
                serverUri = "http://" + ipAddress + ":" + dockerPort;
            } catch(Exception e) {
                throw new RuntimeException("Could not determine container ("+name+") address", e);
            }
        } catch (DockerException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopServer() {
        if(containerId != null) {
            try {
                docker.removeContainer(containerId, forceKill(), removeVolumes());
            } catch (DockerException | InterruptedException e) {
                throw new RuntimeException("Could not remove container", e);
            }
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        dockerImage = environment.getRequiredProperty(AIRSONIC_DOCKER_IMAGE);
        dockerPort = Integer.parseInt(environment.getRequiredProperty(AIRSONIC_DOCKER_PORT));
        readyMaxWaitTime = Long.parseLong(environment.getRequiredProperty(AIRSONIC_READY_MAX_WAIT));
        readySleepTime = Long.parseLong(environment.getRequiredProperty(AIRSONIC_READY_SLEEP_TIME));
        if(readyMaxWaitTime <= 0L || readySleepTime <= 0L) {
            throw new IllegalArgumentException("Max wait time and sleep time must be greater than 0");
        }
    }

    @Override
    public void destroy() throws Exception {
        stopServer();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        startServer();
    }
}
