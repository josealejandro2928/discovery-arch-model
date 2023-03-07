package org.server.app.utils;

import org.server.app.data.ConfigUserModel;
import org.server.app.data.UserModel;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class JupyterServersController {
    int maxJupyterServers = 5;
    int port = 8015;
    static JupyterServersController instance = null;
    Map<String, JupyterServer> servers = new HashMap<>();
    ReentrantLock lock = new ReentrantLock();

    public static JupyterServersController getInstance() {
        if (instance != null) return instance;
        instance = new JupyterServersController();
        return instance;
    }

    public Map<String, String> startNewSession(UserModel user, ConfigUserModel configUserModel) throws IOException {
        if (this.servers.containsKey(user.email)) {
            JupyterServer jp = this.servers.get(user.email);
            jp.stopServer();
            this.servers.remove(user.email);
        }
        ConfigServer configServer = ConfigServer.getInstance();
        int serverPort = this.findAPort();
        String pathToDir = Paths.get(configUserModel.getRootPath(), "jupyter").toAbsolutePath().toString();
        JupyterServer jp = new JupyterServer(serverPort, user, pathToDir);
        jp.startServer();
        this.servers.put(user.email, jp);
        Map<String, String> data = new HashMap<>();
        data.put("user", user.email);
        data.put("url", configServer.dotenv.get("HOST") + ":" + serverPort);
        System.out.println("Open jupyter at: " + String.format("http://%s", data.get("url")));
        return data;
    }

    int findAPort() {
        this.lock.lock();
        int basePort = this.port;
        while (true) {
            boolean foundAvailablePort = true;
            for (JupyterServer jp : this.servers.values()) {
                if (jp.port == basePort) {
                    foundAvailablePort = false;
                    break;
                }
            }
            if (foundAvailablePort) break;
            basePort += 1;
        }
        this.lock.unlock();
        return basePort;
    }

    public void stopSession(UserModel user) throws IOException {
        if (this.servers.containsKey(user.email)) {
            JupyterServer jp = this.servers.get(user.email);
            jp.stopServer();
            this.servers.remove(user.email);
        }
    }

    public class JupyterServer {
        int port;
        UserModel user;
        ProcessBuilder processBuilder = null;
        Process process = null;
        String pathFolder = null;

        JupyterServer(int p, UserModel user, String path) {
            this.port = p;
            this.user = user;
            this.pathFolder = path;
        }

        void startServer() throws IOException {
            this.processBuilder = new ProcessBuilder("jupyter-lab", "--notebook-dir=" + this.pathFolder,
                    "--no-browser", String.format("--port=%s", this.port));
            this.process = this.processBuilder.start();
        }

        void stopServer() {
            this.process.destroy();
            this.processBuilder = null;
            this.process = null;
        }
    }
}


