import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private final int port;
    private final io.grpc.Server server;

    public Server(int port) {
        this(ServerBuilder.forPort(port),port);
    }

    public Server(ServerBuilder serverBuilder, int port) {
        this.port = port;
        ChatService chatService = new ChatService();
        server = serverBuilder.addService(chatService).build();
    }

    public void start() throws IOException {
        server.start();
        logger.info("Server start at port: " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shut down grpc server because JVM is stop!");
            try {
                Server.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("Server shut down!");
        }));
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blocUntilShutdown() throws InterruptedException {
        if (server!=null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = new Server(1234);
        WriteLog logFile = new WriteLog();
        logFile.writeNewLog("");

        try {
            server.start();
            server.blocUntilShutdown();
        } finally {
            server.stop();
            logFile.writeLogAppend("Server is closed!");
        }
    }
}
