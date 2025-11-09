package br.com.mpet.model;

import java.time.LocalDateTime;

/** Thread de chat entre um voluntário da ONG e um adotante sobre um animal específico. */
public class ChatThread {
    int id;           // PK sequencial
    int idAnimal;     // animal relacionado
    String cpfAdotante; // adotante participante
    boolean aberto;   // se o chat está ativo/aberto
    LocalDateTime criadoEm; // quando foi criado
    String zoneId;    // fuso horário em que foi criado (ex.: "America/Sao_Paulo")

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdAnimal() { return idAnimal; }
    public void setIdAnimal(int idAnimal) { this.idAnimal = idAnimal; }
    public String getCpfAdotante() { return cpfAdotante; }
    public void setCpfAdotante(String cpfAdotante) { this.cpfAdotante = cpfAdotante; }
    public boolean isAberto() { return aberto; }
    public void setAberto(boolean aberto) { this.aberto = aberto; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
}
