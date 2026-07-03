# 💬 ChatBox Java - Đồ Án Lập Trình Mạng

## Tính năng tích hợp từ Syllabus

| Topic | Tính năng |
|-------|-----------|
| Topic 3 - IO Stream | `ObjectInputStream` / `ObjectOutputStream` để truyền Message objects qua mạng |
| Topic 4 - Networking | TCP Socket (ServerSocket + Socket), Client-Server architecture |
| Multithreading | Mỗi client chạy trên 1 Thread riêng, sử dụng `ExecutorService` (Thread Pool) |
| Topic 5 - XML with Java | Lưu lịch sử chat ra `chat_history.xml` bằng DOM Parser |
| Topic 7 - Java Security | Mã hóa tin nhắn AES-128 bằng `javax.crypto` |

---

## Cấu trúc Project

```
ChatBoxApp/
└── src/
    └── chatbox/
        ├── model/
        │   └── Message.java          ← Serializable message object
        ├── security/
        │   └── AESEncryption.java    ← Topic 7: Mã hóa AES-128
        ├── xml/
        │   └── ChatHistoryXML.java   ← Topic 5: DOM XML Parser
        ├── server/
        │   ├── ChatServer.java       ← Main server (ServerSocket)
        │   └── ClientHandler.java    ← 1 thread / client
        ├── client/
        │   └── NetworkClient.java    ← TCP client + receive thread
        └── ui/
            └── ChatGUI.java          ← Giao diện Swing dark theme
```

---

## Cách cài trên IntelliJ IDEA

### Bước 1: Tạo Project
1. `File → New → Project`
2. Chọn **Java** (không cần Maven/Gradle)
3. Đặt tên: `ChatBoxApp`
4. SDK: Java 8 hoặc cao hơn

### Bước 2: Thêm Source Files
1. Chuột phải vào `src` → `New → Package` → tạo các package:
   - `chatbox.model`
   - `chatbox.security`
   - `chatbox.xml`
   - `chatbox.server`
   - `chatbox.client`
   - `chatbox.ui`
2. Copy từng file `.java` vào đúng package

### Bước 3: Tạo 2 Run Configurations

**Run Config 1 - Server:**
- `Run → Edit Configurations → + → Application`
- Name: `ChatServer`
- Main class: `chatbox.server.ChatServer`

**Run Config 2 - Client:**
- `Run → Edit Configurations → + → Application`
- Name: `ChatClient`
- Main class: `chatbox.ui.ChatGUI`
- ✅ Tích **Allow multiple instances** (để mở nhiều client)

### Bước 4: Chạy
1. Chạy **ChatServer** trước
2. Chạy **ChatClient** (có thể mở nhiều cửa sổ)
3. Nhập tên → chọn localhost → Kết nối

---

## Luồng hoạt động

```
Client A ──┐                    ┌── Client B
           │  TCP Socket        │
           ▼                    ▼
      [ChatServer : 9999]
           │
           ├── ClientHandler (Thread-1) ── Client A
           ├── ClientHandler (Thread-2) ── Client B
           └── ClientHandler (Thread-N) ── Client N
                     │
                     ├── ObjectStreams (IO)
                     ├── AES Decrypt/Encrypt (Security)
                     ├── Broadcast to all
                     └── Save to XML (History)
```

---

## Giải thích kỹ thuật

### Multithreading
```java
// Server dùng ExecutorService (Thread Pool)
ExecutorService threadPool = Executors.newCachedThreadPool();

// Mỗi client submit 1 Runnable vào pool
threadPool.submit(new ClientHandler(clientSocket));

// Client nhận tin nhắn trên background thread (daemon)
Thread receiveThread = new Thread(this::receiveLoop);
receiveThread.setDaemon(true);
receiveThread.start();
```

### IO Stream
```java
// Gửi/nhận Java objects qua mạng
ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream());

out.writeObject(message);           // Gửi Message object
Message msg = (Message) in.readObject(); // Nhận Message object
```

### Networking
```java
// Server lắng nghe
ServerSocket serverSocket = new ServerSocket(9999);
Socket clientSocket = serverSocket.accept(); // Block đến khi có client

// Client kết nối
Socket socket = new Socket("localhost", 9999);
```

### AES Encryption
```java
// Mã hóa
String encrypted = AESEncryption.encrypt("Hello World");
// → "U2FsdGVkX1..."

// Giải mã
String plain = AESEncryption.decrypt(encrypted);
// → "Hello World"
```

### XML History
```java
// Lưu message vào XML
ChatHistoryXML.saveMessage(msg);

// Đọc lại
List<String> history = ChatHistoryXML.loadHistory();
```

---

## Lưu ý

- File `chat_history.xml` được tạo ở thư mục chạy project (working directory)
- Key AES được hardcode (`ChatBoxSecureKey`) - trong thực tế nên trao đổi key qua Diffie-Hellman
- Server chạy tại port `9999`, có thể đổi trong `ChatServer.PORT`
