package br.com.mpet.dto;

import br.com.mpet.model.Animal;
import br.com.mpet.model.Cachorro;

/**
 * DTO para serialização JSON de Animal usando Gson.
 * Evita exposição direta dos campos internos da entidade.
 */
public class AnimalDto {
    public int id;
    public int idOng;
    public String nome;
    public String tipo;  // "CACHORRO" ou "GATO"
    public String porte;
    public String sexo;
    public boolean vacinado;
    public String descricao;
    public String imageUrl;

    public static AnimalDto fromEntity(Animal a) {
        AnimalDto dto = new AnimalDto();
        dto.id = a.getId();
        dto.idOng = a.getIdOng();
        dto.nome = a.getNome() != null ? a.getNome() : "";
        dto.tipo = a instanceof Cachorro ? "CACHORRO" : "GATO";
        dto.porte = a.getPorte() != null ? a.getPorte().toString() : "";
        dto.sexo = String.valueOf(a.getSexo());
        dto.vacinado = a.isVacinado();
        dto.descricao = a.getDescricao() != null ? a.getDescricao() : "";
        dto.imageUrl = a.getImageUrl() != null ? a.getImageUrl() : "";
        return dto;
    }
}