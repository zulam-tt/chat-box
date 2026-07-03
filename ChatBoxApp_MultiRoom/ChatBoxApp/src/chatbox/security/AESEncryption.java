package chatbox.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES Encryption Utility - Topic 7: Java Security
 * Mã hóa/giải mã tin nhắn bằng thuật toán AES-128
 */
public class AESEncryption {

    private static final String ALGORITHM = "AES";
    private static final String SHARED_KEY = "ChatBoxSecureKey"; // 16 bytes = AES-128

    private static SecretKey getKey() {
        byte[] keyBytes = SHARED_KEY.getBytes();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Mã hóa plaintext -> Base64 ciphertext
     */
    public static String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            System.err.println("[Security] Encrypt error: " + e.getMessage());
            return plaintext; // fallback
        }
    }

    /**
     * Giải mã Base64 ciphertext -> plaintext
     */
    public static String decrypt(String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            System.err.println("[Security] Decrypt error: " + e.getMessage());
            return ciphertext; // fallback
        }
    }

    /**
     * Tạo key ngẫu nhiên (demo)
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(128);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            return null;
        }
    }
}
