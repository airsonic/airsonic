package org.airsonic.test.cucumber.server;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.RequestBuilder;

import java.nio.file.Path;

public interface AirsonicServer {
    String getBaseUri();

    void uploadToDefaultMusicFolder(Path directoryPath, String relativePath);

    default void addRestParameters(RequestBuilder builder) {
        builder.addParameter("c", "inttest");
        builder.addParameter("v", "1.15.0");
        builder.addParameter("u", "admin");
        builder.addParameter("s", "int");
        builder.addParameter("t", DigestUtils.md5Hex("admin" + "int"));
    }
}
