package br.com.mpet.util;

import java.util.*;

public class LZW {
    public static List<Integer> compress(String uncompressed) {
        int dictSize = 256;
        Map<String,Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) dictionary.put("" + (char)i, i);

        String w = "";
        List<Integer> result = new ArrayList<>();
        for (char c : uncompressed.toCharArray()) {
            String wc = w + c;
            if (dictionary.containsKey(wc)) w = wc;
            else {
                result.add(dictionary.get(w));
                dictionary.put(wc, dictSize++);
                w = "" + c;
            }
        }
        if (!w.isEmpty()) result.add(dictionary.get(w));
        return result;
    }

    public static String decompress(List<Integer> compressed) {
        int dictSize = 256;
        Map<Integer,String> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) dictionary.put(i, "" + (char)i);

        Iterator<Integer> iterator = compressed.iterator();
        if (!iterator.hasNext()) return "";
        int k = iterator.next();
        String w = dictionary.get(k);
        StringBuilder result = new StringBuilder(w);

        while (iterator.hasNext()) {
            k = iterator.next();
            String entry;
            if (dictionary.containsKey(k)) entry = dictionary.get(k);
            else if (k == dictSize) entry = w + w.charAt(0);
            else throw new IllegalArgumentException("Bad compressed k: " + k);

            result.append(entry);
            dictionary.put(dictSize++, w + entry.charAt(0));
            w = entry;
        }
        return result.toString();
    }
}
