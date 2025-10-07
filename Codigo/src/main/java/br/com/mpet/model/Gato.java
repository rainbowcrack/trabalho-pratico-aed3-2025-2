package br.com.mpet.model;

/**
 * Gato — especialização de Animal. Campos médicos como exames/FIV/FeLV
 * devem ser registrados em `HistoricoMedico.exames`.
 */
public class Gato extends Animal {

    String raca; // Raça do gato, usar "SRD" quando não definida

    // Sociabilidade
    boolean seDaBemComCachorros;
    boolean seDaBemComGatos;
    boolean seDaBemComCriancas;

    // Comportamento e logística
    boolean acessoExterior; // acostumado a sair para fora
    boolean possuiTelamento; // recomendado para adoção em apartamentos

}
