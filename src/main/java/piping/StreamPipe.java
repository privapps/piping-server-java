package piping;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@NoArgsConstructor
class StreamPipe {

    private static final Logger logger = Logger.getLogger(StreamPipe.class.getName());
    final Queue<PipedOutputStream> outputs = new ConcurrentLinkedQueue<>();
    final Queue<HttpExchange> outputExchanges = new ConcurrentLinkedQueue<>();

    private CountDownLatch receiverLatch;

    StreamPipe(int receivers) {
        initialCountDownLatch(receivers);
    }

    void initialCountDownLatch(int receivers) {
        this.receiverLatch = new CountDownLatch(receivers);
    }

    synchronized void process(HttpExchange senderExchange, int bufferSize) {
        List<String> headerKey = List.of("content-type", "content-disposition"); // more header to pass by?
        Headers headers = senderExchange.getRequestHeaders();
        List<String> keyList = headers.keySet().stream().filter(k -> headerKey.contains(k.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        outputExchanges.forEach(exchange -> {
            keyList.forEach(
                    key -> exchange.getResponseHeaders().set(key, headers.getFirst(key)));
            try {
                exchange.sendResponseHeaders(200, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try (InputStream inputStream = senderExchange.getRequestBody()) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                int finalBytesRead = bytesRead;
                outputs.forEach(pos -> {
                    try {
                        pos.write(buffer, 0, finalBytesRead);
                    } catch (IOException e) {
                        logger.warning("fail in send message: " + e.getMessage());
                    }
                });
            }
            outputs.forEach(output -> {
                try {
                    output.flush();
                    output.close();
                } catch (IOException e) {
                    logger.warning("fail in flush/close: " + e.getMessage());
                }
            });
        } catch (IOException e) {
            logger.warning("Input stream error: " + e.getMessage());
        }
    }

    void receiverReady() {
        receiverLatch.countDown();
    }

    void awaitReceivers() throws InterruptedException {
        receiverLatch.await();
    }

    boolean isSenderUp() {
        return null != receiverLatch;
    }
}
