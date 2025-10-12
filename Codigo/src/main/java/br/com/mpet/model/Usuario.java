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
}
