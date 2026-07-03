package chatbox.server;

import chatbox.model.Message;
import chatbox.xml.ChatHistoryXML;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    public static final int PORT = 9999;

    private static final List<ClientHandler> allClients =
            Collections.synchronizedList(new ArrayList<>());

    // Map phong -> danh sach client trong phong
    private static final Map<String, List<ClientHandler>> rooms =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    // Luu lich su tin nhan theo tung phong (toi da 100 tin moi phong)
    private static final Map<String, List<Message>> roomHistory =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private static final int MAX_HISTORY = 100;

    static {
        rooms.put("chung",   Collections.synchronizedList(new ArrayList<>()));
        rooms.put("gaming",  Collections.synchronizedList(new ArrayList<>()));
        rooms.put("hoc-tap", Collections.synchronizedList(new ArrayList<>()));
        roomHistory.put("chung",   Collections.synchronizedList(new ArrayList<>()));
        roomHistory.put("gaming",  Collections.synchronizedList(new ArrayList<>()));
        roomHistory.put("hoc-tap", Collections.synchronizedList(new ArrayList<>()));
    }

    public static void main(String[] args) {
        System.out.println("=== ChatBox Multi-Room Server | Port: " + PORT + " ===");
        System.out.println("[Rooms] " + rooms.keySet());

        try (ServerSocket ss = new ServerSocket(PORT)) {
            ss.setReuseAddress(true);
            while (true) {
                Socket socket = ss.accept();
                socket.setKeepAlive(true);
                ClientHandler h = new ClientHandler(socket);
                allClients.add(h);
                pool.submit(h);
            }
        } catch (IOException e) {
            System.err.println("[Server] " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    public static void broadcastToRoom(Message msg, String roomName) {
        // Luu vao lich su phong neu la tin nhan TEXT
        if (msg.getType() == Message.Type.TEXT) {
            ChatHistoryXML.saveMessage(msg);
            List<Message> history = roomHistory.computeIfAbsent(roomName,
                    k -> Collections.synchronizedList(new ArrayList<>()));
            synchronized (history) {
                history.add(msg);
                // Giu toi da MAX_HISTORY tin nhan
                if (history.size() > MAX_HISTORY) history.remove(0);
            }
        }
        List<ClientHandler> rc = rooms.get(roomName);
        if (rc == null) return;
        synchronized (rc) {
            for (ClientHandler c : rc) c.sendMessage(msg);
        }
    }

    public static void broadcastToRoomExcept(Message msg, String roomName, ClientHandler except) {
        List<ClientHandler> rc = rooms.get(roomName);
        if (rc == null) return;
        synchronized (rc) {
            for (ClientHandler c : rc) {
                if (c != except) c.sendMessage(msg);
            }
        }
    }

    public static void sendTo(ClientHandler target, Message msg) {
        target.sendMessage(msg);
    }

    public static void joinRoom(ClientHandler client, String roomName) {
        // Rời phòng cũ
        String oldRoom = client.getCurrentRoom();
        if (oldRoom != null) {
            List<ClientHandler> old = rooms.get(oldRoom);
            if (old != null) old.remove(client);
            broadcastToRoom(new Message(client.getUsername(), "", Message.Type.LEAVE, oldRoom), oldRoom);
        }

        rooms.putIfAbsent(roomName, Collections.synchronizedList(new ArrayList<>()));
        roomHistory.putIfAbsent(roomName, Collections.synchronizedList(new ArrayList<>()));
        rooms.get(roomName).add(client);
        client.setCurrentRoom(roomName);
        System.out.println("[Room] " + client.getUsername() + " -> " + roomName);

        // Gui lich su tin nhan cua phong cho client moi vao
        List<Message> history = roomHistory.get(roomName);
        if (history != null && !history.isEmpty()) {
            synchronized (history) {
                for (Message histMsg : history) {
                    sendTo(client, histMsg);
                }
            }
        }

        // Gửi danh sách người trong phòng cho client mới
        List<ClientHandler> members = rooms.get(roomName);
        synchronized (members) {
            for (ClientHandler m : members) {
                if (m != client && m.getUsername() != null) {
                    sendTo(client, new Message(m.getUsername(), "", Message.Type.JOIN, roomName));
                }
            }
        }

        // Thông báo cho phòng mới — sender = tên người vào
        broadcastToRoomExcept(new Message(client.getUsername(), "", Message.Type.JOIN, roomName), roomName, client);
    }

    public static synchronized boolean createRoom(String roomName) {
        if (rooms.containsKey(roomName)) return false;
        rooms.put(roomName, Collections.synchronizedList(new ArrayList<>()));
        roomHistory.put(roomName, Collections.synchronizedList(new ArrayList<>()));
        System.out.println("[Room] Tao phong moi: " + roomName);
        broadcastRoomList();
        return true;
    }

    public static synchronized void deleteRoom(String roomName) {
        if (roomName.equals("chung")) return;
        List<ClientHandler> members = rooms.remove(roomName);
        roomHistory.remove(roomName); // xoa lich su phong
        System.out.println("[Room] Xoa phong: " + roomName);
        if (members != null) {
            synchronized (members) {
                for (ClientHandler c : new ArrayList<>(members)) {
                    joinRoom(c, "chung");
                    sendTo(c, new Message("System",
                            "Phong " + roomName + " da bi xoa. Ban da duoc chuyen ve phong chung.",
                            Message.Type.SYSTEM));
                }
            }
        }
        broadcastRoomList();
    }

    public static void broadcastRoomList() {
        String list = String.join(",", rooms.keySet());
        Message msg = new Message("System", list, Message.Type.ROOM_LIST);
        synchronized (allClients) {
            for (ClientHandler c : allClients) c.sendMessage(msg);
        }
    }

    public static void removeClient(ClientHandler h) {
        allClients.remove(h);
        String room = h.getCurrentRoom();
        if (room != null) {
            List<ClientHandler> rc = rooms.get(room);
            if (rc != null) rc.remove(h);
            if (h.getUsername() != null) {
                broadcastToRoom(new Message(h.getUsername(), "", Message.Type.LEAVE, room), room);
            }
        }
    }

    public static List<String> getRoomNames() { return new ArrayList<>(rooms.keySet()); }
    public static int getOnlineCount()        { return allClients.size(); }
}
