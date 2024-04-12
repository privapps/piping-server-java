package pipingj;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.function.Consumer;

@UtilityClass
@Slf4j
public class HandlerUtil {
    static final int BUFFER_SIZE = 1024 * 8;
    private final TimedConcurrentHashMap<String, StreamPipe> pipes = new TimedConcurrentHashMap<>();

    void handleSenderRequest(HttpServletRequest request, HttpServletResponse response, Consumer<StreamPipe> consumer) throws ServletException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        String path = request.getRequestURI();
        int numReceivers = parseNumReceivers(request);
        if (pipes.containsKey(path)) { // receivers connect first
            StreamPipe pipe = pipes.get(path);
            if (pipe.isSenderUp()) { // already have a sender
                response.setStatus(HttpServletResponse.SC_CONFLICT);
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
            consumer.accept(pipe);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException(e);
        } finally {
            pipes.remove(path);
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }

    void handleReceiverRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        String path = request.getRequestURI();
        StreamPipe pipe = pipes.computeIfAbsent(path, k -> new StreamPipe());
        try (PipedOutputStream pos = new PipedOutputStream();
             InputStream is = new PipedInputStream(pos);
             OutputStream os = response.getOutputStream()) {
            pipe.outputs.add(pos);
            pipe.responses.add(response);
            if (pipe.isSenderUp()) {
                pipe.receiverReady(); // Indicate that one receiver is ready
                pipe.awaitReceivers();
            }
            response.setStatus(HttpServletResponse.SC_OK);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException(e);
        }
    }

    int parseNumReceivers(HttpServletRequest request) {
        int num = 1;
        try {
            String nParam = request.getParameter("n");
            if (null != nParam) {
                num = Integer.parseInt(nParam);
            }
        } catch (Exception e) {
            log.warn("failed to parse n", e);
        }
        return num;
    }
}
