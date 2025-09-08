package br.com.mpet.storage;

import java.io.*;
import java.util.*;

/**
 * Simple LZW implementation for byte arrays (not optimized). Encodes to int codes then packs as 32-bit ints.
 */
public class LZWCodec implements PayloadCodec {
    @Override public byte[] encode(byte[] data) {
        if (data == null || data.length == 0) return new byte[0];
        // Build initial dictionary
        Map<String, Integer> dict = new HashMap<>();
        for (int i = 0; i < 256; i++) dict.put(String.valueOf((char)i), i);
        int dictSize = 256;
        String w = "";
        List<Integer> codes = new ArrayList<>();
        for (byte b : data) {
            char c = (char)(b & 0xFF);
            String wc = w + c;
            if (dict.containsKey(wc)) {
                w = wc;
            } else {
                codes.add(dict.get(w));
                dict.put(wc, dictSize++);
                w = String.valueOf(c);
            }
        }
        if (!w.isEmpty()) codes.add(dict.get(w));
        // pack as 32-bit ints
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(codes.size());
            for (int code : codes) dos.writeInt(code);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    @Override public byte[] decode(byte[] data) {
        if (data == null || data.length == 0) return new byte[0];
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            int n = dis.readInt();
            if (n <= 0) return new byte[0];
            int[] codes = new int[n];
            for (int i = 0; i < n; i++) codes[i] = dis.readInt();
            // Rebuild dictionary
            Map<Integer, String> dict = new HashMap<>();
            for (int i = 0; i < 256; i++) dict.put(i, String.valueOf((char)i));
            int dictSize = 256;
            String w = dict.get(codes[0]);
            StringBuilder out = new StringBuilder(w);
            for (int i = 1; i < codes.length; i++) {
                int k = codes[i];
                String entry;
                if (dict.containsKey(k)) entry = dict.get(k);
                else if (k == dictSize) entry = w + w.charAt(0);
                else throw new IllegalStateException("Invalid LZW code: " + k);
                out.append(entry);
                dict.put(dictSize++, w + entry.charAt(0));
                w = entry;
            }
            // convert to bytes (0-255)
            byte[] res = new byte[out.length()];
            for (int i = 0; i < out.length(); i++) res[i] = (byte)(out.charAt(i) & 0xFF);
            return res;
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}
