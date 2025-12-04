package br.com.mpet.dto;

import br.com.mpet.model.Voluntario;

public class VoluntarioDto {
    public String cpf;
    public String nome;
    public String telefone;
    public int idOng;
    public String cargo;

    public static VoluntarioDto fromEntity(Voluntario v) {
        VoluntarioDto dto = new VoluntarioDto();
        dto.cpf = v.getCpf() != null ? v.getCpf() : "";
        dto.nome = v.getNome() != null ? v.getNome() : "";
        dto.telefone = v.getTelefone() != null ? v.getTelefone() : "";
        dto.idOng = v.getIdOng();
        dto.cargo = v.getCargo() != null ? v.getCargo().toString() : "";
        return dto;
    }
}