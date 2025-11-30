package br.com.mpet;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do algoritmo de casamento de padrões Boyer-Moore
 * com a heurística do "mau caractere" (bad character).
 */
public class BoyerMoore {

    private static final int ALPHABET_SIZE = 256; // ASCII

    /**
     * Pré-processa o padrão para construir a tabela de "mau caractere".
     * A tabela armazena a última ocorrência de cada caractere no padrão.
     */
    private int[] buildBadCharTable(String pattern) {
        int[] badChar = new int[ALPHABET_SIZE];
        int patternLength = pattern.length();

        for (int i = 0; i < ALPHABET_SIZE; i++) {
            badChar[i] = -1; // Inicializa todas as ocorrências como -1
        }

        for (int i = 0; i < patternLength; i++) {
            badChar[(int) pattern.charAt(i)] = i;
        }

        return badChar;
    }

    /**
     * Procura por todas as ocorrências de um padrão em um texto.
     * @return Uma lista de índices onde o padrão foi encontrado.
     */
    public List<Integer> search(String text, String pattern) {
        List<Integer> occurrences = new ArrayList<>();
        int patternLength = pattern.length();
        int textLength = text.length();
        int[] badChar = buildBadCharTable(pattern);

        int shift = 0;
        while (shift <= (textLength - patternLength)) {
            int j = patternLength - 1;

            while (j >= 0 && pattern.charAt(j) == text.charAt(shift + j)) {
                j--;
            }

            if (j < 0) {
                occurrences.add(shift);
                shift += (shift + patternLength < textLength) ? patternLength - badChar[text.charAt(shift + patternLength)] : 1;
            } else {
                shift += Math.max(1, j - badChar[text.charAt(shift + j)]);
            }
        }
        return occurrences;
    }
}