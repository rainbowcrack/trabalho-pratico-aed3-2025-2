package br.com.mpet.model;

import java.time.LocalDate;

public class Adotante {

    String cpf; // chave primária (natural)
    String nomeCompleto;
    LocalDate dataNascimento;
    String telefone;
    TipoMoradia tipoMoradia;
    boolean possuiTelaProtetora;
    boolean possuiOutrosAnimais;
    String descOutrosAnimais;
    int horasForaDeCasa;
    ComposicaoFamiliar composicaoFamiliar;
    boolean viagensFrequentes;
    String descViagensFrequentes;
    boolean jaTevePets;
    String experienciaComPets;
    String motivoAdocao; // renomeado (camelCase)
    boolean cientePossuiResponsavel;
    boolean cienteCustos;
    boolean ativo; // espelha lápide

}