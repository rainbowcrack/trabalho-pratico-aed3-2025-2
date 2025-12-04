package br.com.mpet.dto;

import br.com.mpet.model.Adocao;

public class AdocaoDto {
    public int id;
    public String cpfAdotante;
    public int idAnimal;
    public String dataAdocao;

    public static AdocaoDto fromEntity(Adocao a) {
        AdocaoDto dto = new AdocaoDto();
        dto.id = a.getId();
        dto.cpfAdotante = a.getCpfAdotante() != null ? a.getCpfAdotante() : "";
        dto.idAnimal = a.getIdAnimal();
        dto.dataAdocao = a.getDataAdocao() != null ? a.getDataAdocao().toString() : "";
        return dto;
    }
}