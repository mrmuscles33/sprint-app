package com.spring.application.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Component
public class StringUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String SPECIAL_CHARS = "àâäéèêëîïôöùûüÿçµ£¤§!@#$%^&*()-°¨_+[]{}|;:',.<>?/~`";

    private StringUtils() {
        super();
    }

    /**
     * Check if a string is empty
     *
     * @param str The string to check
     * @return true if the string is empty or null
     */
    public static boolean isEmpty(Object str) {
        return str == null || StringUtils.toString(str).trim().isEmpty();
    }

    /**
     * Convert an object to a string
     * If the object is null, return the empty string
     *
     * @param obj The object to convert
     * @return The string
     */
    public static String toString(Object obj) {
        return toString(obj, "");
    }

    /**
     * Convert an object to a string
     * If the object is null, return the default value
     *
     * @param obj          The object to convert
     * @param defaultValue The default value
     * @return The string
     */
    public static String toString(Object obj, String defaultValue) {
        return Objects.toString(obj, defaultValue);
    }

    /**
     * Convert an object to a boolean
     *
     * @param value The object to convert
     * @return The boolean
     */
    public static boolean toBool(Object value) {
        String strValue = toString(value);
        return "true".equalsIgnoreCase(strValue) ||
                "1".equals(strValue) ||
                "on".equalsIgnoreCase(strValue) ||
                "yes".equalsIgnoreCase(strValue) ||
                "y".equalsIgnoreCase(strValue) ||
                "oui".equalsIgnoreCase(strValue) ||
                "o".equalsIgnoreCase(strValue);
    }

    /**
     * Encode a string in base64
     *
     * @param message The message to encode
     * @return The encoded message
     */
    public static String encode64(String message) {
        return Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decode a base64 message
     *
     * @param message The message to decode
     * @return The decoded message
     */
    public static String decode64(String message) {
        return new String(Base64.getDecoder().decode(message), StandardCharsets.UTF_8);
    }

    /**
     * Crypt a message with a key
     *
     * @param message The message to crypt
     * @param key     The secret key
     * @return The crypted message
     */
    public static String crypt(String message, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transform a JSON string to a Map
     *
     * @param json The JSON string
     * @return The Map
     */
    public static Map<String, ?> jsonToMap(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
    }

    /**
     * Transform a Map to a JSON string
     *
     * @param map The Map
     * @return The JSON string
     */
    public static String mapToJson(Map<String, ?> map) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(map);
    }

    /**
     * Generate a random string of a given size
     *
     * @param size The size of the string
     * @return The random string
     */
    public static String random(int size) {
        return random(size, false);
    }

    /**
     * Generate a random string of a given size
     *
     * @param size         The size of the string
     * @param specialChars If true, the string will contain special characters
     * @return The random string
     */
    public static String random(int size, boolean specialChars) {
        String chars = specialChars ? CHARS + SPECIAL_CHARS : CHARS;
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }
}
