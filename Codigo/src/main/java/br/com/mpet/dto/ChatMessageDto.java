package br.com.mpet.dto;

import br.com.mpet.model.ChatMessage;

public class ChatMessageDto {
    public int id;
    public int threadId;
    public String sender;
    public String conteudo;
    public long enviadoEm;

    public static ChatMessageDto fromEntity(ChatMessage msg) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.id = msg.getId();
        dto.threadId = msg.getThreadId();
        dto.sender = msg.getSender() != null ? msg.getSender().toString() : "UNKNOWN";
        dto.conteudo = msg.getConteudo() != null ? msg.getConteudo() : "";
        
        // Converter LocalDateTime para timestamp (epoch millis)
        if (msg.getEnviadoEm() != null) {
            dto.enviadoEm = msg.getEnviadoEm().atZone(java.time.ZoneId.of("America/Sao_Paulo")).toInstant().toEpochMilli();
        } else {
            dto.enviadoEm = 0;
        }
        
        return dto;
    }
}