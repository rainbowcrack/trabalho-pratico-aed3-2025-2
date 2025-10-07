package br.com.mpet.persistence.dao;

import br.com.mpet.model.Animal;
import br.com.mpet.persistence.CrudDao;

/**
 * DAO para Animal (polimórfico). A implementação concreta de serialização
 * binária virá nas próximas etapas.
 */
public abstract class AnimalDao implements CrudDao<Animal, Integer> {
}
