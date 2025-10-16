package br.com.mpet.persistence.dao;

import br.com.mpet.model.Voluntario;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class VoluntarioDataFileDao extends BaseDataFile<Voluntario> implements CrudDao<Voluntario, String> {
    public VoluntarioDataFileDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
    }
    @Override public Voluntario create(Voluntario entity) { return null; }
    @Override public Optional<Voluntario> read(String id) { return Optional.empty(); }
    @Override public boolean update(Voluntario entity) { return false; }
    @Override public boolean delete(String id) { return false; }
    @Override public List<Voluntario> listAllActive() { return List.of(); }
    @Override public void rebuildIfEmpty() {}
    @Override public void vacuum() {}
}
