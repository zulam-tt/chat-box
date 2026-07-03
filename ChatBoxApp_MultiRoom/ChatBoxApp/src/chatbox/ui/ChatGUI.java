package chatbox.ui;

import chatbox.client.NetworkClient;
import chatbox.model.Message;
import chatbox.xml.ChatHistoryXML;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Arrays;
import java.util.List;

public class ChatGUI extends JFrame {

    // Colors
    private static final Color C_BG        = new Color(13,  15,  23);
    private static final Color C_PANEL     = new Color(24,  27,  40);
    private static final Color C_SIDEBAR   = new Color(14,  16,  26);
    private static final Color C_ROOMS     = new Color(10,  12,  20);
    private static final Color C_INPUT     = new Color(30,  34,  50);
    private static final Color C_ACCENT    = new Color(99,  102, 241);
    private static final Color C_ACCENT2   = new Color(139, 148, 255);
    private static final Color C_BSELF     = new Color(79,  70,  229);
    private static final Color C_BOTHER    = new Color(36,  40,  58);
    private static final Color C_TEXT      = new Color(220, 222, 255);
    private static final Color C_MUTED     = new Color(100, 108, 145);
    private static final Color C_GREEN     = new Color(52,  211, 153);
    private static final Color C_RED       = new Color(239,  68,  68);
    private static final Color C_BORDER    = new Color(35,  39,  58);
    private static final Color C_SELECTED  = new Color(40,  44,  68);
    private static final Color C_HOVER     = new Color(30,  34,  52);
    private static final Color C_YELLOW    = new Color(251, 191,  36);

    // Emojis
    private static final String[][] EMOJI_DATA = {
        {"😀","1f600"},{"😂","1f602"},{"😍","1f60d"},{"🥰","1f970"},
        {"😎","1f60e"},{"🤩","1f929"},{"😢","1f622"},{"😡","1f621"},
        {"🤔","1f914"},{"😴","1f634"},{"😅","1f605"},{"🤣","1f923"},
        {"😊","1f60a"},{"😇","1f607"},{"😏","1f60f"},{"😒","1f612"},
        {"😳","1f633"},{"😱","1f631"},{"🥳","1f973"},{"😆","1f606"},
        {"👍","1f44d"},{"👎","1f44e"},{"❤","2764"}, {"🔥","1f525"},
        {"🎉","1f389"},{"👋","1f44b"},{"💪","1f4aa"},{"🙏","1f64f"},
        {"😘","1f618"},{"🤝","1f91d"}
    };
    private static final Map<String, ImageIcon> emojiCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Components
    private JPanel      chatArea;
    private JScrollPane chatScroll;
    private JTextField  inputField;
    private JButton     sendButton;
    private JLabel      statusLabel;
    private JLabel      connDot;
    private JLabel      roomTitle;
    private JPanel      userListPanel;
    private JPanel      roomListPanel;
    private JCheckBox   encryptBox;

    // State
    private NetworkClient client;
    private String        myName;
    private boolean       connected   = false;
    private boolean       isAdmin     = false; // chi localhost moi la admin
    private String        currentRoom = "chung";
    private JButton       addRoomBtn;           // nút + tạo phòng
    private final Map<String, JPanel>       roomButtons  = new LinkedHashMap<>();
    private final Map<String, List<Object>> roomMessages = new HashMap<>();
    private final Set<String>               onlineUsers  = new HashSet<>();

