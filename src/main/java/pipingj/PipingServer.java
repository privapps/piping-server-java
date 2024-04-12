package pipingj;

import org.eclipse.jetty.server.Server;

public class PipingServer {
    public static void main(String[] args) throws Exception {
        int port = getIntFromEnv("PORT", 8088);
        Server server = new Server(port);
        server.setHandler(new MainHandler());

        server.start();
        server.join();
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
