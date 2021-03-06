package level_30.task3008;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String,Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args){
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())){
            ConsoleHelper.writeMessage("Server started");
            while (true){
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendBroadcastMessage(Message message){
        try {
            for(Map.Entry<String,Connection> x: connectionMap.entrySet()){
                x.getValue().send(message);
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Message doesn't send");
        }
    }

    private static class Handler extends Thread{
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException{
            connection.send(new Message(MessageType.NAME_REQUEST));
            while (true) {
                Message userName = connection.receive();
                if (userName.getType() == MessageType.USER_NAME) {
                    if (!userName.getData().isEmpty()) {
                        if(!connectionMap.containsKey(userName.getData())) {
                            connectionMap.put(userName.getData(), connection);
                            connection.send(new Message(MessageType.NAME_ACCEPTED));
                            return userName.getData();
                        }
                        else connection.send(new Message(MessageType.NAME_REQUEST));
                    }
                    else connection.send(new Message(MessageType.NAME_REQUEST));
                }
                else connection.send(new Message(MessageType.NAME_REQUEST));
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException{
            for(Map.Entry<String,Connection> x: connectionMap.entrySet()){
                if(!x.getKey().equals(userName)){
                    connection.send(new Message(MessageType.USER_ADDED,x.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName)throws IOException,ClassNotFoundException{
            while (true) {
                Message message = connection.receive();
                if(message.getType()==(MessageType.TEXT)){
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                }
                else ConsoleHelper.writeMessage("ERROR! It's not a text. Try again.");
            }
        }

        public void run(){
            String userName="";
            try {
                ConsoleHelper.writeMessage("Connection with "+ socket.getRemoteSocketAddress()+" accepted");
                Connection connection = new Connection(socket);
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED,userName));
                notifyUsers(connection,userName);
                serverMainLoop(connection,userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("ERROR of exchange data with remote client");
            } finally {
                try {
                    if(!userName.equals("")) {
                        connectionMap.remove(userName);
                        sendBroadcastMessage(new Message(MessageType.USER_REMOVED,userName));
                    }
                    socket.close();
                    ConsoleHelper.writeMessage("Socket is closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
