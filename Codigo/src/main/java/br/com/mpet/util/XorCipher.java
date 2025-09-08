package br.com.mpet.util;

import java.nio.charset.StandardCharsets;

public class XorCipher {
    public static byte[] apply(byte[] data, String key) {
        byte[] k = key.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte)(data[i] ^ k[i % k.length]);
        }
        return out;
    }

    public static String encrypt(String plain, String key) {
        return bytesToHex(apply(plain.getBytes(StandardCharsets.UTF_8), key));
    }

    public static String decrypt(String hex, String key) {
        byte[] data = hexToBytes(hex);
        return new String(apply(data, key), StandardCharsets.UTF_8);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
