package br.com.mpet.persistence.dao;

import br.com.mpet.model.Animal;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;

import java.io.File;
import java.io.IOException;

/**
 * Abstração para DAOs da família de Animal.
 */
public abstract class AnimalDao extends BaseDataFile<Animal> implements CrudDao<Animal, Integer> {
    protected AnimalDao(File file, byte versaoFormato) throws IOException {
        super(file, versaoFormato);
    }
}
