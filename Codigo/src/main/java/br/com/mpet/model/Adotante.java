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

    // Getters e Setters
    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public TipoMoradia getTipoMoradia() { return tipoMoradia; }
    public void setTipoMoradia(TipoMoradia tipoMoradia) { this.tipoMoradia = tipoMoradia; }

    public boolean isPossuiTelaProtetora() { return possuiTelaProtetora; }
    public void setPossuiTelaProtetora(boolean possuiTelaProtetora) { this.possuiTelaProtetora = possuiTelaProtetora; }

    public boolean isPossuiOutrosAnimais() { return possuiOutrosAnimais; }
    public void setPossuiOutrosAnimais(boolean possuiOutrosAnimais) { this.possuiOutrosAnimais = possuiOutrosAnimais; }

    public String getDescOutrosAnimais() { return descOutrosAnimais; }
    public void setDescOutrosAnimais(String descOutrosAnimais) { this.descOutrosAnimais = descOutrosAnimais; }

    public int getHorasForaDeCasa() { return horasForaDeCasa; }
    public void setHorasForaDeCasa(int horasForaDeCasa) { this.horasForaDeCasa = horasForaDeCasa; }

    public ComposicaoFamiliar getComposicaoFamiliar() { return composicaoFamiliar; }
    public void setComposicaoFamiliar(ComposicaoFamiliar composicaoFamiliar) { this.composicaoFamiliar = composicaoFamiliar; }

    public boolean isViagensFrequentes() { return viagensFrequentes; }
    public void setViagensFrequentes(boolean viagensFrequentes) { this.viagensFrequentes = viagensFrequentes; }

    public String getDescViagensFrequentes() { return descViagensFrequentes; }
    public void setDescViagensFrequentes(String descViagensFrequentes) { this.descViagensFrequentes = descViagensFrequentes; }

    public boolean isJaTevePets() { return jaTevePets; }
    public void setJaTevePets(boolean jaTevePets) { this.jaTevePets = jaTevePets; }

    public String getExperienciaComPets() { return experienciaComPets; }
    public void setExperienciaComPets(String experienciaComPets) { this.experienciaComPets = experienciaComPets; }

    public String getMotivoAdocao() { return motivoAdocao; }
    public void setMotivoAdocao(String motivoAdocao) { this.motivoAdocao = motivoAdocao; }

    public boolean isCientePossuiResponsavel() { return cientePossuiResponsavel; }
    public void setCientePossuiResponsavel(boolean cientePossuiResponsavel) { this.cientePossuiResponsavel = cientePossuiResponsavel; }

    public boolean isCienteCustos() { return cienteCustos; }
    public void setCienteCustos(boolean cienteCustos) { this.cienteCustos = cienteCustos; }
}