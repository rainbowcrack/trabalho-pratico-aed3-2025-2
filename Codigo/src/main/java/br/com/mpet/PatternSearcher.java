package br.com.mpet;

import java.util.List;

/**
 * Classe de serviço para realizar buscas de padrões usando diferentes algoritmos.
 */
public class PatternSearcher {

    public enum Algorithm {
        KMP,
        BOYER_MOORE
    }

    private final KMP kmp;
    private final BoyerMoore boyerMoore;

    public PatternSearcher() {
        this.kmp = new KMP();
        this.boyerMoore = new BoyerMoore();
    }

    /**
     * Realiza a busca de um padrão em um texto usando o algoritmo especificado.
     *
     * @param text O texto onde a busca será realizada.
     * @param pattern O padrão a ser encontrado.
     * @param algorithm O algoritmo a ser utilizado (KMP ou BOYER_MOORE).
     * @return true se o padrão for encontrado, false caso contrário.
     */
    public boolean search(String text, String pattern, Algorithm algorithm) {
        List<Integer> occurrences = algorithm == Algorithm.KMP
                ? kmp.search(text, pattern)
                : boyerMoore.search(text, pattern);
        return !occurrences.isEmpty();
    }
}