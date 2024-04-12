package piping;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.logging.Logger;

class DataHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(DataHandler.class.getName());
    private static final int BUFFER_SIZE = 1024 * 8;
    private static final String HEADER_ORIGIN = "Access-Control-Allow-Origin";

    private final TimedConcurrentHashMap<String, StreamPipe> pipes = new TimedConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        logger.info(method + "\t" + path);
        try {
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set(HEADER_ORIGIN, "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
                handleSenderRequest(exchange, path);
            } else if ("GET".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set(HEADER_ORIGIN, "*");
                handleReceiverRequest(exchange, path);
            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set(HEADER_ORIGIN, "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "*");
                exchange.sendResponseHeaders(200, -1); // Method Not Allowed
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        } catch (Exception e) {
            logger.warning("failed to handle " + exchange + "\n" + e);
        } finally {
            exchange.close();
        }
    }

    private void handleSenderRequest(HttpExchange exchange, String path) throws IOException {
        int numReceivers = parseNumReceivers(exchange);
        if (numReceivers == -1) return;
        if (pipes.containsKey(path)) { // receivers connect first
            StreamPipe pipe = pipes.get(path);
            if (pipe.isSenderUp()) { // already have a sender
                exchange.sendResponseHeaders(409, -1);
                return;
            } else {
                pipe.initialCountDownLatch(numReceivers - pipe.outputs.size());
            }
        } else {
            pipes.computeIfAbsent(path, k -> new StreamPipe(numReceivers));
        }
        StreamPipe pipe = pipes.get(path);
        // Signal sender is ready
        try {
            pipe.awaitReceivers();
            pipe.process(exchange, BUFFER_SIZE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            pipes.remove(path);
        }
        exchange.sendResponseHeaders(200, -1);
    }

    private void handleReceiverRequest(HttpExchange exchange, String path) throws IOException {
        StreamPipe pipe = pipes.computeIfAbsent(path, k -> new StreamPipe());
        try (PipedOutputStream pos = new PipedOutputStream();
             InputStream is = new PipedInputStream(pos);
             OutputStream os = exchange.getResponseBody()) {
            pipe.outputs.add(pos);
            pipe.outputExchanges.add(exchange);
            if (pipe.isSenderUp()) {
                pipe.receiverReady(); // Indicate that one receiver is ready
                pipe.awaitReceivers();
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private int parseNumReceivers(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        int numReceivers = 1; // Default is one receiver

        if (query != null) {
            String[] queryParams = query.split("&");
            for (String param : queryParams) {
                String[] keyValue = param.split("=");
                if ("n".equals(keyValue[0]) && keyValue.length == 2) {
                    try {
                        return Integer.parseInt(keyValue[1]);
                    } catch (NumberFormatException e) {
                        // do nothing
                    }
                }
            }
        }
        return numReceivers;
    }
}

