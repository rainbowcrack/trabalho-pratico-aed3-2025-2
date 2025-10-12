package br.com.mpet.model;

import java.time.LocalDate;

/**
 * Registro de exame clínico/laboratorial.
 */
public class Exame {
    private String nome; // ex: "Hemograma", "FIV/FeLV"
    private LocalDate dataRealizacao;
    private ResultadoTeste resultadoPadrao; // POSITIVO, NEGATIVO, NAO_TESTADO
    private String resultadoLivre; // texto do laudo
    private String observacoes;
}

// INTERESSANTE
// valeu =)