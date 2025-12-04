package br.com.mpet.dto;

import br.com.mpet.model.Adotante;

public class AdotanteDto {
    public String cpf;
    public String nome;
    public String telefone;

    public static AdotanteDto fromEntity(Adotante a) {
        AdotanteDto dto = new AdotanteDto();
        dto.cpf = a.getCpf() != null ? a.getCpf() : "";
        dto.nome = a.getNomeCompleto() != null ? a.getNomeCompleto() : "";
        dto.telefone = a.getTelefone() != null ? a.getTelefone() : "";
        return dto;
    }
}