package br.com.mpet.persistence.dao;

import br.com.mpet.model.Voluntario;

import java.io.File;
import java.io.IOException;

/** DAO concreto de Voluntario persistindo em arquivo binário com índice B+. */
public class VoluntarioDataFileDao extends UsuarioDataFileDao<Voluntario> {
    public VoluntarioDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato, Voluntario.class);
    }
}
