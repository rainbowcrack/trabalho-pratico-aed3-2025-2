package br.com.mpet.dto;

import br.com.mpet.model.Interesse;

public class InteresseDto {
    public int id;
    public String cpfAdotante;
    public int idAnimal;
    public String status;

    public static InteresseDto fromEntity(Interesse i) {
        InteresseDto dto = new InteresseDto();
        dto.id = i.getId();
        dto.cpfAdotante = i.getCpfAdotante() != null ? i.getCpfAdotante() : "";
        dto.idAnimal = i.getIdAnimal();
        dto.status = i.getStatus() != null ? i.getStatus().toString() : "";
        return dto;
    }
}