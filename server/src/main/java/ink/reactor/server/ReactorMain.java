package ink.reactor.server;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import ink.reactor.api.Reactor;
import ink.reactor.api.ReactorServer;
import ink.reactor.api.config.YamlConfigManager;
import ink.reactor.api.config.server.ServerConfig;
import ink.reactor.api.player.connection.ProtocolConnector;
import ink.reactor.server.config.ServerConfigLoader;
import ink.reactor.server.console.Console;
import ink.reactor.server.console.ConsoleStart;
import ink.reactor.server.player.ReactorPlayer;
import ink.reactor.server.tick.MainThread;
import ink.reactor.protocol.ServerConnection;

public final class ReactorMain {
    public static void main(String[] args) {
        final Console console = startServer();
        if (console != null) {
            console.run();
        }
    }

    private static Console startServer() {
        final long time = System.currentTimeMillis();

        final ServerConnection serverConnection = new ServerConnection();
        final File mainDirectory = new File("").getAbsoluteFile();
        final ServerConfig config = new ServerConfigLoader(mainDirectory)
            .load(new YamlConfigManager(mainDirectory, new Yaml(), ReactorServer.class.getClassLoader()));

        try {
            serverConnection.connect(config.ip(), config.port());
        } catch (final Exception e) {
            Logger.error("Error starting the server");
            Logger.error(e);
            return null;
        }

        final Console console = new ConsoleStart().createConsole();
        if (console == null) {
            return null;
        }

        final ScheduledExecutorService mainExecutorService = Executors.newScheduledThreadPool(1);
        final MainThread mainThread = new MainThread(serverConnection, mainExecutorService);
        final ReactorServerImpl server = new ReactorServerImpl(config, mainThread, console);

        Reactor.setServer(server);
        Runtime.getRuntime().addShutdownHook(new Thread(server::onExit));
        ProtocolConnector.setPlayerCreator(ReactorPlayer::new);

        server.getPluginManager().loadPlugins(new File(mainDirectory, "plugins"));

        // Disable only for debug mode
         Debug.startDebugPlugin();

        server.getPluginManager().enablePlugins();

        serverConnection.registerDefaultHandlers();

        console.sendMessage("Server started in " + (System.currentTimeMillis() - time) + "ms");
        return console;
    }
}