package br.com.mpet.api;

import br.com.mpet.domain.*;
import br.com.mpet.repository.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Repositories implements AutoCloseable {
    public final VoluntarioAPI voluntario;
    public final AdotanteAPI adotante;
    public final OngAPI ong;
    public final AnimalAPI animal;

    private final VoluntarioRepository vRepo;
    private final AdotanteRepository aRepo;
    private final OngRepository oRepo;
    private final AnimalRepository anRepo;

    public Repositories(Path dataDir) throws IOException {
        this.vRepo = new VoluntarioRepository(dataDir);
        this.aRepo = new AdotanteRepository(dataDir);
        this.oRepo = new OngRepository(dataDir);
        this.anRepo = new AnimalRepository(dataDir);
        this.voluntario = new VoluntarioAPI();
        this.adotante = new AdotanteAPI();
        this.ong = new OngAPI();
        this.animal = new AnimalAPI();
    }

    public class VoluntarioAPI {
        public boolean create(Voluntario v) throws IOException { return vRepo.create(v); }
        public Optional<Voluntario> read(String cpf) throws IOException { return vRepo.readByCpf(cpf); }
        public boolean update(Voluntario v) throws IOException { return vRepo.update(v); }
        public boolean delete(String cpf) throws IOException { return vRepo.delete(cpf); }
        public List<Voluntario> searchByNomeFragment(String frag) throws IOException { return vRepo.searchByNomeFragment(frag); }
    }

    public class AdotanteAPI {
        public boolean create(Adotante a) throws IOException { return aRepo.create(a); }
        public Optional<Adotante> read(String cpf) throws IOException { return aRepo.readByCpf(cpf); }
        public boolean update(Adotante a) throws IOException { return aRepo.update(a); }
        public boolean delete(String cpf) throws IOException { return aRepo.delete(cpf); }
        public List<Adotante> searchByNomeFragment(String frag) throws IOException { return aRepo.searchByNomeFragment(frag); }
    }

    public class OngAPI {
        public boolean create(Ong o) throws IOException { return oRepo.create(o); }
        public Optional<Ong> read(String nome) throws IOException { return oRepo.readByNome(nome); }
        public boolean update(Ong o) throws IOException { return oRepo.update(o); }
        public boolean delete(String nome) throws IOException { return oRepo.delete(nome); }
        public Iterable<Ong> listAllOrdered() throws IOException { return oRepo.listOrdered(); }
    }

    public class AnimalAPI {
        public boolean create(Animal a) throws IOException { return anRepo.create(a); }
        public Optional<Animal> read(String id) throws IOException { return anRepo.readById(id); }
        public boolean update(Animal a) throws IOException { return anRepo.update(a); }
        public boolean delete(String id) throws IOException { return anRepo.delete(id); }
        public List<Animal> search(String nome, String especie, Boolean castrado) throws IOException { return anRepo.search(nome, especie, castrado); }
    }

    @Override public void close() throws IOException {
        vRepo.close(); aRepo.close(); oRepo.close(); anRepo.close();
    }

    // Maintenance: compact files and rebuild indexes for all repos
    public void vacuumAll() throws IOException {
        vRepo.vacuum();
        aRepo.vacuum();
        oRepo.vacuum();
        anRepo.vacuum();
    }
}
