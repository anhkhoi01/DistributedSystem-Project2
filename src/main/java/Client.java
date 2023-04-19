import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import service.ChatServiceGrpc;
import service.ChatServiceOuterClass;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Client {
    String client_id;
    public static final Logger logger = Logger.getLogger(Client.class.getName());
    private final ManagedChannel chanel;
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;

    public Client(String host, int port,String client_id) {
        chanel = ManagedChannelBuilder.forAddress(host,port).usePlaintext().build();
        blockingStub = ChatServiceGrpc.newBlockingStub(chanel);
        asyncStub = ChatServiceGrpc.newStub(chanel);
        this.client_id=client_id;
    }

    public void shutdown() throws InterruptedException {
        chanel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void sendLike(int message_id) {
        ChatServiceOuterClass.LikeRequest request = ChatServiceOuterClass.LikeRequest.newBuilder()
                .setSender(client_id)
                .setMessageNumber(message_id)
                .build();
        ChatServiceOuterClass.ServerMessageResponse response;
        try {
            response = blockingStub.sendLike(request);
        } catch (Exception e) {
            return;
        }
        logger.info(response.getMessage());
    }

    public void sendChat(Scanner scanner) throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<ChatServiceOuterClass.ChatMessageRequest> requestStreamObserver = asyncStub
                .sendChat(new StreamObserver<>() {
                    @Override
                    public void onNext(ChatServiceOuterClass.ServerMessageResponse value) {
                        System.out.println(value.getHeader() + value.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.info(t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Server complete response.");
                        finishLatch.countDown();
                    }
                });

        // Call gRPC method with responseObserver
        ChatServiceOuterClass.ChatMessageRequest request = ChatServiceOuterClass.ChatMessageRequest.newBuilder()
                .setClientId(client_id)
                .setContent("")
                .build();
        requestStreamObserver.onNext(request);

        System.out.println("Chat something:");
        while (true) {
            String mess = scanner.nextLine().trim();
            if (mess.contains("LIKE")) {
                String[] arr = mess.split(" ");
                try {
                    sendLike(Integer.parseInt(arr[1]));
                } catch (NumberFormatException e) {
                    logger.info("Message ID wrong!");
                }
            } else if (!mess.equals("LEAVE")) {
                request = ChatServiceOuterClass.ChatMessageRequest.newBuilder()
                        .setClientId(client_id)
                        .setContent(mess)
                        .build();
                requestStreamObserver.onNext(request);
            } else {
                logger.info("break");
                requestStreamObserver.onCompleted();
                break;
            }
        }

        if (!finishLatch.await(5,TimeUnit.SECONDS)) {
            logger.info("request can't finish within 5 SECONDS.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        String client_id;

        if (args.length > 0) {
            client_id = args[0];
        } else {
            System.out.print("Input your name: ");
            client_id = scanner.nextLine().trim();
        }

        Client client = new Client("0.0.0.0",1234,client_id);

        try {
            client.sendChat(scanner);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
            client.shutdown();
        }

    }
}
