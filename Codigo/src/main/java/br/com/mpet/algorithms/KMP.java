package br.com.mpet.algorithms;

public class KMP {
    public static boolean contains(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) return true;
        if (text == null) return false;
        int[] lps = buildLps(pattern);
        int i = 0, j = 0;
        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++; j++;
                if (j == pattern.length()) return true;
            } else if (j != 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return false;
    }

    private static int[] buildLps(String p) {
        int[] lps = new int[p.length()];
        int len = 0; // length of the previous longest prefix suffix
        int i = 1;
        while (i < p.length()) {
            if (p.charAt(i) == p.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0; i++;
                }
            }
        }
        return lps;
    }
}
