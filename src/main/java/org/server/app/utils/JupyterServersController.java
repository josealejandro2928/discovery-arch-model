package org.server.app.utils;

import org.server.app.data.ConfigUserModel;
import org.server.app.data.UserModel;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class JupyterServersController {
    int maxJupyterServers = 5;
    int port = 8015;
    static JupyterServersController instance = null;
    Map<String, JupyterServer> servers = new HashMap<>();

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
        JupyterServer jp = new JupyterServer(this.port, user, Paths.get(configUserModel.getRootPath(), "jupyter").toAbsolutePath().toString());
        jp.startServer();
        this.servers.put(user.email, jp);
        Map<String, String> data = new HashMap<>();
        data.put("user", user.email);
        data.put("url", configServer.dotenv.get("HOST") + ":" + this.port);
        this.port++;
        return data;
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


