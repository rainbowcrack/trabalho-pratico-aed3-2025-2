package br.com.mpet;

import br.com.mpet.model.*;
import br.com.mpet.persistence.dao.AnimalDataFileDao;
import br.com.mpet.persistence.dao.OngDataFileDao;
import br.com.mpet.persistence.dao.VoluntarioDataFileDao;
import br.com.mpet.persistence.dao.AdotanteDataFileDao;

import java.io.File;

public class Seed {
    private static final byte VERSAO = 1;

    public static void main(String[] args) throws Exception {
        File dataDir = resolveDataDir();
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            System.err.println("Falha ao criar diretório dats/");
            return;
        }

        File ongsDat = new File(dataDir, "ongs.dat");
        File animaisDat = new File(dataDir, "animais.dat");
    File ongsIdx = new File(dataDir, "ongs.dat.idx");
    File animaisIdx = new File(dataDir, "animais.dat.idx");

    File voluntariosDat = new File(dataDir, "voluntarios.dat");
    File voluntariosIdx = new File(dataDir, "voluntarios.dat.idx");
    File adotantesDat = new File(dataDir, "adotantes.dat");
    File adotantesIdx = new File(dataDir, "adotantes.dat.idx");

    // RESET: remove arquivos existentes para seed determinístico
    deleteQuiet(ongsDat); deleteQuiet(ongsIdx);
    deleteQuiet(animaisDat); deleteQuiet(animaisIdx);
    deleteQuiet(voluntariosDat); deleteQuiet(voluntariosIdx);
    deleteQuiet(adotantesDat); deleteQuiet(adotantesIdx);

