package br.com.mpet.model;

public class Cachorro extends Animal {
    String raca; // Raça do cachorro, caso nao tenha usar "SRD"
    NivelAdestramento nivelAdestramento; // Nenhum, basico, avancado
    boolean seDaBemComCachorros, seDaBemComGatos, seDaBemComCriancas;
    
    // Getters e Setters específicos
    public String getRaca() { return raca; }
    public void setRaca(String raca) { this.raca = raca; }
    public NivelAdestramento getNivelAdestramento() { return nivelAdestramento; }
    public void setNivelAdestramento(NivelAdestramento nivelAdestramento) { this.nivelAdestramento = nivelAdestramento; }
    public boolean isSeDaBemComCachorros() { return seDaBemComCachorros; }
    public void setSeDaBemComCachorros(boolean v) { this.seDaBemComCachorros = v; }
    public boolean isSeDaBemComGatos() { return seDaBemComGatos; }
    public void setSeDaBemComGatos(boolean v) { this.seDaBemComGatos = v; }
    public boolean isSeDaBemComCriancas() { return seDaBemComCriancas; }
    public void setSeDaBemComCriancas(boolean v) { this.seDaBemComCriancas = v; }
}

// NAO PRECISA DE NECESSIDADE DE EXERCICIO, APENAS RACA E BOOLEAN E VACINA (DETALHE: PODE ESTAR EM FICHA MEDICA)