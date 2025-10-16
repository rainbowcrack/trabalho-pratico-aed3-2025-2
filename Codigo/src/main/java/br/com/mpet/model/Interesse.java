package br.com.mpet.model;

import java.time.LocalDate;

/** Registro de interesse ("curtida") de um adotante por um animal. */
public class Interesse {
    int id;           // PK sequencial
    String cpfAdotante; // FK lógico
    int idAnimal;       // FK lógico
    LocalDate data;     // quando o interesse foi criado
    InteresseStatus status; // PENDENTE, APROVADO, REJEITADO
    boolean ativo;       // espelha tombstone

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCpfAdotante() { return cpfAdotante; }
    public void setCpfAdotante(String cpfAdotante) { this.cpfAdotante = cpfAdotante; }
    public int getIdAnimal() { return idAnimal; }
    public void setIdAnimal(int idAnimal) { this.idAnimal = idAnimal; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public InteresseStatus getStatus() { return status; }
    public void setStatus(InteresseStatus status) { this.status = status; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
