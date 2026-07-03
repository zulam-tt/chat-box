package chatbox.xml;

import chatbox.model.Message;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * XML Chat History - Topic 5: XML with Java
 * Lưu và đọc lịch sử chat từ file XML
 */
public class ChatHistoryXML {

    private static final String FILE_PATH = "chat_history.xml";

    /**
     * Lưu một tin nhắn vào file XML
     */
    public static synchronized void saveMessage(Message msg) {
        try {
            Document doc;
            Element root;
            File file = new File(FILE_PATH);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            if (file.exists()) {
                doc = builder.parse(file);
                root = doc.getDocumentElement();
            } else {
                doc = builder.newDocument();
                root = doc.createElement("ChatHistory");
                root.setAttribute("date", LocalDate.now().toString());
                doc.appendChild(root);
            }

            // Tạo element <message>
            Element msgEl = doc.createElement("message");
            msgEl.setAttribute("type", msg.getType().toString());
            msgEl.setAttribute("encrypted", String.valueOf(msg.isEncrypted()));

            Element senderEl = doc.createElement("sender");
            senderEl.setTextContent(msg.getSender());

            Element contentEl = doc.createElement("content");
            contentEl.setTextContent(msg.getContent());

            Element timeEl = doc.createElement("timestamp");
            timeEl.setTextContent(msg.getTimestamp());

            msgEl.appendChild(senderEl);
            msgEl.appendChild(contentEl);
            msgEl.appendChild(timeEl);
            root.appendChild(msgEl);

            // Ghi ra file
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(doc), new StreamResult(file));

        } catch (Exception e) {
            System.err.println("[XML] Save error: " + e.getMessage());
        }
    }

    /**
     * Đọc toàn bộ lịch sử chat từ file XML
     */
    public static List<String> loadHistory() {
        List<String> history = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return history;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList messages = doc.getElementsByTagName("message");
            for (int i = 0; i < messages.getLength(); i++) {
                Element el = (Element) messages.item(i);
                String sender    = el.getElementsByTagName("sender").item(0).getTextContent();
                String content   = el.getElementsByTagName("content").item(0).getTextContent();
                String timestamp = el.getElementsByTagName("timestamp").item(0).getTextContent();
                history.add("[" + timestamp + "] " + sender + ": " + content);
            }
        } catch (Exception e) {
            System.err.println("[XML] Load error: " + e.getMessage());
        }
        return history;
    }

    /**
     * Xóa lịch sử (tạo file mới)
     */
    public static void clearHistory() {
        File file = new File(FILE_PATH);
        if (file.exists()) file.delete();
    }
}
