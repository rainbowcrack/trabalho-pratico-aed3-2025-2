package br.com.mpet.persistence.dao;

import br.com.mpet.model.Animal;
import br.com.mpet.persistence.BaseDataFile;
import br.com.mpet.persistence.CrudDao;

import java.io.File;
import java.io.IOException;

/**
 * DAO base para Animal (polimórfico), oferecendo acesso ao arquivo binário via {@link BaseDataFile}.
 * Implementações concretas devem chamar o construtor super com (file, versaoFormato).
 */
public abstract class AnimalDao extends BaseDataFile implements CrudDao<Animal, Integer> {

	protected AnimalDao(File file, byte versaoFormato) throws IOException {
		super(file, versaoFormato);
	}
}
