package br.com.mpet.model;

import java.time.LocalDate;

public class Adotante extends Usuario {

    String nomeCompleto;
    LocalDate dataNascimento;
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

}