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

    // Getters e setters específicos
    public String getRaca() { return raca; }
    public void setRaca(String raca) { this.raca = raca; }
    public boolean isSeDaBemComCachorros() { return seDaBemComCachorros; }
    public void setSeDaBemComCachorros(boolean v) { this.seDaBemComCachorros = v; }
    public boolean isSeDaBemComGatos() { return seDaBemComGatos; }
    public void setSeDaBemComGatos(boolean v) { this.seDaBemComGatos = v; }
    public boolean isSeDaBemComCriancas() { return seDaBemComCriancas; }
    public void setSeDaBemComCriancas(boolean v) { this.seDaBemComCriancas = v; }
    public boolean isAcessoExterior() { return acessoExterior; }
    public void setAcessoExterior(boolean acessoExterior) { this.acessoExterior = acessoExterior; }
    public boolean isPossuiTelamento() { return possuiTelamento; }
    public void setPossuiTelamento(boolean possuiTelamento) { this.possuiTelamento = possuiTelamento; }
}
