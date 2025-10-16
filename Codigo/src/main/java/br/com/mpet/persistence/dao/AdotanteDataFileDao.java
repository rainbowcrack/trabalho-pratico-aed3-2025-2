package br.com.mpet.persistence.dao;

import br.com.mpet.model.Adotante;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AdotanteDataFileDao extends BaseDataFile<Adotante> implements CrudDao<Adotante, String> {
    public AdotanteDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
    }
    @Override public Adotante create(Adotante entity) { return null; }
    @Override public Optional<Adotante> read(String id) { return Optional.empty(); }
    @Override public boolean update(Adotante entity) { return false; }
    @Override public boolean delete(String id) { return false; }
    @Override public List<Adotante> listAllActive() { return List.of(); }
    @Override public void rebuildIfEmpty() {}
    @Override public void vacuum() {}
}
