package br.com.mpet.model;

public class Voluntario extends Usuario {
    String nome;
    String endereco;
    int idOng; // associação principal (se futuramente for N:N trocar para coleção)
    Role cargo;

    // Getters/Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public int getIdOng() { return idOng; }
    public void setIdOng(int idOng) { this.idOng = idOng; }

    public Role getCargo() { return cargo; }
    public void setCargo(Role cargo) { this.cargo = cargo; }
}

// VOLUNTARIO PODE ADOTAR, LOGO SOFRE HERANÇA DE ADOTANTE
// MAS VOLUNTARIO NÃO PODE USAR A MESMA CONTA DE VOLUNTARIO PARA ADOTAR