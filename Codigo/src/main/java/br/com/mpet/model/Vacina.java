package br.com.mpet.model;

import java.time.LocalDate;

/**
 * Registro de vacina aplicada.
 */
public class Vacina {
    private String nome; // ex: "Antirrábica"
    private LocalDate dataAplicacao;
    private String observacoes; // lote, reforço, reações
}
