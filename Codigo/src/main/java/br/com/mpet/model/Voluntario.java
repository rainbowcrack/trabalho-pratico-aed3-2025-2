package br.com.mpet.model;

public class Voluntario extends Usuario {
    String nome;
    String endereco;
    int idOng; // associação principal (se futuramente for N:N trocar para coleção)
    Role cargo;

    // Getters and Setters (a gerar futuramente)
}

// VOLUNTARIO PODE ADOTAR, LOGO SOFRE HERANÇA DE ADOTANTE
// MAS VOLUNTARIO NÃO PODE USAR A MESMA CONTA DE VOLUNTARIO PARA ADOTAR