import io.grpc.stub.StreamObserver;
import service.ChatServiceGrpc;
import service.ChatServiceOuterClass;

import java.util.*;
import java.util.logging.Logger;

class Message {
    String client_id;
    String content;
    int count_like;
    ArrayList<String> whosLiked;
    int id;


    public Message(String client_id, String content, int id) {
        this.client_id = client_id;
        this.content = content;
        this.count_like = 0;
        whosLiked = new ArrayList<>();
        this.id = id;
    }
}

public class ChatService extends ChatServiceGrpc.ChatServiceImplBase {
    private static final ArrayList<Message> messages = new ArrayList<>();
    private static final HashMap<String, Boolean> clients = new HashMap<>();
    private static final HashMap<String, Integer> notLikedMessage = new HashMap<>();
    private static final Logger logger = Logger.getLogger(ChatService.class.getName());

    private static final Set<StreamObserver<ChatServiceOuterClass.ServerMessageResponse>> clientsStubs = new HashSet<>();


    @Override
    public synchronized StreamObserver<ChatServiceOuterClass.ChatMessageRequest> sendChat(StreamObserver<ChatServiceOuterClass.ServerMessageResponse> responseObserver) {
        return new StreamObserver<>() {
            WriteLog logFile = new WriteLog();
            @Override
            public void onNext(ChatServiceOuterClass.ChatMessageRequest value) {
                String sender = value.getClientId();
                String content = value.getContent();

                logger.info("Receive a message from " + sender + ": " + content);
                logFile.writeLogAppend("Receive a message from " + sender + ": " + content);

                int recentMessage = messages.size() + 1;
                ChatServiceOuterClass.ServerMessageResponse response = ChatServiceOuterClass.ServerMessageResponse.newBuilder()
                        .setHeader(sender + " (" + recentMessage + "): ")
                        .setMessage(content)
                        .build();

                if (clients.containsKey(sender)) {
                    if (clients.get(sender)) {
                        for (StreamObserver<ChatServiceOuterClass.ServerMessageResponse> client : clientsStubs) {
                            if (client.equals(responseObserver)) {
                                continue;
                            }
                            client.onNext(response);
                        }

                        clients.put(sender, false);
                        notLikedMessage.put(sender, recentMessage);
                        messages.add(new Message(sender, content, recentMessage));
                        logger.info("Broadcast message complete !");
                        logFile.writeLogAppend("Broadcast message complete !");
                    } else {
                        response = ChatServiceOuterClass.ServerMessageResponse.newBuilder()
                                .setHeader("")
                                .setMessage("Your previous message must be liked by 2 member!")
                                .build();
                        responseObserver.onNext(response);
                        logFile.writeLogAppend("Message isn't broadcast because sender's previous message isn't liked enough!");
                    }
                } else {
                    response = ChatServiceOuterClass.ServerMessageResponse.newBuilder()
                            .setHeader("")
                            .setMessage(sender + " join chat!")
                            .build();
                    for (StreamObserver<ChatServiceOuterClass.ServerMessageResponse> client : clientsStubs) {
                        client.onNext(response);
                    }

                    clients.put(sender, true);
                    clientsStubs.add(responseObserver);
                    logger.info("New client!");
                    logFile.writeLogAppend(sender + " join chat!");
                }
            }

            @Override
            public void onError(Throwable t) {
                clients.remove(responseObserver);
                logger.info(t.getMessage());
                logFile.writeLogAppend(t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
                clients.remove(responseObserver);
                logger.info("Close a stub!");
                logFile.writeLogAppend("A member has left chat!");
            }
        };
    }

    @Override
    public void sendLike(ChatServiceOuterClass.LikeRequest request, StreamObserver<ChatServiceOuterClass.ServerMessageResponse> responseObserver) {
        String sender = request.getSender();
        int message_id = request.getMessageNumber();
        Boolean likeAccepted = false;
        WriteLog logFile = new WriteLog();

        logFile.writeLogAppend("Received a Like Request");
        if (message_id > messages.size() || message_id < 1) {
            ChatServiceOuterClass.ServerMessageResponse response = ChatServiceOuterClass.ServerMessageResponse.newBuilder()
                    .setMessage("Message ID not found !")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            logFile.writeLogAppend("Message ID not found !");
            return;
        }

        Message message = messages.get(message_id - 1);
        if (message_id == message.id) {
            if (!(message.client_id.equals(sender))) {
                if (message.whosLiked.contains(sender)) {
                    ChatServiceOuterClass.ServerMessageResponse response = ChatServiceOuterClass.ServerMessageResponse.newBuilder()
                            .setMessage("You have already liked this message!")
                            .build();
                    responseObserver.onNext(response);
                    logger.info("User can't like a message 2 times!");
                    logFile.writeLogAppend("Sender had liked this message!");
                } else {
                    message.whosLiked.add(sender);
                    ChatServiceOuterClass.ServerMessageResponse response = ChatServiceOuterClass.ServerMessageResponse.newBuilder()
                            .setMessage(sender + " like message id: " + message_id)
                            .build();
                    for (StreamObserver<ChatServiceOuterClass.ServerMessageResponse> client : clientsStubs) {
                        client.onNext(response);
                    }
                    likeAccepted = true;
                }
            } else {
                ChatServiceOuterClass.ServerMessageResponse response = ChatServiceOuterClass.ServerMessageResponse.newBuilder()
                        .setMessage("You can't like message yourself!")
                        .build();
                responseObserver.onNext(response);
                logger.info("User can't like message himself!");
                logFile.writeLogAppend("User can't like message himself!");
            }
        }

        if (notLikedMessage.get(message.client_id) == message_id && likeAccepted) {
            if (message.whosLiked.size() == 2) {
                clients.put(message.client_id, true);
                notLikedMessage.remove(message.client_id);
            }
        }
        responseObserver.onCompleted();
    }

}
