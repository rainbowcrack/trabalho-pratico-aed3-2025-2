package br.com.mpet;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do algoritmo de casamento de padrões Knuth-Morris-Pratt (KMP).
 */
public class KMP {

    /**
     * Pré-processa o padrão para construir o array LPS (Longest Proper Prefix which is also Suffix).
     */
    private int[] computeLPSArray(String pattern) {
        int length = 0;
        int i = 1;
        int[] lps = new int[pattern.length()];
        lps[0] = 0;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(length)) {
                length++;
                lps[i] = length;
                i++;
            } else {
                if (length != 0) {
                    length = lps[length - 1];
                } else {
                    lps[i] = length;
                    i++;
                }
            }
        }
        return lps;
    }

    /**
     * Procura por todas as ocorrências de um padrão em um texto.
     * @return Uma lista de índices onde o padrão foi encontrado.
     */
    public List<Integer> search(String text, String pattern) {
        List<Integer> occurrences = new ArrayList<>();
        int patternLength = pattern.length();
        int textLength = text.length();
        int[] lps = computeLPSArray(pattern);
        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < textLength) {
            if (pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }
            if (j == patternLength) {
                occurrences.add(i - j);
                j = lps[j - 1];
            } else if (i < textLength && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) j = lps[j - 1];
                else i++;
            }
        }
        return occurrences;
    }
}