    public ChatGUI() {
        super("ChatBox");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 700);
        setMinimumSize(new Dimension(860, 540));
        setLocationRelativeTo(null);
        buildUI();
        showLogin();
    }

    // ── BUILD UI ──────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.add(buildRoomsSidebar(), BorderLayout.WEST);
        root.add(buildMain(),         BorderLayout.CENTER);
        setContentPane(root);
    }

    // ── SIDEBAR TRÁI: danh sách phòng kiểu Discord ────────────────

    private JPanel buildRoomsSidebar() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(C_ROOMS);
        outer.setPreferredSize(new Dimension(220, 0));
        outer.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, C_BORDER));

        // Header
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(new Color(8, 10, 18));
        hdr.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(14, 14, 14, 14)));
        JLabel title = new JLabel("ChatBox");
        title.setForeground(C_TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        hdr.add(title, BorderLayout.WEST);

        // Section label KENH
        JPanel sectionLabel = new JPanel(new BorderLayout());
        sectionLabel.setBackground(C_ROOMS);
        sectionLabel.setBorder(new EmptyBorder(14, 14, 4, 8));

        JLabel kenh = new JLabel("KENH VAN BAN");
        kenh.setForeground(C_MUTED);
        kenh.setFont(kenh.getFont().deriveFont(Font.BOLD, 10f));

        // Nút + tạo phòng mới — ẩn mặc định, chỉ hiện nếu là admin
        addRoomBtn = new JButton("+");
        addRoomBtn.setFont(addRoomBtn.getFont().deriveFont(Font.BOLD, 14f));
        addRoomBtn.setForeground(C_MUTED);
        addRoomBtn.setBackground(C_ROOMS);
        addRoomBtn.setFocusPainted(false);
        addRoomBtn.setBorderPainted(false);
        addRoomBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addRoomBtn.setToolTipText("Tao phong moi (Chi admin)");
        addRoomBtn.setVisible(false); // ẩn mặc định cho đến khi biết là admin
        addRoomBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { addRoomBtn.setForeground(C_TEXT); }
            public void mouseExited(MouseEvent e)  { addRoomBtn.setForeground(C_MUTED); }
        });
        addRoomBtn.addActionListener(e -> showCreateRoomDialog());
        sectionLabel.add(kenh,      BorderLayout.WEST);
        sectionLabel.add(addRoomBtn, BorderLayout.EAST);

        // Danh sach phong
        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setBackground(C_ROOMS);

        JScrollPane roomScroll = new JScrollPane(roomListPanel);
        roomScroll.setBorder(null);
        roomScroll.getViewport().setBackground(C_ROOMS);
        roomScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Online users phía dưới
        JPanel onlineSection = buildOnlineSection();

        // Bottom: encrypt + XML
        encryptBox = new JCheckBox("Ma hoa AES", true);
        encryptBox.setBackground(C_ROOMS);
        encryptBox.setForeground(C_GREEN);
        encryptBox.setFont(encryptBox.getFont().deriveFont(Font.PLAIN, 12f));
        encryptBox.setFocusPainted(false);

        JButton histBtn  = sideBtn("Lich su XML");
        JButton clearBtn = sideBtn("Xoa lich su");
        histBtn.addActionListener(e -> showHistory());
        clearBtn.addActionListener(e -> {
            ChatHistoryXML.clearHistory();
            JOptionPane.showMessageDialog(this, "Da xoa lich su.");
        });

        JPanel bot = new JPanel(new GridLayout(3, 1, 0, 5));
        bot.setBackground(C_ROOMS);
        bot.setBorder(new EmptyBorder(6, 10, 14, 10));
        bot.add(encryptBox); bot.add(histBtn); bot.add(clearBtn);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(C_ROOMS);
        center.add(sectionLabel,  BorderLayout.NORTH);
        center.add(roomScroll,    BorderLayout.CENTER);
        center.add(onlineSection, BorderLayout.SOUTH);

        outer.add(hdr,    BorderLayout.NORTH);
        outer.add(center, BorderLayout.CENTER);
        outer.add(bot,    BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildOnlineSection() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_ROOMS);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel lbl = new JLabel("  ONLINE");
        lbl.setForeground(C_MUTED);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
        lbl.setBorder(new EmptyBorder(10, 10, 4, 0));

        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(C_ROOMS);
        userListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JScrollPane us = new JScrollPane(userListPanel);
        us.setBorder(null);
        us.getViewport().setBackground(C_ROOMS);
        us.setPreferredSize(new Dimension(0, 100));

        p.add(lbl, BorderLayout.NORTH);
        p.add(us,  BorderLayout.CENTER);
        return p;
    }

    /** Tạo 1 nút phòng kiểu Discord — chuột phải để xóa */
    private JPanel makeRoomRow(String roomName) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(C_ROOMS);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setBorder(new EmptyBorder(2, 8, 2, 8));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setName(roomName);

        JPanel inner = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        inner.setBackground(C_ROOMS);
        inner.setOpaque(true);

        JLabel hash = new JLabel("#");
        hash.setForeground(C_MUTED);
        hash.setFont(hash.getFont().deriveFont(Font.BOLD, 15f));

        JLabel name = new JLabel(roomName);
        name.setForeground(C_MUTED);
        name.setFont(name.getFont().deriveFont(Font.PLAIN, 14f));

        inner.add(hash);
        inner.add(name);
        row.add(inner, BorderLayout.CENTER);

        // Context menu chuột phải — CHỈ admin mới thấy menu xóa
        if (!roomName.equals("chung")) {
            JPopupMenu menu = new JPopupMenu();
            menu.setBackground(C_PANEL);

            JMenuItem deleteItem = new JMenuItem("  Xoa kenh # " + roomName);
            deleteItem.setForeground(C_RED);
            deleteItem.setBackground(C_PANEL);
            deleteItem.setFont(deleteItem.getFont().deriveFont(Font.BOLD, 13f));
            deleteItem.setBorderPainted(false);
            deleteItem.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Xoa kenh # " + roomName + "?", "Xac nhan xoa",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (connected) client.deleteRoom(roomName);
                    deleteRoomLocally(roomName);
                }
            });
            menu.add(deleteItem);

            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e)  {
                    // Chỉ hiện menu xóa nếu là admin
                    if (e.isPopupTrigger() && isAdmin) menu.show(e.getComponent(), e.getX(), e.getY());
                }
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger() && isAdmin) menu.show(e.getComponent(), e.getX(), e.getY());
                }
                public void mouseEntered(MouseEvent e) {
                    if (!roomName.equals(currentRoom)) {
                        row.setBackground(C_HOVER); inner.setBackground(C_HOVER);
                        name.setForeground(C_TEXT);
                    }
                }
                public void mouseExited(MouseEvent e) {
                    if (!roomName.equals(currentRoom)) {
                        row.setBackground(C_ROOMS); inner.setBackground(C_ROOMS);
                        name.setForeground(C_MUTED);
                    }
                }
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && connected) switchToRoom(roomName);
                }
            };
            row.addMouseListener(ma);
            inner.addMouseListener(ma);
            name.addMouseListener(ma);
            hash.addMouseListener(ma);
        } else {
            // Phòng chung: chỉ hover + click, không có menu xóa
            MouseAdapter ma = new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!roomName.equals(currentRoom)) {
                        row.setBackground(C_HOVER); inner.setBackground(C_HOVER);
                        name.setForeground(C_TEXT);
                    }
                }
                public void mouseExited(MouseEvent e) {
                    if (!roomName.equals(currentRoom)) {
                        row.setBackground(C_ROOMS); inner.setBackground(C_ROOMS);
                        name.setForeground(C_MUTED);
                    }
                }
                public void mouseClicked(MouseEvent e) {
                    if (connected) switchToRoom(roomName);
                }
            };
            row.addMouseListener(ma);
            inner.addMouseListener(ma);
            name.addMouseListener(ma);
            hash.addMouseListener(ma);
        }

        roomMessages.putIfAbsent(roomName, new ArrayList<>());
        return row;
    }

    /** Xóa phòng chỉ ở phía client (UI) */
    private void deleteRoomLocally(String roomName) {
        if (roomName.equals("chung")) return;
        if (roomName.equals(currentRoom)) switchToRoom("chung");
        SwingUtilities.invokeLater(() -> {
            JPanel row = roomButtons.remove(roomName);
            if (row != null) {
                int idx = -1;
                Component[] comps = roomListPanel.getComponents();
                for (int i = 0; i < comps.length; i++) {
                    if (comps[i] == row) { idx = i; break; }
                }
                roomListPanel.remove(row);
                // Xóa strut đi kèm nếu còn
                if (idx >= 0 && idx < roomListPanel.getComponentCount()) {
                    Component next = roomListPanel.getComponent(idx);
                    if (next instanceof Box.Filler) roomListPanel.remove(idx);
                }
                roomMessages.remove(roomName);
                roomListPanel.revalidate();
                roomListPanel.repaint();
            }
        });
    }

    private void highlightRoom(String roomName) {
        for (Map.Entry<String, JPanel> entry : roomButtons.entrySet()) {
            JPanel row   = entry.getValue();
            JPanel inner = (JPanel) row.getComponent(0);
            boolean selected = entry.getKey().equals(roomName);
            Color bg = selected ? C_SELECTED : C_ROOMS;
            row.setBackground(bg);
            inner.setBackground(bg);
            // Đặt màu chữ
            for (Component c : inner.getComponents()) {
                if (c instanceof JLabel) {
                    ((JLabel)c).setForeground(selected ? C_TEXT : C_MUTED);
                }
            }
        }
    }

    private void addRoomToSidebar(String roomName) {
        SwingUtilities.invokeLater(() -> {
            if (roomButtons.containsKey(roomName)) return;
            JPanel row = makeRoomRow(roomName);
            roomButtons.put(roomName, row);
            roomListPanel.add(row);
            roomListPanel.add(Box.createVerticalStrut(2));
            roomListPanel.revalidate();
            roomListPanel.repaint();
        });
    }

    // ── MAIN AREA ─────────────────────────────────────────────────

    private JPanel buildMain() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_BG);
        p.add(buildHeader(), BorderLayout.NORTH);
        p.add(buildMsgs(),   BorderLayout.CENTER);
        p.add(buildInput(),  BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(C_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(13, 20, 13, 16)));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setBackground(C_PANEL);
        roomTitle = new JLabel("# chung");
        roomTitle.setForeground(C_TEXT);
        roomTitle.setFont(roomTitle.getFont().deriveFont(Font.BOLD, 15f));
        statusLabel = new JLabel("Chua ket noi");
        statusLabel.setForeground(C_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        left.add(roomTitle);
        left.add(statusLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setBackground(C_PANEL);

        JButton callBtn  = makePhoneBtn();
        JButton videoBtn = makeVideoBtn();
        callBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "Tinh nang goi dien dang phat trien!"));
        videoBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "Tinh nang goi video dang phat trien!"));

        connDot = new JLabel("●  Offline");
        connDot.setForeground(C_RED);
        connDot.setFont(connDot.getFont().deriveFont(Font.BOLD, 13f));

        right.add(callBtn); right.add(videoBtn);
        right.add(Box.createHorizontalStrut(6));
        right.add(connDot);

        p.add(left,  BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JScrollPane buildMsgs() {
        chatArea = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0,0,new Color(13,15,23),
                        0,getHeight(),new Color(18,22,38)));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.dispose();
            }
        };
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
        chatArea.setOpaque(false);
        chatArea.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0,0,new Color(13,15,23),
                        0,getHeight(),new Color(18,22,38)));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.add(chatArea, BorderLayout.NORTH);

        chatScroll = new JScrollPane(wrap);
        chatScroll.setBorder(null);
        chatScroll.getViewport().setBackground(C_BG);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        return chatScroll;
    }

    private JPanel buildInput() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(C_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER),
                new EmptyBorder(10, 14, 12, 14)));

        JButton emojiBtn = makeSmileyBtn();
        emojiBtn.addActionListener(e -> showEmojiPicker(emojiBtn));

        inputField = new JTextField();
        inputField.setBackground(C_INPUT);
        inputField.setForeground(C_TEXT);
        inputField.setCaretColor(C_ACCENT2);
        inputField.setFont(inputField.getFont().deriveFont(Font.PLAIN, 14f));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(9, 12, 9, 12)));
        inputField.setEnabled(false);
        inputField.addActionListener(e -> doSend());

        sendButton = new JButton("Gui");
        sendButton.setBackground(C_ACCENT);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD, 13f));
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setEnabled(false);
        sendButton.setPreferredSize(new Dimension(80, 42));
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> doSend());

        JPanel left = new JPanel(new BorderLayout(6, 0));
        left.setBackground(C_PANEL);
        left.add(emojiBtn,   BorderLayout.WEST);
        left.add(inputField, BorderLayout.CENTER);

        p.add(left,       BorderLayout.CENTER);
        p.add(sendButton, BorderLayout.EAST);
        return p;
    }

    // ── ROOM ACTIONS ──────────────────────────────────────────────

    private void switchToRoom(String roomName) {
        if (roomName.equals(currentRoom)) return;
        currentRoom = roomName;
        client.switchRoom(roomName);
        roomTitle.setText("# " + roomName);
        highlightRoom(roomName);

        // Xoa chat + online list — server se gui lai lich su khi join
        chatArea.removeAll();
        onlineUsers.clear();
        userListPanel.removeAll();
        userListPanel.revalidate();
        userListPanel.repaint();

        // Them chinh minh vao online list phong moi
        addUserDot(myName);

        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }

    private void showCreateRoomDialog() {
        if (!connected) {
            JOptionPane.showMessageDialog(this, "Ban chua ket noi!"); return;
        }
        String name = JOptionPane.showInputDialog(this,
                "Nhap ten phong moi (khong dau, khong cach):", "Tao phong", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        name = name.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        client.createRoom(name);
    }

    // ── NETWORK ───────────────────────────────────────────────────

    private void showLogin() {
        JDialog dlg = new JDialog(this, "Dang nhap ChatBox", true);
        dlg.setSize(460, 320);
        dlg.setLocationRelativeTo(null);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_PANEL);
        root.setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel title = new JLabel("ChatBox Login", SwingConstants.CENTER);
        title.setForeground(C_TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel form = new JPanel(new GridLayout(3, 1, 0, 10));
        form.setBackground(C_PANEL);

        JTextField tfName = plainField("Ten nguoi dung");
        JTextField tfHost = plainField("Server (mac dinh: localhost)");
        JTextField tfPort = plainField("Port (mac dinh: 9999)");
        form.add(tfName); form.add(tfHost); form.add(tfPort);

        JButton btn = new JButton("Ket noi");
        btn.setBackground(C_ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 14f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(0, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setBackground(C_PANEL);
        btnWrap.setBorder(new EmptyBorder(16, 0, 0, 0));
        btnWrap.add(btn, BorderLayout.CENTER);

        btn.addActionListener(e -> {
            String name = tfName.getText().trim();
            if (name.isEmpty() || name.equals("Ten nguoi dung")) {
                JOptionPane.showMessageDialog(dlg, "Vui long nhap ten!"); return;
            }
            String host = tfHost.getText().trim();
            if (host.isEmpty() || host.equals("Server (mac dinh: localhost)")) host = "localhost";
            int port = 9999;
            try {
                String p = tfPort.getText().trim();
                if (!p.isEmpty() && !p.equals("Port (mac dinh: 9999)")) port = Integer.parseInt(p);
            } catch (Exception ignored) {}
            doConnect(name, host, port, dlg);
        });

        root.add(title,   BorderLayout.NORTH);
        root.add(form,    BorderLayout.CENTER);
        root.add(btnWrap, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // Fields dùng chung cho login dialog
    private JTextField[] fName, fHost, fPort;

    /** Field có placeholder mờ, chữ hiện đầy đủ không bị clip */
    private JTextField plainField(String placeholder) {
        JTextField f = new JTextField();
        // Đặt preferred height đủ lớn để chữ không bị cắt
        f.setPreferredSize(new Dimension(0, 40));
        f.setBackground(C_INPUT);
        f.setForeground(C_MUTED);
        f.setCaretColor(C_ACCENT2);
        // Font size 13 — vừa đủ, không bị clip
        f.setFont(f.getFont().deriveFont(Font.PLAIN, 13f));
        // Margin trong field — top/bottom nhỏ để chữ không bị cắt
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(0, 12, 0, 12)));
        f.setText(placeholder);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) {
                    f.setText("");
                    f.setForeground(C_TEXT);
                }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().trim().isEmpty()) {
                    f.setText(placeholder);
                    f.setForeground(C_MUTED);
                }
            }
        });
        return f;
    }

    private void doConnect(String name, String host, int port, JDialog dlg) {
        myName = name;
        client = new NetworkClient(host, port, name, encryptBox.isSelected());
        client.setOnMessageReceived(this::onMsg);
        client.setOnDisconnected(this::onDisconn);
        try {
            client.connect();
            connected = true;
            dlg.dispose();
            setTitle("ChatBox  —  " + name);
            statusLabel.setText("Ket noi: " + host + ":" + port
                    + (encryptBox.isSelected() ? "  [AES]" : ""));
            connDot.setText("●  Online");
            connDot.setForeground(C_GREEN);
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.requestFocus();
            addUserDot(name);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dlg,
                    "Khong the ket noi!\n" + ex.getMessage(),
                    "Loi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onMsg(Message msg) {
        SwingUtilities.invokeLater(() -> handleMessage(msg));
    }

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case ROOM_LIST:
                String[] rms = msg.getContent().split(",");
                Set<String> serverRooms = new HashSet<>(Arrays.asList(rms));
                for (String r : rms) addRoomToSidebar(r.trim());
                new ArrayList<>(roomButtons.keySet()).forEach(r -> {
                    if (!serverRooms.contains(r)) deleteRoomLocally(r);
                });
                highlightRoom(currentRoom);
                break;

            case SWITCH_ROOM:
                currentRoom = msg.getRoom();
                client.setCurrentRoom(currentRoom);
                roomTitle.setText("# " + currentRoom);
                highlightRoom(currentRoom);
                // Hiện 1 dòng nhỏ giữa chat — không phải bubble System
                sysMsg("\u0110\u00e3 v\u00e0o ph\u00f2ng # " + currentRoom);
                break;

            case JOIN:
                String joiner = msg.getSender();
                // Chỉ cập nhật online list nếu người đó vào CÙNG phòng mình đang ở
                if (msg.getRoom().equals(currentRoom)) {
                    addUserDot(joiner);
                    sysMsg(joiner + " \u0111\u00e3 v\u00e0o ph\u00f2ng");
                }
                roomMessages.computeIfAbsent(msg.getRoom(), k -> new ArrayList<>()).add(msg);
                break;

            case LEAVE:
                String leaver = msg.getSender();
                // Chỉ xóa khỏi online list nếu người đó rời CÙNG phòng mình đang ở
                if (msg.getRoom().equals(currentRoom)) {
                    removeUserDot(leaver);
                    sysMsg(leaver + " \u0111\u00e3 r\u1eddi ph\u00f2ng");
                }
                roomMessages.computeIfAbsent(msg.getRoom(), k -> new ArrayList<>()).add(msg);
                break;

            case ADMIN_INFO:
                isAdmin = "true".equals(msg.getContent());
                addRoomBtn.setVisible(isAdmin);
                if (isAdmin) {
                    statusLabel.setText(statusLabel.getText() + "  [ADMIN]");
                }
                break;

            case SYSTEM:
                // Chỉ hiện các msg system thực sự quan trọng (lỗi, cảnh báo)
                String sc = msg.getContent();
                boolean skip = sc.contains("da tham gia") || sc.contains("da vao")
                        || sc.contains("da roi") || sc.contains("tham gia phong")
                        || sc.contains("nguoi dang online");
                if (!skip) sysMsg(sc);
                break;

            case TEXT:
                // Server gui truc tiep lich su + tin nhan moi → render thang
                if (msg.getRoom().equals(currentRoom)) renderMsg(msg);
                else {
                    // Luu vao cache cua phong khac de hien khi switch
                    roomMessages.computeIfAbsent(msg.getRoom(), k -> new ArrayList<>()).add(msg);
                }
                break;

            default: break;
        }
    }

    private void onDisconn() {
        SwingUtilities.invokeLater(() -> {
            connected = false;
            connDot.setText("●  Offline");
            connDot.setForeground(C_RED);
            statusLabel.setText("Mat ket noi");
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
            sysMsg("Mat ket noi den server!");
        });
    }

    private void doSend() {
        if (!connected) return;
        String txt = inputField.getText().trim();
        if (txt.isEmpty()) return;
        client.sendMessage(txt);
        inputField.setText("");
        inputField.requestFocus();
    }

    // ── RENDER ────────────────────────────────────────────────────

    private void renderMsg(Message msg) {
        // Bỏ qua tin nhắn rỗng (JOIN/LEAVE dùng content="" )
        if (msg.getContent() == null || msg.getContent().trim().isEmpty()) return;
        boolean mine = msg.getSender().equals(myName);

        JPanel row = new JPanel(new FlowLayout(
                mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 3));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));

        JPanel bubble = new RoundPanel(mine ? C_BSELF : C_BOTHER, 14);
        bubble.setLayout(new BorderLayout(0, 4));
        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));

        if (!mine) {
            JLabel sender = new JLabel(msg.getSender());
            sender.setForeground(C_ACCENT2);
            sender.setFont(sender.getFont().deriveFont(Font.BOLD, 12f));
            bubble.add(sender, BorderLayout.NORTH);
        }

        JTextArea body = new JTextArea(msg.getContent());
        body.setForeground(C_TEXT);
        body.setBackground(mine ? C_BSELF : C_BOTHER);
        body.setFont(body.getFont().deriveFont(Font.PLAIN, 14f));
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBorder(null);
        body.setOpaque(false);

        JLabel time = new JLabel(msg.getTimestamp());
        time.setForeground(new Color(150, 155, 200));
        time.setFont(time.getFont().deriveFont(Font.PLAIN, 10f));
        time.setHorizontalAlignment(mine ? SwingConstants.RIGHT : SwingConstants.LEFT);

        bubble.add(body, BorderLayout.CENTER);
        bubble.add(time, BorderLayout.SOUTH);
        bubble.setMaximumSize(new Dimension(460, 9999));

        row.add(bubble);
        chatArea.add(row);
        chatArea.add(Box.createVerticalStrut(6));
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }

    private void sysMsg(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        JLabel lbl = new JLabel(text);
        lbl.setForeground(C_MUTED);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_BORDER, 1),
                new EmptyBorder(3, 12, 3, 12)));
        row.add(lbl);
        chatArea.add(row);
        chatArea.add(Box.createVerticalStrut(4));
        chatArea.revalidate();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() ->
                chatScroll.getVerticalScrollBar().setValue(
                        chatScroll.getVerticalScrollBar().getMaximum()));
    }

    // ── ONLINE USERS ──────────────────────────────────────────────

    private void addUserDot(String name) {
        if (!onlineUsers.add(name)) return;
        SwingUtilities.invokeLater(() -> {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setName(name);
            row.setBackground(C_ROOMS);
            row.setBorder(new EmptyBorder(3, 12, 3, 8));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

            JLabel dot = new JLabel("●");
            dot.setForeground(C_GREEN);
            dot.setFont(dot.getFont().deriveFont(Font.PLAIN, 9f));

            // Dùng JLabel với preferred width lớn để hiện đủ tên
            JLabel nm = new JLabel(name);
            nm.setForeground(C_TEXT);
            nm.setFont(nm.getFont().deriveFont(Font.PLAIN, 13f));
            // Không giới hạn width — hiện full tên
            nm.setPreferredSize(null);

            row.add(dot, BorderLayout.WEST);
            row.add(nm,  BorderLayout.CENTER);

            userListPanel.add(row);
            userListPanel.add(Box.createVerticalStrut(2));
            userListPanel.revalidate();
            userListPanel.repaint();
        });
    }

    private void removeUserDot(String name) {
        onlineUsers.remove(name);
        SwingUtilities.invokeLater(() -> {
            for (Component c : userListPanel.getComponents()) {
                if (name.equals(c.getName())) { userListPanel.remove(c); break; }
            }
            userListPanel.revalidate();
            userListPanel.repaint();
        });
    }

    // ── EMOJI ─────────────────────────────────────────────────────

    private void showEmojiPicker(Component anchor) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(C_PANEL);
        popup.setBorder(BorderFactory.createLineBorder(C_BORDER, 1));

        JPanel grid = new JPanel(new GridLayout(5, 6, 4, 4));
        grid.setBackground(C_PANEL);
        grid.setBorder(new EmptyBorder(8, 8, 8, 8));

        for (String[] entry : EMOJI_DATA) {
            String unicode = entry[0], cp = entry[1];
            JButton btn = new JButton("...");
            btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 10f));
            btn.setBackground(C_INPUT);
            btn.setForeground(C_MUTED);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setPreferredSize(new Dimension(44, 40));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(unicode);
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btn.setBackground(C_BORDER); }
                public void mouseExited(MouseEvent e)  { btn.setBackground(C_INPUT);  }
            });
            btn.addActionListener(e -> {
                inputField.setText(inputField.getText() + unicode);
                inputField.requestFocus();
                popup.setVisible(false);
            });
            grid.add(btn);

            new SwingWorker<ImageIcon, Void>() {
                protected ImageIcon doInBackground() { return loadTwemoji(cp); }
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        if (icon != null) { btn.setIcon(icon); btn.setText(""); }
                        else { btn.setText(unicode); btn.setForeground(C_YELLOW);
                               btn.setFont(btn.getFont().deriveFont(Font.BOLD, 11f)); }
                    } catch (Exception ignored) {}
                }
            }.execute();
        }

        popup.add(grid);
        popup.show(anchor, 0, -popup.getPreferredSize().height - 4);
    }

    private static ImageIcon loadTwemoji(String cp) {
        return emojiCache.computeIfAbsent(cp, k -> {
            try {
                String url = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/14.0.2/72x72/" + k + ".png";
                Image img = javax.imageio.ImageIO.read(new java.net.URL(url))
                        .getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            } catch (Exception e) { return null; }
        });
    }

    // ── CUSTOM BUTTONS ────────────────────────────────────────────

    private JButton makeSmileyBtn() {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth()/2, cy = getHeight()/2, r = 10;
                g2.setColor(C_MUTED);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(cx-r, cy-r, r*2, r*2);
                g2.setColor(C_TEXT);
                g2.fillOval(cx-4, cy-4, 2, 2);
                g2.fillOval(cx+2, cy-4, 2, 2);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(cx-5, cy-1, 10, 7, 0, -180);
                g2.dispose();
            }
        };
        b.setBackground(C_INPUT); b.setFocusPainted(false); b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(44, 42));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText("Chon emoji");
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(C_BORDER); b.repaint(); }
            public void mouseExited(MouseEvent e)  { b.setBackground(C_INPUT);  b.repaint(); }
        });
        return b;
    }

    private JButton makePhoneBtn() {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_TEXT);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth()/2, cy = getHeight()/2;
                int[] px = {cx-7,cx-4,cx-2,cx+2,cx+6,cx+7};
                int[] py = {cy+6,cy+7,cy+3,cy-2,cy-6,cy-7};
                g2.drawPolyline(px, py, px.length);
                g2.drawArc(cx-8, cy-9, 8, 8, 180, -90);
                g2.drawArc(cx+1, cy+2, 8, 8,   0, -90);
                g2.dispose();
            }
        };
        styleIconBtn(b, "Goi dien"); return b;
    }

    private JButton makeVideoBtn() {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_TEXT);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth()/2, cy = getHeight()/2;
                g2.drawRoundRect(cx-10, cy-6, 13, 12, 3, 3);
                int[] vx = {cx+4, cx+10, cx+10};
                int[] vy = {cy-4, cy-7,  cy+7};
                g2.drawPolygon(vx, vy, 3);
                g2.dispose();
            }
        };
        styleIconBtn(b, "Goi video"); return b;
    }

    private void styleIconBtn(JButton b, String tooltip) {
        b.setBackground(C_INPUT); b.setFocusPainted(false); b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(40, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(tooltip);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(C_BORDER); b.repaint(); }
            public void mouseExited(MouseEvent e)  { b.setBackground(C_INPUT);  b.repaint(); }
        });
    }

    // ── HELPERS ───────────────────────────────────────────────────

    private JButton sideBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(C_INPUT); b.setForeground(C_TEXT);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void showHistory() {
        List<String> h = ChatHistoryXML.loadHistory();
        if (h.isEmpty()) { JOptionPane.showMessageDialog(this, "Chua co lich su."); return; }
        JTextArea a = new JTextArea(String.join("\n", h));
        a.setEditable(false); a.setBackground(C_INPUT); a.setForeground(C_TEXT);
        JScrollPane sp = new JScrollPane(a);
        sp.setPreferredSize(new Dimension(500, 340));
        JOptionPane.showMessageDialog(this, sp,
                "Lich su Chat - " + h.size() + " tin nhan", JOptionPane.PLAIN_MESSAGE);
    }

    // ── ROUND BUBBLE ──────────────────────────────────────────────

    static class RoundPanel extends JPanel {
        private final Color color; private final int arc;
        RoundPanel(Color c, int a) { color=c; arc=a; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── MAIN ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatGUI().setVisible(true));
    }
}
