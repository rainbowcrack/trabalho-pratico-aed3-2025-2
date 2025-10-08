package br.com.mpet.model;

public class Voluntario {
    String nome;
    String cpf; // chave primária (natural)
    String telefone;
    String endereco;
    int idOng; // associação principal (se futuramente for N:N trocar para coleção)
    Role cargo;
    boolean ativo; // espelha lápide

    // Getters and Setters (a gerar futuramente)
}

// VOLUNTARIO PODE ADOTAR, LOGO SOFRE HERANÇA DE ADOTANTE