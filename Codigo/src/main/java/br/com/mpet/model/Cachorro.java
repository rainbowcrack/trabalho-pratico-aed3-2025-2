package br.com.mpet.model;

public class Cachorro extends Animal {
    String raca; // Raça do cachorro, caso nao tenha usar "SRD"
    NecessidadeExercicio necessidadeExercicio; // Baixa, media, alta
    NivelAdestramento nivelAdestramento; // Nenhum, basico, avancado
    boolean seDaBemComCachorros, seDaBemComGatos, seDaBemComCriancas;
    
}

// NAO PRECISA DE NECESSIDADE DE EXERCICIO, APENAS RACA E BOOLEAN E VACINA (DETALHE: PODE ESTAR EM FICHA MEDICA)