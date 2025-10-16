package br.com.mpet.model;

/**
 * Classe base abstrata para qualquer usuário do sistema (adotantes, voluntários, etc.).
 * Contém atributos comuns e credenciais básicas.
 */
public abstract class Usuario {

    // Identificação básica
    String telefone;
    boolean ativo;

    // Credenciais de login
    String cpf;
    String senha; // Observação: em produção, armazene hash/salt em vez de texto puro

    // Futuramente: perfis/roles comuns aqui, se necessário

    // Getters/Setters básicos para permitir persistência em DAOs
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
