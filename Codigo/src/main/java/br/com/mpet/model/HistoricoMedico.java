package br.com.mpet.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Ficha médica do animal — apenas atributos (POJO de suporte).
 */
public class HistoricoMedico {
    // Indicadores básicos
    private boolean castrado; // true se castrado
    private boolean vermifugado; // true se vermifugado

    // Listas de registros detalhados
    private List<Vacina> vacinas; // lista de vacinas aplicadas
    private List<Exame> exames; // lista de exames realizados

    // Condições e medicação
    private String condicoesCronicas; // descrição de doenças crônicas
    private boolean usaMedicacaoContinua; // indicativo
    private String medicacaoDescricao; // nomes e dosagens

    // Controle
    private LocalDate dataUltimoCheckup; // data do último check-up
}
