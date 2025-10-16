package br.com.mpet.model;

import java.time.LocalDate;

/**
 * Relação de adoção: 1 Adotante -> N Animais, sem alterar o payload de Animal.
 */
public class Adocao {
    int id; // PK sequencial
    String cpfAdotante; // chave do adotante
    int idAnimal; // FK para Animal
    LocalDate dataAdocao; // data da adoção
    boolean ativo; // espelha tombstone

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCpfAdotante() { return cpfAdotante; }
    public void setCpfAdotante(String cpfAdotante) { this.cpfAdotante = cpfAdotante; }
    public int getIdAnimal() { return idAnimal; }
    public void setIdAnimal(int idAnimal) { this.idAnimal = idAnimal; }
    public LocalDate getDataAdocao() { return dataAdocao; }
    public void setDataAdocao(LocalDate dataAdocao) { this.dataAdocao = dataAdocao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