       try (OngDataFileDao ongDao = new OngDataFileDao(ongsDat, VERSAO);
           AnimalDataFileDao animalDao = new AnimalDataFileDao(animaisDat, VERSAO);
           VoluntarioDataFileDao voluntarioDao = new VoluntarioDataFileDao(voluntariosDat, VERSAO);
           AdotanteDataFileDao adotanteDao = new AdotanteDataFileDao(adotantesDat, VERSAO)) {

            // 1) Criar 2 ONGs
            Ong ong1 = new Ong();
            ong1.setNome("ONG Amor Animal");
            ong1.setCnpj("11.111.111/0001-11");
            ong1.setEndereco("Rua das Flores, 123");
            ong1.setTelefone("(11) 99999-1111");
            ong1.setCpfResponsavel(null); // opcional
            ong1.setAtivo(true);
            ong1 = ongDao.create(ong1);

            Ong ong2 = new Ong();
            ong2.setNome("ONG Patinhas Felizes");
            ong2.setCnpj("22.222.222/0001-22");
            ong2.setEndereco("Av. dos Animais, 456");
            ong2.setTelefone("(11) 98888-2222");
            ong2.setCpfResponsavel(null);
            ong2.setAtivo(true);
            ong2 = ongDao.create(ong2);

            // 2) Criar voluntários e adotantes
            Voluntario vol1 = new Voluntario();
            vol1.setCpf("11111111111");
            vol1.setSenha("1111");
            vol1.setTelefone("(11) 90000-0001");
            vol1.setNome("Ana Voluntária");
            vol1.setEndereco("Rua A, 10");
            vol1.setIdOng(ong1.getId());
            vol1.setCargo(Role.ATENDIMENTO);
            vol1.setAtivo(true);
            voluntarioDao.create(vol1);

            Voluntario vol2 = new Voluntario();
            vol2.setCpf("22222222222");
            vol2.setSenha("2222");
            vol2.setTelefone("(11) 90000-0002");
            vol2.setNome("Bruno Voluntário");
            vol2.setEndereco("Rua B, 20");
            vol2.setIdOng(ong2.getId());
            vol2.setCargo(Role.LOGISTICA);
            vol2.setAtivo(true);
            voluntarioDao.create(vol2);

            // Define responsável por CPF na ONG 1
            ong1.setCpfResponsavel(vol1.getCpf());
            ongDao.update(ong1);

            // Adotantes
            Adotante ad1 = new Adotante();
            ad1.setCpf("33333333333");
            ad1.setSenha("3333");
            ad1.setTelefone("(11) 90000-0003");
            ad1.setAtivo(true);
            ad1.setNomeCompleto("Carlos Adotante");
            ad1.setTipoMoradia(TipoMoradia.APARTAMENTO);
            ad1.setPossuiTelaProtetora(true);
            ad1.setPossuiOutrosAnimais(false);
            ad1.setHorasForaDeCasa(8);
            ad1.setComposicaoFamiliar(ComposicaoFamiliar.PESSOA_SOZINHA);
            ad1.setMotivoAdocao("Companhia");
            adotanteDao.create(ad1);

            Adotante ad2 = new Adotante();
            ad2.setCpf("44444444444");
            ad2.setSenha("4444");
            ad2.setTelefone("(11) 90000-0004");
            ad2.setAtivo(true);
            ad2.setNomeCompleto("Daniela Adotante");
            ad2.setTipoMoradia(TipoMoradia.CASA_COM_QUINTAL_MURADO);
            ad2.setPossuiTelaProtetora(true);
            ad2.setPossuiOutrosAnimais(true);
            ad2.setDescOutrosAnimais("1 cachorro");
            ad2.setHorasForaDeCasa(6);
            ad2.setComposicaoFamiliar(ComposicaoFamiliar.FAMILIA_COM_CRIANCAS);
            ad2.setMotivoAdocao("Ama animais");
            adotanteDao.create(ad2);

            // 3) Criar 5 animais (2 na primeira ONG, 3 na segunda)
            // ONG 1
            Cachorro dog1 = new Cachorro();
            dog1.setIdOng(ong1.getId());
            dog1.setNome("Thor");
            dog1.setSexo('M');
            dog1.setPorte(Porte.MEDIO);
            dog1.setVacinado(true);
            dog1.setDescricao("Brincalhão e dócil");
            dog1.setRaca("SRD");
            dog1.setNivelAdestramento(NivelAdestramento.BASICO);
            dog1.setSeDaBemComCachorros(true);
            dog1.setSeDaBemComGatos(false);
            dog1.setSeDaBemComCriancas(true);
            animalDao.create(dog1);

            Gato cat1 = new Gato();
            cat1.setIdOng(ong1.getId());
            cat1.setNome("Mimi");
            cat1.setSexo('F');
            cat1.setPorte(Porte.PEQUENO);
            cat1.setVacinado(true);
            cat1.setDescricao("Calma e carinhosa");
            cat1.setRaca("SRD");
            cat1.setSeDaBemComCachorros(false);
            cat1.setSeDaBemComGatos(true);
            cat1.setSeDaBemComCriancas(true);
            cat1.setAcessoExterior(false);
            cat1.setPossuiTelamento(true);
            animalDao.create(cat1);

            // ONG 2
            Cachorro dog2 = new Cachorro();
            dog2.setIdOng(ong2.getId());
            dog2.setNome("Rex");
            dog2.setSexo('M');
            dog2.setPorte(Porte.GRANDE);
            dog2.setVacinado(true);
            dog2.setDescricao("Protetor e treinado");
            dog2.setRaca("Labrador");
            dog2.setNivelAdestramento(NivelAdestramento.AVANCADO);
            dog2.setSeDaBemComCachorros(true);
            dog2.setSeDaBemComGatos(true);
            dog2.setSeDaBemComCriancas(true);
            animalDao.create(dog2);

            Gato cat2 = new Gato();
            cat2.setIdOng(ong2.getId());
            cat2.setNome("Luna");
            cat2.setSexo('F');
            cat2.setPorte(Porte.PEQUENO);
            cat2.setVacinado(false);
            cat2.setDescricao("Observadora e independente");
            cat2.setRaca("Siames");
            cat2.setSeDaBemComCachorros(false);
            cat2.setSeDaBemComGatos(true);
            cat2.setSeDaBemComCriancas(false);
            cat2.setAcessoExterior(true);
            cat2.setPossuiTelamento(false);
            animalDao.create(cat2);

            Cachorro dog3 = new Cachorro();
            dog3.setIdOng(ong2.getId());
            dog3.setNome("Bidu");
            dog3.setSexo('M');
            dog3.setPorte(Porte.PEQUENO);
            dog3.setVacinado(false);
            dog3.setDescricao("Ativo e curioso");
            dog3.setRaca("Beagle");
            dog3.setNivelAdestramento(NivelAdestramento.NENHUM);
            dog3.setSeDaBemComCachorros(true);
            dog3.setSeDaBemComGatos(false);
            dog3.setSeDaBemComCriancas(true);
            animalDao.create(dog3);

            System.out.println("Seed concluído: 2 ONGs e 5 animais criados.");
            System.out.printf("ONG1 id=%d, ONG2 id=%d\n", ong1.getId(), ong2.getId());
        }
    }

    private static File resolveDataDir() {
        File wd = new File(System.getProperty("user.dir"));
        if (new File(wd, "Codigo").exists()) {
            return new File(wd, "dats");
        }
        if (wd.getName().equals("Codigo") && wd.getParentFile() != null) {
            return new File(wd.getParentFile(), "dats");
        }
        File cur = wd;
        for (int i = 0; i < 6 && cur != null; i++) {
            if (cur.getName().equals("Codigo")) {
                File root = cur.getParentFile();
                if (root != null) return new File(root, "dats");
            }
            cur = cur.getParentFile();
        }
        return new File(wd, "dats");
    }

    private static void deleteQuiet(File f) {
        try { if (f.exists()) f.delete(); } catch (Exception ignored) {}
    }
}
