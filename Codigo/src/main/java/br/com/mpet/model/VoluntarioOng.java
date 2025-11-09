package br.com.mpet.model;

import java.time.LocalDate;

/** Relação N:N entre Voluntário e ONG. */
public class VoluntarioOng {
    int id;                 // PK sequencial
    String cpfVoluntario;   // FK lógico para Voluntario (Usuario.cpf)
    int idOng;              // FK lógico para Ong.id
    LocalDate dataEntrada;  // quando começou a atuar na ONG
    boolean ativo;          // espelha tombstone

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCpfVoluntario() { return cpfVoluntario; }
    public void setCpfVoluntario(String cpfVoluntario) { this.cpfVoluntario = cpfVoluntario; }
    public int getIdOng() { return idOng; }
    public void setIdOng(int idOng) { this.idOng = idOng; }
    public LocalDate getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDate dataEntrada) { this.dataEntrada = dataEntrada; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
