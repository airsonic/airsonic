package org.libresonic.player;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

public class TestServer {

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/libresonic");
        String baseDir = TestServer.class.getResource("/" + TestServer.class.getName().replaceAll("\\.", "/") + ".class").getFile();
        baseDir = baseDir.substring(0,baseDir.indexOf("/target"));
        webapp.setWar(baseDir + "/src/main/webapp");
        server.setHandler(webapp);
        server.setAttribute("reload", "automatic");
        server.start();
        server.join();
    }
}