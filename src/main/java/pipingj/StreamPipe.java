package pipingj;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import static pipingj.HandlerUtil.BUFFER_SIZE;

@Slf4j
@NoArgsConstructor
class StreamPipe {

    final Queue<PipedOutputStream> outputs = new ConcurrentLinkedQueue<>();
    final Queue<HttpServletResponse> responses = new ConcurrentLinkedQueue<>();

    private CountDownLatch receiverLatch;

    StreamPipe(int receivers) {
        initialCountDownLatch(receivers);
    }

    void initialCountDownLatch(int receivers) {
        this.receiverLatch = new CountDownLatch(receivers);
    }

    void process(HttpServletRequest request) {
        responses.forEach(response -> response.setContentType(request.getContentType()));
        try (InputStream inputStream = request.getInputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                int finalBytesRead = bytesRead;
                outputs.forEach(pos -> {
                    try {
                        pos.write(buffer, 0, finalBytesRead);
                    } catch (IOException e) {
                        log.warn("fail in send message", e);
                    }
                });
            }
            outputs.forEach(output -> {
                try {
                    output.flush();
                    output.close();
                } catch (IOException e) {
                    log.warn("fail in flush/close", e);
                }
            });
        } catch (IOException e) {
            log.warn("Input stream error: ", e);
        }
    }
// this is expected way of handle multipart. but for some reason, it does not work
//    @SneakyThrows
//    void processMultipart(Request request, int bufferSize) {
//        String boundary = "Boundary-" + System.currentTimeMillis();
//        responses.forEach(response -> response.setContentType("multipart/form-data; boundary=" + boundary));
//        // Handle the multi-part request
//        for (Part part : request.getParts()) {
//            outputs.forEach(outputStream -> {
//                StringBuilder sb = new StringBuilder("--" + boundary);
//                sb.append("\nContent-Disposition: form-data; name=\"").append(part.getName()).append("\"; filename=\"")
//                        .append(part.getSubmittedFileName()).append("\"");
//
//                String contentType = part.getContentType();
//                if (contentType != null && !contentType.isEmpty()) {
//                    sb.append("\nContent-Type: " + contentType);
//                }
//                try {
//                    outputStream.write(sb.append("\n\n").toString().getBytes());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//            // Send the bytes directly to the output stream of the response
//            byte[] buffer = new byte[bufferSize];
//            int bytesRead;
//            try (InputStream inputStream = part.getInputStream()) {
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    int finalBytesRead = bytesRead;
//                    outputs.forEach(pos -> {
//                        try {
//                            pos.write(buffer, 0, finalBytesRead);
//                        } catch (IOException e) {
//                            logger.warning("fail in send message: " + e.getMessage());
//                        }
//                    });
//                }
//            }
//            outputs.forEach(output -> {
//                try {
//                    output.write("\n".getBytes());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//        }
//        outputs.forEach(output -> {
//            try {
//                output.write(("--" + boundary + "--\n").getBytes());
//                output.flush();
//                output.close();
//            } catch (IOException e) {
//                logger.warning("fail in flush/close: " + e.getMessage());
//            }
//        });
//    }

    @SneakyThrows
    void processMultipart(Request request) { // only one file for now
        if (request.getParts() == null || request.getParts().isEmpty()) {
            // don't know how to handle it
            log.warn("don't know how to handle it {}", request.getParts());
            return;
        }
        Part part = request.getParts().iterator().next();
        responses.forEach(response -> {
            String contentType = part.getContentType();
            if (contentType != null && !contentType.isEmpty()) {
                response.setContentType(contentType);
            }
            response.setHeader("Content-Disposition", "form-data; name=\"" + part.getName() + "\"; filename=\"" +
                    part.getSubmittedFileName() + "\"");

        });
        // Send the bytes directly to the output stream of the response
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        try (InputStream inputStream = part.getInputStream()) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                int finalBytesRead = bytesRead;
                outputs.forEach(pos -> {
                    try {
                        pos.write(buffer, 0, finalBytesRead);
                    } catch (IOException e) {
                        log.warn("fail in send message: " + e.getMessage());
                    }
                });
            }
        }
        outputs.forEach(output -> {
            try {
                output.flush();
                output.close();
            } catch (IOException e) {
                log.warn("fail in flush/close: " + e.getMessage());
            }
        });
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
