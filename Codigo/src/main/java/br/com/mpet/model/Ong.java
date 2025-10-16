package br.com.mpet.model;

public class Ong {
    int id; // chave primária sequencial
    String nome;
    String cnpj;
    String endereco;
    String telefone;
    String cpfResponsavel; // CPF do voluntário responsável
    boolean ativo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCpfResponsavel() { return cpfResponsavel; }
    public void setCpfResponsavel(String cpfResponsavel) { this.cpfResponsavel = cpfResponsavel; }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
