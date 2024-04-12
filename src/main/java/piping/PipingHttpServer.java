package piping;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class PipingHttpServer {
    private static final Logger logger = Logger.getLogger(PipingHttpServer.class.getName());

    private final HttpServer server;

    public PipingHttpServer(int port) throws IOException {
        HttpHandler notFound = exchange -> {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
        };
        server = HttpServer.create(new InetSocketAddress(port), 0);
        List.of("/favicon.ico", "/robots.txt", "/help", "/version", "/noscript").forEach(path -> {
            server.createContext(path, notFound);
        });

        server.createContext("/", new DataHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        logger.info("Server is listening on port " + port);
    }

    public static void main(String[] args) throws IOException {
        int port = getIntFromEnv("PORT", 8088);
        new PipingHttpServer(port);
    }

    private static int getIntFromEnv(String param, int defaultVal) {
        try {
            String portStr = System.getenv(param);
            if (null != portStr)
                return Integer.parseInt(portStr);
        } catch (Exception e) {
            // do nothing
        }
        return defaultVal;
    }

}
