package chatbox.server;

import chatbox.model.Message;
import chatbox.security.AESEncryption;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private String username;
    private String currentRoom;

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            // Nhan JOIN dau tien
            Message joinMsg = (Message) in.readObject();
            this.username = joinMsg.getSender();

            // Xac dinh admin: client ket noi tu bat ky IP nao cua chinh may server
            String clientIP = socket.getInetAddress().getHostAddress();
            boolean isAdmin = isLocalAddress(clientIP);
            System.out.println("[+] " + username + " (" + clientIP + ")"
                    + (isAdmin ? " [ADMIN]" : ""));

            // Gui trang thai admin cho client
            ChatServer.sendTo(this, new Message("System",
                    isAdmin ? "true" : "false", Message.Type.ADMIN_INFO));

            // Gui danh sach phong
            ChatServer.sendTo(this, new Message("System",
                    String.join(",", ChatServer.getRoomNames()),
                    Message.Type.ROOM_LIST));

            // Chao mung
            ChatServer.sendTo(this, new Message("System",
                    "Chao mung " + username + " den ChatBox!", Message.Type.SYSTEM));

            // Mac dinh vao phong chung
            ChatServer.joinRoom(this, "chung");

            // Vong lap chinh
            while (true) {
                Message msg = (Message) in.readObject();
                if (msg == null) break;

                switch (msg.getType()) {
                    case SWITCH_ROOM:
                        ChatServer.joinRoom(this, msg.getContent());
                        ChatServer.sendTo(this, new Message("System",
                                "Da vao phong: " + msg.getContent(),
                                Message.Type.SWITCH_ROOM, msg.getContent()));
                        break;

                    case CREATE_ROOM:
                        // Chi admin moi duoc tao phong
                        if (!isAdmin) {
                            ChatServer.sendTo(this, new Message("System",
                                    "Ban khong co quyen tao phong!", Message.Type.SYSTEM));
                            break;
                        }
                        boolean ok = ChatServer.createRoom(msg.getContent());
                        if (ok) {
                            ChatServer.joinRoom(this, msg.getContent());
                            ChatServer.sendTo(this, new Message("System",
                                    "Da tao phong: " + msg.getContent(),
                                    Message.Type.SWITCH_ROOM, msg.getContent()));
                        } else {
                            ChatServer.sendTo(this, new Message("System",
                                    "Phong '" + msg.getContent() + "' da ton tai!",
                                    Message.Type.SYSTEM));
                        }
                        break;

                    case DELETE_ROOM:
                        // Chi admin moi duoc xoa phong
                        if (!isAdmin) {
                            ChatServer.sendTo(this, new Message("System",
                                    "Ban khong co quyen xoa phong!", Message.Type.SYSTEM));
                            break;
                        }
                        String toDelete = msg.getContent();
                        if (!toDelete.equals("chung")) {
                            ChatServer.deleteRoom(toDelete);
                        }
                        break;

                    case TEXT:
                        if (msg.isEncrypted()) {
                            msg.setContent(AESEncryption.decrypt(msg.getContent()));
                            msg.setEncrypted(false);
                        }
                        String room = msg.getRoom();
                        System.out.println("[MSG][" + room + "] " + msg.getSender()
                                + ": " + msg.getContent());
                        ChatServer.broadcastToRoom(msg, room);
                        break;

                    default: break;
                }
            }
        } catch (EOFException | SocketException e) {
        } catch (Exception e) {
            System.err.println("[Handler] " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public synchronized void sendMessage(Message msg) {
        try {
            if (out != null && !socket.isClosed()) {
                out.writeObject(msg);
                out.flush();
                out.reset();
            }
        } catch (IOException ignored) {}
    }

    private void disconnect() {
        ChatServer.removeClient(this);
        try { if (!socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        System.out.println("[-] " + username + " ngat ket noi");
    }

    public String getUsername()    { return username; }
    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String r) { this.currentRoom = r; }

    /**
     * Kiem tra xem clientIP co phai la IP cua chinh may dang chay server khong.
     * Bao gom: localhost (127.0.0.1), IPv6 loopback, va tat ca IP thuc cua may.
     */
    private static boolean isLocalAddress(String clientIP) {
        // Localhost va IPv6 loopback
        if (clientIP.equals("127.0.0.1") || clientIP.equals("0:0:0:0:0:0:0:1")
                || clientIP.equals("::1")) return true;
        try {
            // Lay tat ca NetworkInterface cua may
            java.util.Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                java.util.Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().equals(clientIP)) return true;
                }
            }
        } catch (Exception e) {
            System.err.println("[Admin check] " + e.getMessage());
        }
        return false;
    }
}
