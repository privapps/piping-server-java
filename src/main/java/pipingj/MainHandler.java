package pipingj;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.List;

@MultipartConfig
public class MainHandler extends AbstractHandler {
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement("");

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {
        if (baseRequest.getMethod().equalsIgnoreCase("POST") &&
                request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
            handleMultipart(baseRequest, request, response);
            return;
        }
        String requestURI = baseRequest.getRequestURI();
        for (String uri : List.of("/favicon.ico", "/robots.txt", "/help", "/version", "/noscript")) {
            if (requestURI.equals(uri)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        switch (baseRequest.getMethod().toLowerCase()) {
            case "post":
            case "put":
                HandlerUtil.handleSenderRequest(request, response, pipe -> pipe.process(request));
                break;
            case "get":
                HandlerUtil.handleReceiverRequest(request, response);
                break;
            case "options":
                response.setHeader("Access-Control-Allow-Origin", "*");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        baseRequest.setHandled(true);
    }

    public void handleMultipart(Request baseRequest,
                                HttpServletRequest request,
                                HttpServletResponse response)
            throws ServletException {

        // Enable multi-part form data parsing
        baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
        HandlerUtil.handleSenderRequest(request, response, pipe -> pipe.processMultipart(baseRequest));
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }
}

