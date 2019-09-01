package org.airsonic.test.cucumber_hooks.local;

import org.airsonic.test.cucumber.server.AirsonicServer;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Profile("local")
public class LocalServer implements AirsonicServer, EnvironmentAware, InitializingBean {
    private static final String AIRSONIC_SERVER_PORT = "airsonic.server.port";
    private static final String AIRSONIC_SERVER_DEFAULT_MUSIC_DIR = "airsonic.server.default.music.dir";

    private int port;
    private String defaultMusicDir;

    @Override
    public String getBaseUri() {
        return "http://localhost:" + port;
    }

    @Override
    public void uploadToDefaultMusicFolder(Path directoryPath, String relativePath) {
        Path dest = Paths.get(defaultMusicDir, relativePath);
        try {
            FileUtils.copyDirectory(directoryPath.toFile(), dest.toFile(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void setEnvironment(Environment environment) {
        port = Integer.parseInt(environment.getRequiredProperty(AIRSONIC_SERVER_PORT));
        defaultMusicDir = environment.getRequiredProperty(AIRSONIC_SERVER_DEFAULT_MUSIC_DIR);
    }
}
