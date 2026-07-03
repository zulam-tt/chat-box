package chatbox.client;

import chatbox.model.Message;
import chatbox.security.AESEncryption;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class NetworkClient {

    private final String  host;
    private final int     port;
    private final String  username;
    private final boolean useEncryption;

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private String             currentRoom = "chung";

    private Consumer<Message> onMessageReceived;
    private Runnable          onDisconnected;
    private volatile boolean  connected = false;

    public NetworkClient(String host, int port, String username, boolean useEncryption) {
        this.host = host; this.port = port;
        this.username = username; this.useEncryption = useEncryption;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        socket.setKeepAlive(true);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
        connected = true;

        sendRaw(new Message(username, "joined", Message.Type.JOIN));

        Thread t = new Thread(this::receiveLoop, "Recv-" + username);
        t.setDaemon(true);
        t.start();
    }

    private void receiveLoop() {
        try {
            while (connected) {
                Message msg = (Message) in.readObject();
                if (msg != null && onMessageReceived != null)
                    onMessageReceived.accept(msg);
            }
        } catch (EOFException | SocketException e) {
        } catch (Exception e) {
            if (connected) System.err.println("[Client] " + e.getMessage());
        } finally {
            connected = false;
            if (onDisconnected != null) onDisconnected.run();
        }
    }

    public void sendMessage(String text) {
        if (!connected) return;
        String content = useEncryption ? AESEncryption.encrypt(text) : text;
        Message msg = new Message(username, content, Message.Type.TEXT, currentRoom);
        msg.setEncrypted(useEncryption);
        sendRaw(msg);
    }

    public void switchRoom(String roomName) {
        if (!connected) return;
        currentRoom = roomName;
        sendRaw(new Message(username, roomName, Message.Type.SWITCH_ROOM, roomName));
    }

    public void createRoom(String roomName) {
        if (!connected) return;
        sendRaw(new Message(username, roomName, Message.Type.CREATE_ROOM, roomName));
    }

    public void deleteRoom(String roomName) {
        if (!connected) return;
        sendRaw(new Message(username, roomName, Message.Type.DELETE_ROOM, roomName));
    }

    private synchronized void sendRaw(Message msg) {
        try {
            out.writeObject(msg); out.flush(); out.reset();
        } catch (IOException e) {
            System.err.println("[Client] Send: " + e.getMessage());
        }
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected()  { return connected; }
    public String  getUsername()  { return username; }
    public String  getCurrentRoom() { return currentRoom; }
    public void    setCurrentRoom(String r) { currentRoom = r; }

    public void setOnMessageReceived(Consumer<Message> cb) { onMessageReceived = cb; }
    public void setOnDisconnected(Runnable cb)             { onDisconnected = cb; }
}
