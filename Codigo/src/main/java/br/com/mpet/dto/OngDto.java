package br.com.mpet.dto;

import br.com.mpet.model.Ong;

public class OngDto {
    public int id;
    public String nome;
    public String cnpj;
    public String endereco;
    public String telefone;
    public boolean ativo;

    public static OngDto fromEntity(Ong o) {
        OngDto dto = new OngDto();
        dto.id = o.getId();
        dto.nome = o.getNome() != null ? o.getNome() : "";
        dto.cnpj = o.getCnpj() != null ? o.getCnpj() : "";
        dto.endereco = o.getEndereco() != null ? o.getEndereco() : "";
        dto.telefone = o.getTelefone() != null ? o.getTelefone() : "";
        dto.ativo = o.isAtivo();
        return dto;
    }
}