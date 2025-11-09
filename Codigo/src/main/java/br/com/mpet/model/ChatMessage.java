package br.com.mpet.model;

import java.time.LocalDateTime;

/** Mensagem em um chat. */
public class ChatMessage {
    int id;           // PK sequencial
    int threadId;     // FK para ChatThread
    ChatSender sender; // quem enviou
    String conteudo;  // texto
    LocalDateTime enviadoEm; // timestamp
    boolean ativo;     // espelha tombstone
    String zoneId;     // fuso horário do emissor no envio (ex.: "America/Sao_Paulo")

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getThreadId() { return threadId; }
    public void setThreadId(int threadId) { this.threadId = threadId; }
    public ChatSender getSender() { return sender; }
    public void setSender(ChatSender sender) { this.sender = sender; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
    public void setEnviadoEm(LocalDateTime enviadoEm) { this.enviadoEm = enviadoEm; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
}
