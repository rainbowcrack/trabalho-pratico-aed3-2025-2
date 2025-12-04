package br.com.mpet.dto;

import br.com.mpet.model.ChatThread;

public class ChatThreadDto {
    public int id;
    public int idAnimal;
    public String cpfAdotante;
    public boolean aberto;
    public long criadoEm;

    public static ChatThreadDto fromEntity(ChatThread ct) {
        ChatThreadDto dto = new ChatThreadDto();
        dto.id = ct.getId();
        dto.idAnimal = ct.getIdAnimal();
        dto.cpfAdotante = ct.getCpfAdotante() != null ? ct.getCpfAdotante() : "";
        dto.aberto = ct.isAberto();
        
        // Converter LocalDateTime para timestamp (epoch millis)
        if (ct.getCriadoEm() != null) {
            dto.criadoEm = ct.getCriadoEm().atZone(java.time.ZoneId.of("America/Sao_Paulo")).toInstant().toEpochMilli();
        } else {
            dto.criadoEm = 0;
        }
        
        return dto;
    }
}