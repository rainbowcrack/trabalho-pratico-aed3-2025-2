package br.com.mpet.persistence.dao;

import br.com.mpet.model.Adotante;

import java.io.File;
import java.io.IOException;

/** DAO concreto de Adotante persistindo em arquivo binário com índice B+. */
public class AdotanteDataFileDao extends UsuarioDataFileDao<Adotante> {
    public AdotanteDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato, Adotante.class);
    }
}
