package br.com.mpet;

import br.com.mpet.model.*;
import br.com.mpet.persistence.dao.AnimalDataFileDao;
import br.com.mpet.persistence.dao.AdotanteDataFileDao;
import br.com.mpet.persistence.dao.OngDataFileDao;
import br.com.mpet.persistence.dao.VoluntarioDataFileDao;
import br.com.mpet.persistence.dao.AdocaoDataFileDao;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Interface de linha de comando (CLI) aprimorada para operar o sistema PetMatch.
 *
 * Funcionalidades:
 * - Menus organizados para gerenciar Animais, ONGs e o Sistema.
 * - CRUD completo para todas as entidades.
 * - Visual com cores para melhor legibilidade.
 * - Backup/Restore em ZIP para todos os arquivos de dados (.dat) e índices (.idx).
 * - Compactação (vacuum) para otimizar os arquivos de dados.
 */
public class Interface {

    // --- Constantes de Arquivos ---
    private static final File DATA_DIR = new File("dats");
    private static final String ANIMAIS_DATA_FILENAME = "animais.dat";
    private static final String ANIMAIS_IDX_FILENAME = "animais.dat.idx";
    private static final String ONGS_DATA_FILENAME = "ongs.dat";
    private static final String ONGS_IDX_FILENAME = "ongs.dat.idx";
    private static final String ADOTANTES_DATA_FILENAME = "adotantes.dat";
    private static final String ADOTANTES_IDX_FILENAME = "adotantes.dat.idx";
    private static final String VOLUNTARIOS_DATA_FILENAME = "voluntarios.dat";
    private static final String VOLUNTARIOS_IDX_FILENAME = "voluntarios.dat.idx";
    private static final String ADOCOES_DATA_FILENAME = "adocoes.dat";
    private static final String ZIP_FILENAME = "backup.zip";

    private static final File ANIMAIS_DATA_FILE = new File(DATA_DIR, ANIMAIS_DATA_FILENAME);
    private static final File ANIMAIS_IDX_FILE = new File(DATA_DIR, ANIMAIS_IDX_FILENAME);
    private static final File ONGS_DATA_FILE = new File(DATA_DIR, ONGS_DATA_FILENAME);
    private static final File ONGS_IDX_FILE = new File(DATA_DIR, ONGS_IDX_FILENAME);
    private static final File ZIP_FILE = new File(DATA_DIR, ZIP_FILENAME);
    private static final File ADOTANTES_DATA_FILE = new File(DATA_DIR, ADOTANTES_DATA_FILENAME);
    private static final File ADOTANTES_IDX_FILE = new File(DATA_DIR, ADOTANTES_IDX_FILENAME);
    private static final File VOLUNTARIOS_DATA_FILE = new File(DATA_DIR, VOLUNTARIOS_DATA_FILENAME);
    private static final File VOLUNTARIOS_IDX_FILE = new File(DATA_DIR, VOLUNTARIOS_IDX_FILENAME);
    private static final File ADOCOES_DATA_FILE = new File(DATA_DIR, ADOCOES_DATA_FILENAME);
    private static final byte VERSAO = 1;

    // --- Cores ANSI para o Console ---
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BOLD = "\u001B[1m";


    public static void main(String[] args) {
        if (!DATA_DIR.exists() && !DATA_DIR.mkdirs()) {
            System.out.println(ANSI_RED + "Falha ao criar diretório de dados." + ANSI_RESET);
            return;
        }
        try (
            Scanner sc = new Scanner(System.in);
            AnimalDataFileDao animalDao = new AnimalDataFileDao(ANIMAIS_DATA_FILE, VERSAO);
            OngDataFileDao ongDao = new OngDataFileDao(ONGS_DATA_FILE, VERSAO);
            AdotanteDataFileDao adotanteDao = new AdotanteDataFileDao(ADOTANTES_DATA_FILE, VERSAO);
            VoluntarioDataFileDao voluntarioDao = new VoluntarioDataFileDao(VOLUNTARIOS_DATA_FILE, VERSAO);
            AdocaoDataFileDao adocaoDao = new AdocaoDataFileDao(ADOCOES_DATA_FILE, VERSAO)
        ) {
            while (true) {
                UsuarioLogado login = telaLogin(sc, adotanteDao, voluntarioDao);
                if (login == null) {
                    System.out.println(ANSI_PURPLE + "Até logo!" + ANSI_RESET);
                    return;
                }
                switch (login.tipo) {
                    case ADMIN -> menuAdmin(sc, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao);
                    case ADOTANTE -> menuAdotanteLogado(sc, adotanteDao, animalDao, (Adotante) login.usuario);
                    case VOLUNTARIO -> menuVoluntarioLogado(sc, voluntarioDao, animalDao, (Voluntario) login.usuario);
                }
            }
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Ocorreu um erro fatal: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }

    private enum TipoSessao { ADMIN, ADOTANTE, VOLUNTARIO }
    private record UsuarioLogado(TipoSessao tipo, Usuario usuario) {}

    private static UsuarioLogado telaLogin(Scanner sc, AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao) throws IOException {
        System.out.println("\n" + ANSI_BOLD + ANSI_CYAN + "══════════════════════════════════════════════" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_CYAN + "            🐾 PetMatch - Login 🐾           " + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_CYAN + "══════════════════════════════════════════════" + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "Dica: Admin = (admin / admin). Demais: use CPF e senha cadastrados." + ANSI_RESET);
        mostrarLoginsDisponiveis(adotanteDao, voluntarioDao);
        System.out.print("Usuário: ");
        String usuario = sc.nextLine().trim();
        if (usuario.equals("0")) return null;
        System.out.print("Senha: ");
        String senha = sc.nextLine().trim();

        // Admin padrão
        if ("admin".equalsIgnoreCase(usuario) && "admin".equals(senha)) {
            System.out.println(ANSI_GREEN + "Bem-vindo, administrador!" + ANSI_RESET);
            return new UsuarioLogado(TipoSessao.ADMIN, null);
        }

        // Autenticação por CPF
        Optional<Adotante> a = adotanteDao.read(usuario);
        if (a.isPresent() && a.get().isAtivo() && Objects.equals(a.get().getSenha(), senha)) {
            System.out.println(ANSI_GREEN + "Login como Adotante bem-sucedido." + ANSI_RESET);
            return new UsuarioLogado(TipoSessao.ADOTANTE, a.get());
        }
        Optional<Voluntario> v = voluntarioDao.read(usuario);
        if (v.isPresent() && v.get().isAtivo() && Objects.equals(v.get().getSenha(), senha)) {
            System.out.println(ANSI_GREEN + "Login como Voluntário bem-sucedido." + ANSI_RESET);
            return new UsuarioLogado(TipoSessao.VOLUNTARIO, v.get());
        }

        System.out.println(ANSI_RED + "Usuário não encontrado ou senha incorreta. (Digite 0 como usuário para sair)" + ANSI_RESET);
        return telaLogin(sc, adotanteDao, voluntarioDao);
    }

    private static void mostrarLoginsDisponiveis(AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao) {
        try {
            List<Adotante> adotantes = adotanteDao.listAllActive();
            List<Voluntario> voluntarios = voluntarioDao.listAllActive();
            System.out.println(ANSI_CYAN + "→ Logins de exemplo:" + ANSI_RESET);
            System.out.println("  Admin: admin / admin");
            if (!adotantes.isEmpty()) {
                System.out.println("  Adotantes (CPF / senha):");
                adotantes.stream().limit(5).forEach(a ->
                        System.out.printf("    - %s / %s (%s)\n", a.getCpf(), a.getSenha(), a.getNomeCompleto()));
            }
            if (!voluntarios.isEmpty()) {
                System.out.println("  Voluntários (CPF / senha):");
                voluntarios.stream().limit(5).forEach(v ->
                        System.out.printf("    - %s / %s (%s)\n", v.getCpf(), v.getSenha(), v.getNome()));
            }
        } catch (Exception e) {
            // silencioso
        }
    }

    private static void menuAdmin(Scanner sc, AnimalDataFileDao animalDao, OngDataFileDao ongDao, AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao, AdocaoDataFileDao adocaoDao) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + ANSI_BOLD + "\n🐾 PetMatch - Painel do Admin 🐾" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "---------------------------------" + ANSI_RESET);
            System.out.println("1) Gerenciar Animais");
            System.out.println("2) Gerenciar ONGs");
            System.out.println("3) Gerenciar Adotantes");
            System.out.println("4) Gerenciar Voluntários");
            System.out.println("6) Gerenciar Adoções (Adotante -> Animal)");
            System.out.println("5) Sistema (Backup/Restore/Vacuum)");
            System.out.println(ANSI_RED + "0) Logout" + ANSI_RESET);
            System.out.print("Escolha uma opção: ");
            String op = sc.nextLine().trim();
                switch (op) {
                case "1" -> menuAnimais(sc, animalDao, ongDao);
                case "2" -> menuOngs(sc, ongDao, voluntarioDao);
                case "3" -> menuAdotantes(sc, adotanteDao);
                case "4" -> menuVoluntarios(sc, voluntarioDao);
                case "6" -> menuAdocoes(sc, adocaoDao, adotanteDao, animalDao);
                case "5" -> menuSistema(sc, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao);
                case "0" -> { return; }
                default -> System.out.println(ANSI_RED + "Opção inválida. Tente novamente." + ANSI_RESET);
            }
        }
    }

    private static void menuAdocoes(Scanner sc, AdocaoDataFileDao adocaoDao, AdotanteDataFileDao adotanteDao, AnimalDataFileDao animalDao) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Gerenciar Adoções ---" + ANSI_RESET);
            System.out.println("1) Registrar adoção (adotante -> animal)");
            System.out.println("2) Listar todas adoções");
            System.out.println("3) Remover adoção por ID");
            System.out.println("4) Listar animais por CPF do adotante");
            System.out.println(ANSI_RED + "0) Voltar" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            switch (op) {
                case "1" -> registrarAdocao(sc, adocaoDao, adotanteDao, animalDao);
                case "2" -> listarAdocoes(adocaoDao);
                case "3" -> removerAdocao(sc, adocaoDao);
                case "4" -> listarAnimaisPorAdotante(sc, adocaoDao, animalDao);
                case "0" -> { return; }
                default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
            }
        }
    }

    private static void registrarAdocao(Scanner sc, AdocaoDataFileDao adocaoDao, AdotanteDataFileDao adotanteDao, AnimalDataFileDao animalDao) throws IOException {
        // Escolher adotante por CPF
        List<Adotante> adotantes = adotanteDao.listAllActive();
        if (adotantes.isEmpty()) { System.out.println(ANSI_YELLOW + "Sem adotantes." + ANSI_RESET); return; }
        adotantes.forEach(a -> System.out.printf(" - %s (%s)\n", a.getCpf(), a.getNomeCompleto()));
        String cpf = perguntarString(sc, "CPF do adotante", null);
        Optional<Adotante> optA = adotanteDao.read(cpf);
        if (optA.isEmpty()) { System.out.println(ANSI_RED + "CPF inválido." + ANSI_RESET); return; }

        // Escolher animal por ID (somente ativos)
        List<Animal> animais = animalDao.listAllActive();
        if (animais.isEmpty()) { System.out.println(ANSI_YELLOW + "Sem animais." + ANSI_RESET); return; }
        animais.forEach(Interface::imprimirAnimal);
        int idAnimal = perguntarInt(sc, "ID do animal");
        Optional<Animal> optAn = animalDao.read(idAnimal);
        if (optAn.isEmpty()) { System.out.println(ANSI_RED + "Animal inválido." + ANSI_RESET); return; }

        br.com.mpet.model.Adocao ad = new br.com.mpet.model.Adocao();
        ad.setCpfAdotante(cpf);
        ad.setIdAnimal(idAnimal);
        ad.setDataAdocao(java.time.LocalDate.now());
        ad.setAtivo(true);
        adocaoDao.create(ad);
        System.out.println(ANSI_GREEN + "Adoção registrada." + ANSI_RESET);
    }

    private static void listarAdocoes(AdocaoDataFileDao adocaoDao) throws IOException {
        List<br.com.mpet.model.Adocao> list = adocaoDao.listAllActive();
        if (list.isEmpty()) { System.out.println(ANSI_YELLOW + "Sem adoções." + ANSI_RESET); return; }
        list.forEach(a -> System.out.printf("[ADOCAO] id=%d, cpf=%s, animalId=%d, data=%s\n", a.getId(), a.getCpfAdotante(), a.getIdAnimal(), a.getDataAdocao()));
    }

    private static void removerAdocao(Scanner sc, AdocaoDataFileDao adocaoDao) throws IOException {
        int id = perguntarInt(sc, "ID da adoção");
        if (adocaoDao.delete(id)) System.out.println(ANSI_GREEN + "Adoção removida." + ANSI_RESET);
        else System.out.println(ANSI_YELLOW + "Não encontrada." + ANSI_RESET);
    }

    private static void listarAnimaisPorAdotante(Scanner sc, AdocaoDataFileDao adocaoDao, AnimalDataFileDao animalDao) throws IOException {
        String cpf = perguntarString(sc, "CPF do adotante", null);
        List<br.com.mpet.model.Adocao> list = adocaoDao.listAllActive();
        List<Integer> ids = list.stream().filter(a -> cpf.equals(a.getCpfAdotante())).map(br.com.mpet.model.Adocao::getIdAnimal).toList();
        if (ids.isEmpty()) { System.out.println(ANSI_YELLOW + "Sem adoções para este CPF." + ANSI_RESET); return; }
        for (Integer id : ids) animalDao.read(id).ifPresent(Interface::imprimirAnimal);
    }

    // =================================================================================
    // MENU ANIMAIS
    // =================================================================================
    private static void menuAnimais(Scanner sc, AnimalDataFileDao dao, OngDataFileDao ongDao) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Gerenciar Animais ---" + ANSI_RESET);
            System.out.println("1) Criar Animal (Cachorro/Gato)");
            System.out.println("2) Ler Animal por ID");
            System.out.println("3) Listar Todos os Ativos");
            System.out.println("4) Editar Animal");
            System.out.println("5) Remover Animal");
            System.out.println(ANSI_RED + "0) Voltar ao Menu Principal" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();

            try {
                switch (op) {
                    case "1" -> criarAnimal(sc, dao, ongDao);
                    case "2" -> lerAnimal(sc, dao);
                    case "3" -> listarAnimais(dao);
                    case "4" -> editarAnimal(sc, dao, ongDao);
                    case "5" -> removerAnimal(sc, dao);
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "Erro: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void criarAnimal(Scanner sc, AnimalDataFileDao dao, OngDataFileDao ongDao) throws IOException {
        System.out.print("Tipo (C=cachorro, G=gato): ");
        String t = sc.nextLine().trim().toUpperCase();
        Animal a;
        if (t.equals("C")) a = new Cachorro(); else if (t.equals("G")) a = new Gato(); else { System.out.println(ANSI_RED + "Tipo inválido." + ANSI_RESET); return; }

        Integer idOng = escolherOng(sc, ongDao);
        if (idOng == null) { System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET); return; }

        preencherBasicoAnimal(sc, a, idOng);

        if (a instanceof Cachorro c) {
            System.out.print("Raça: "); c.setRaca(sc.nextLine().trim());
            c.setNivelAdestramento(perguntarEnum(sc, "Nível de adestramento (NENHUM/BASICO/AVANCADO): ", NivelAdestramento.class, NivelAdestramento.NENHUM));
            c.setSeDaBemComCachorros(perguntarBool(sc, "Se dá bem com cachorros? (s/n): "));
            c.setSeDaBemComGatos(perguntarBool(sc, "Se dá bem com gatos? (s/n): "));
            c.setSeDaBemComCriancas(perguntarBool(sc, "Se dá bem com crianças? (s/n): "));
        } else if (a instanceof Gato g) {
            System.out.print("Raça: "); g.setRaca(sc.nextLine().trim());
            g.setSeDaBemComCachorros(perguntarBool(sc, "Se dá bem com cachorros? (s/n): "));
            g.setSeDaBemComGatos(perguntarBool(sc, "Se dá bem com gatos? (s/n): "));
            g.setSeDaBemComCriancas(perguntarBool(sc, "Se dá bem com crianças? (s/n): "));
            g.setAcessoExterior(perguntarBool(sc, "Tem acesso ao exterior? (s/n): "));
            g.setPossuiTelamento(perguntarBool(sc, "Possui telamento? (s/n): "));
        }

        Animal salvo = dao.create(a);
        System.out.println(ANSI_GREEN + "Animal criado com sucesso! ID: " + salvo.getId() + ANSI_RESET);
    }

    private static void lerAnimal(Scanner sc, AnimalDataFileDao dao) throws IOException {
        int id = perguntarInt(sc, "ID do animal: ");
        Optional<Animal> opt = dao.read(id);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "Animal não encontrado." + ANSI_RESET); return; }
        imprimirAnimal(opt.get());
    }

    private static void listarAnimais(AnimalDataFileDao dao) throws IOException {
        List<Animal> todos = dao.listAllActive();
        System.out.println(ANSI_CYAN + "\n--- Lista de Animais Ativos ---" + ANSI_RESET);
        if (todos.isEmpty()) { System.out.println(ANSI_YELLOW + "Nenhum animal cadastrado." + ANSI_RESET); return; }
        todos.forEach(Interface::imprimirAnimal);
        System.out.println(ANSI_YELLOW + "---------------------------------" + ANSI_RESET);
    }

    private static void editarAnimal(Scanner sc, AnimalDataFileDao dao, OngDataFileDao ongDao) throws IOException {
        int id = perguntarInt(sc, "ID do animal a editar: ");
        Optional<Animal> opt = dao.read(id);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "Animal não encontrado." + ANSI_RESET); return; }
        Animal a = opt.get();
        System.out.println(ANSI_BLUE + "Editando o seguinte animal:" + ANSI_RESET);
        imprimirAnimal(a);

        System.out.println(ANSI_BLUE + "Digite os novos valores (ou pressione Enter para manter o atual):" + ANSI_RESET);
        a.setNome(perguntarString(sc, "Nome", a.getNome()));
        a.setDescricao(perguntarString(sc, "Descrição", a.getDescricao()));

        if (a instanceof Cachorro c) {
            c.setRaca(perguntarString(sc, "Raça", c.getRaca()));
        } else if (a instanceof Gato g) {
            g.setRaca(perguntarString(sc, "Raça", g.getRaca()));
        }

        if (perguntarBool(sc, "Deseja trocar a ONG? (s/n): ")) {
            Integer novoIdOng = escolherOng(sc, ongDao);
            if (novoIdOng != null) a.setIdOng(novoIdOng);
        }

        boolean ok = dao.update(a);
        System.out.println(ok ? ANSI_GREEN + "Atualizado com sucesso." + ANSI_RESET : ANSI_RED + "Falha ao atualizar." + ANSI_RESET);
    }

    private static void removerAnimal(Scanner sc, AnimalDataFileDao dao) throws IOException {
        int id = perguntarInt(sc, "ID do animal a remover: ");
        boolean ok = dao.delete(id);
        System.out.println(ok ? ANSI_GREEN + "Removido com sucesso (tombstone)." + ANSI_RESET : ANSI_YELLOW + "Animal não encontrado." + ANSI_RESET);
    }

    // =================================================================================
    // MENU ONGS
    // =================================================================================
    private static void menuOngs(Scanner sc, OngDataFileDao dao, VoluntarioDataFileDao voluntarioDao) throws IOException {
         while (true) {
            System.out.println(ANSI_CYAN + "\n--- Gerenciar ONGs ---" + ANSI_RESET);
            System.out.println("1) Criar ONG");
            System.out.println("2) Ler ONG por ID");
            System.out.println("3) Listar Todas as Ativas");
            System.out.println("4) Editar ONG");
            System.out.println("5) Remover ONG");
        System.out.println("6) Listar animais por ONG");
            System.out.println(ANSI_RED + "0) Voltar ao Menu Principal" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();

            try {
                switch (op) {
                    case "1" -> criarOng(sc, dao, voluntarioDao);
                    case "2" -> lerOng(sc, dao);
                    case "3" -> listarOngs(dao);
                    case "4" -> editarOng(sc, dao, voluntarioDao);
                    case "5" -> removerOng(sc, dao);
                    case "6" -> listarAnimaisPorOng(sc);
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "Erro: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void listarAnimaisPorOng(Scanner sc) throws IOException {
        try (AnimalDataFileDao animalDao = new AnimalDataFileDao(ANIMAIS_DATA_FILE, VERSAO)) {
            int id = perguntarInt(sc, "ID da ONG");
            List<Animal> todos = animalDao.listAllActive();
            todos.stream().filter(a -> a.getIdOng() == id).forEach(Interface::imprimirAnimal);
        }
    }

    private static void criarOng(Scanner sc, OngDataFileDao dao, VoluntarioDataFileDao voluntarioDao) throws IOException {
        System.out.println(ANSI_BLUE + "--- Cadastro de Nova ONG ---" + ANSI_RESET);
        Ong ong = new Ong();
        ong.setNome(perguntarString(sc, "Nome da ONG", null));
        ong.setCnpj(perguntarString(sc, "CNPJ", null));
        ong.setEndereco(perguntarString(sc, "Endereço", null));
        ong.setTelefone(perguntarString(sc, "Telefone", null));
        // Responsável agora por CPF; admin escolhe de uma lista (opcional)
        ong.setCpfResponsavel(escolherVoluntarioCpf(sc, voluntarioDao));
        ong.setAtivo(true);

        Ong salva = dao.create(ong);
        System.out.println(ANSI_GREEN + "ONG criada com sucesso! ID: " + salva.getId() + ANSI_RESET);
    }

    private static void lerOng(Scanner sc, OngDataFileDao dao) throws IOException {
        int id = perguntarInt(sc, "ID da ONG: ");
        Optional<Ong> opt = dao.read(id);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "ONG não encontrada." + ANSI_RESET); return; }
        imprimirOng(opt.get());
    }

    private static void listarOngs(OngDataFileDao dao) throws IOException {
        List<Ong> todas = dao.listAllActive();
        System.out.println(ANSI_CYAN + "\n--- Lista de ONGs Ativas ---" + ANSI_RESET);
        if (todas.isEmpty()) { System.out.println(ANSI_YELLOW + "Nenhuma ONG cadastrada." + ANSI_RESET); return; }
        todas.forEach(Interface::imprimirOng);
        System.out.println(ANSI_YELLOW + "----------------------------" + ANSI_RESET);
    }

    private static void editarOng(Scanner sc, OngDataFileDao dao, VoluntarioDataFileDao voluntarioDao) throws IOException {
        int id = perguntarInt(sc, "ID da ONG a editar: ");
        Optional<Ong> opt = dao.read(id);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "ONG não encontrada." + ANSI_RESET); return; }
        Ong ong = opt.get();
        System.out.println(ANSI_BLUE + "Editando a seguinte ONG:" + ANSI_RESET);
        imprimirOng(ong);

        System.out.println(ANSI_BLUE + "Digite os novos valores (ou pressione Enter para manter o atual):" + ANSI_RESET);
        ong.setNome(perguntarString(sc, "Nome", ong.getNome()));
        ong.setCnpj(perguntarString(sc, "CNPJ", ong.getCnpj()));
        ong.setEndereco(perguntarString(sc, "Endereço", ong.getEndereco()));
        ong.setTelefone(perguntarString(sc, "Telefone", ong.getTelefone()));
        if (perguntarBool(sc, "Trocar responsável? (s/n): ")) {
            ong.setCpfResponsavel(escolherVoluntarioCpf(sc, voluntarioDao));
        }

        boolean ok = dao.update(ong);
        System.out.println(ok ? ANSI_GREEN + "ONG atualizada com sucesso." + ANSI_RESET : ANSI_RED + "Falha ao atualizar." + ANSI_RESET);
    }

    private static void removerOng(Scanner sc, OngDataFileDao dao) throws IOException {
        int id = perguntarInt(sc, "ID da ONG a remover: ");
        boolean ok = dao.delete(id);
        System.out.println(ok ? ANSI_GREEN + "ONG removida com sucesso (tombstone)." + ANSI_RESET : ANSI_YELLOW + "ONG não encontrada." + ANSI_RESET);
    }

    // =================================================================================
    // MENU ADOTANTES E VOLUNTÁRIOS
    // =================================================================================
    private static void menuAdotantes(Scanner sc, AdotanteDataFileDao dao) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Gerenciar Adotantes ---" + ANSI_RESET);
            System.out.println("1) Criar Adotante");
            System.out.println("2) Ler por CPF");
            System.out.println("3) Listar Todos");
            System.out.println("4) Editar Adotante");
            System.out.println("5) Remover Adotante");
            System.out.println(ANSI_RED + "0) Voltar" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            try {
                switch (op) {
                    case "1" -> criarAdotante(sc, dao);
                    case "2" -> lerAdotante(sc, dao);
                    case "3" -> listarAdotantes(dao);
                    case "4" -> editarAdotante(sc, dao);
                    case "5" -> removerAdotante(sc, dao);
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "Erro: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void menuVoluntarios(Scanner sc, VoluntarioDataFileDao dao) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Gerenciar Voluntários ---" + ANSI_RESET);
            System.out.println("1) Criar Voluntário");
            System.out.println("2) Ler por CPF");
            System.out.println("3) Listar Todos");
            System.out.println("4) Editar Voluntário");
            System.out.println("5) Remover Voluntário");
            System.out.println(ANSI_RED + "0) Voltar" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            try {
                switch (op) {
                    case "1" -> criarVoluntario(sc, dao);
                    case "2" -> lerVoluntario(sc, dao);
                    case "3" -> listarVoluntarios(dao);
                    case "4" -> editarVoluntario(sc, dao);
                    case "5" -> removerVoluntario(sc, dao);
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "Erro: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    // =================================================================================
    // PAINÉIS DE USUÁRIOS LOGADOS
    // =================================================================================
    private static void menuAdotanteLogado(Scanner sc, AdotanteDataFileDao adotanteDao, AnimalDataFileDao animalDao, Adotante a) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Painel do Adotante ---" + ANSI_RESET);
            System.out.println("1) Ver meus dados");
            System.out.println("2) Editar meus dados básicos");
            System.out.println("3) Listar animais disponíveis");
            System.out.println(ANSI_RED + "0) Logout" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            switch (op) {
                case "1" -> imprimirAdotante(a);
                case "2" -> {
                    a.setTelefone(perguntarString(sc, "Telefone", a.getTelefone()));
                    a.setSenha(perguntarString(sc, "Senha", a.getSenha()));
                    adotanteDao.update(a);
                    System.out.println(ANSI_GREEN + "Dados atualizados." + ANSI_RESET);
                }
                case "3" -> listarAnimais(animalDao);
                case "0" -> { return; }
                default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
            }
        }
    }

    private static void menuVoluntarioLogado(Scanner sc, VoluntarioDataFileDao voluntarioDao, AnimalDataFileDao animalDao, Voluntario v) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Painel do Voluntário ---" + ANSI_RESET);
            System.out.println("1) Ver meus dados");
            System.out.println("2) Editar meus dados básicos");
            System.out.println("3) Listar animais da ONG " + v.getIdOng());
            System.out.println(ANSI_RED + "0) Logout" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            switch (op) {
                case "1" -> imprimirVoluntario(v);
                case "2" -> {
                    v.setTelefone(perguntarString(sc, "Telefone", v.getTelefone()));
                    v.setSenha(perguntarString(sc, "Senha", v.getSenha()));
                    voluntarioDao.update(v);
                    System.out.println(ANSI_GREEN + "Dados atualizados." + ANSI_RESET);
                }
                case "3" -> {
                    List<Animal> todos = animalDao.listAllActive();
                    todos.stream().filter(an -> an.getIdOng() == v.getIdOng()).forEach(Interface::imprimirAnimal);
                }
                case "0" -> { return; }
                default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
            }
        }
    }

    private static void criarAdotante(Scanner sc, AdotanteDataFileDao dao) throws IOException {
        Adotante a = new Adotante();
        preencherBasicoUsuario(sc, a);
        a.setNomeCompleto(perguntarString(sc, "Nome completo", null));
        a.setDataNascimento(perguntarData(sc, "Data nascimento (yyyy-mm-dd) ou enter"));
        a.setTipoMoradia(perguntarEnum(sc, "Tipo Moradia (CASA_COM_QUINTAL_MURADO/CASA_SEM_QUINTAL/APARTAMENTO)", TipoMoradia.class, TipoMoradia.APARTAMENTO));
        a.setPossuiTelaProtetora(perguntarBool(sc, "Possui tela protetora? (s/n): "));
        a.setPossuiOutrosAnimais(perguntarBool(sc, "Possui outros animais? (s/n): "));
        a.setDescOutrosAnimais(perguntarString(sc, "Descrição outros animais", null));
        a.setHorasForaDeCasa(perguntarInt(sc, "Horas fora de casa/dia", 8));
        a.setComposicaoFamiliar(perguntarEnum(sc, "Composição Familiar (PESSOA_SOZINHA/CASAL_SEM_FILHOS/FAMILIA_COM_CRIANCAS)", ComposicaoFamiliar.class, ComposicaoFamiliar.PESSOA_SOZINHA));
        a.setViagensFrequentes(perguntarBool(sc, "Viagens frequentes? (s/n): "));
        a.setDescViagensFrequentes(perguntarString(sc, "Descrição viagens (opcional)", null));
        a.setJaTevePets(perguntarBool(sc, "Já teve pets? (s/n): "));
        a.setExperienciaComPets(perguntarString(sc, "Experiência com pets (opcional)", null));
        a.setMotivoAdocao(perguntarString(sc, "Motivo da adoção", null));
        a.setCientePossuiResponsavel(perguntarBool(sc, "Ciente que precisa de responsável? (s/n): "));
        a.setCienteCustos(perguntarBool(sc, "Ciente dos custos? (s/n): "));
        dao.create(a);
        System.out.println(ANSI_GREEN + "Adotante criado." + ANSI_RESET);
    }

    private static void lerAdotante(Scanner sc, AdotanteDataFileDao dao) throws IOException {
        String cpf = perguntarString(sc, "CPF (somente números)", null);
        Optional<Adotante> opt = dao.read(cpf);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "Não encontrado." + ANSI_RESET); return; }
        imprimirAdotante(opt.get());
    }

    private static void listarAdotantes(AdotanteDataFileDao dao) throws IOException {
        List<Adotante> list = dao.listAllActive();
        if (list.isEmpty()) { System.out.println(ANSI_YELLOW + "Nenhum adotante." + ANSI_RESET); return; }
        list.forEach(Interface::imprimirAdotante);
    }

    private static void editarAdotante(Scanner sc, AdotanteDataFileDao dao) throws IOException {
        String cpf = perguntarString(sc, "CPF do adotante", null);
        Optional<Adotante> opt = dao.read(cpf);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "Não encontrado." + ANSI_RESET); return; }
        Adotante a = opt.get();
        a.setTelefone(perguntarString(sc, "Telefone", a.getTelefone()));
        a.setSenha(perguntarString(sc, "Senha", a.getSenha()));
        a.setNomeCompleto(perguntarString(sc, "Nome completo", a.getNomeCompleto()));
        a.setMotivoAdocao(perguntarString(sc, "Motivo da adoção", a.getMotivoAdocao()));
        dao.update(a);
        System.out.println(ANSI_GREEN + "Atualizado." + ANSI_RESET);
    }

    private static void removerAdotante(Scanner sc, AdotanteDataFileDao dao) throws IOException {
        String cpf = perguntarString(sc, "CPF do adotante", null);
        if (dao.delete(cpf)) System.out.println(ANSI_GREEN + "Removido." + ANSI_RESET);
        else System.out.println(ANSI_YELLOW + "Não encontrado." + ANSI_RESET);
    }

    private static void criarVoluntario(Scanner sc, VoluntarioDataFileDao dao) throws IOException {
        Voluntario v = new Voluntario();
        preencherBasicoUsuario(sc, v);
        v.setNome(perguntarString(sc, "Nome", null));
        v.setEndereco(perguntarString(sc, "Endereço", null));
        // Seleciona ONG existente por lista (se disponível)
        try (OngDataFileDao ongDao = new OngDataFileDao(ONGS_DATA_FILE, VERSAO)) {
            Integer idOng = escolherOng(sc, ongDao);
            v.setIdOng(idOng == null ? 0 : idOng);
        }
        v.setCargo(perguntarEnum(sc, "Cargo (TRIAGEM/LOGISTICA/ATENDIMENTO/VETERINARIO/ADMIN)", Role.class, Role.ATENDIMENTO));
        dao.create(v);
        System.out.println(ANSI_GREEN + "Voluntário criado." + ANSI_RESET);
    }

    private static void lerVoluntario(Scanner sc, VoluntarioDataFileDao dao) throws IOException {
        String cpf = perguntarString(sc, "CPF (somente números)", null);
        Optional<Voluntario> opt = dao.read(cpf);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "Não encontrado." + ANSI_RESET); return; }
        imprimirVoluntario(opt.get());
    }

    private static void listarVoluntarios(VoluntarioDataFileDao dao) throws IOException {
        List<Voluntario> list = dao.listAllActive();
        if (list.isEmpty()) { System.out.println(ANSI_YELLOW + "Nenhum voluntário." + ANSI_RESET); return; }
        list.forEach(Interface::imprimirVoluntario);
    }

    private static void editarVoluntario(Scanner sc, VoluntarioDataFileDao dao) throws IOException {
        String cpf = perguntarString(sc, "CPF do voluntário", null);
        Optional<Voluntario> opt = dao.read(cpf);
        if (opt.isEmpty()) { System.out.println(ANSI_YELLOW + "Não encontrado." + ANSI_RESET); return; }
        Voluntario v = opt.get();
        v.setTelefone(perguntarString(sc, "Telefone", v.getTelefone()));
        v.setSenha(perguntarString(sc, "Senha", v.getSenha()));
        v.setNome(perguntarString(sc, "Nome", v.getNome()));
        v.setEndereco(perguntarString(sc, "Endereço", v.getEndereco()));
        // permitir trocar ONG
        try (OngDataFileDao ongDao = new OngDataFileDao(ONGS_DATA_FILE, VERSAO)) {
            if (perguntarBool(sc, "Trocar ONG? (s/n): ")) {
                Integer idOng = escolherOng(sc, ongDao);
                if (idOng != null) v.setIdOng(idOng);
            }
        }
        v.setCargo(perguntarEnum(sc, "Cargo", Role.class, v.getCargo()==null?Role.ATENDIMENTO:v.getCargo()));
        dao.update(v);
        System.out.println(ANSI_GREEN + "Atualizado." + ANSI_RESET);
    }

    private static void removerVoluntario(Scanner sc, VoluntarioDataFileDao dao) throws IOException {
        String cpf = perguntarString(sc, "CPF do voluntário", null);
        if (dao.delete(cpf)) System.out.println(ANSI_GREEN + "Removido." + ANSI_RESET);
        else System.out.println(ANSI_YELLOW + "Não encontrado." + ANSI_RESET);
    }


    // =================================================================================
    // MENU SISTEMA
    // =================================================================================
    private static void menuSistema(Scanner sc, AnimalDataFileDao animalDao, OngDataFileDao ongDao, AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao, AdocaoDataFileDao adocaoDao) {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Sistema ---" + ANSI_RESET);
            System.out.println("1) Fazer Backup (ZIP)");
            System.out.println("2) Restaurar Backup (ZIP)");
            System.out.println("3) Compactar Arquivos (Vacuum)");
            System.out.println(ANSI_RED + "0) Voltar ao Menu Principal" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();

            try {
                switch (op) {
                    case "1" -> backupZip();
                    case "2" -> {
                        System.out.println(ANSI_YELLOW + "ATENÇÃO: Esta ação sobrescreverá os dados atuais." + ANSI_RESET);
                        if (perguntarBool(sc, "Deseja continuar? (s/n): ")) {
                            animalDao.close();
                            ongDao.close();
                            adotanteDao.close();
                            voluntarioDao.close();
                            adocaoDao.close();
                            restoreZip();
                            System.out.println(ANSI_GREEN + "Restauração concluída. Por favor, reinicie o programa para carregar os novos dados." + ANSI_RESET);
                            System.exit(0);
                        }
                    }
                    case "3" -> {
                        System.out.println(ANSI_YELLOW + "Iniciando compactação (vacuum)..." + ANSI_RESET);
                        animalDao.vacuum();
                        ongDao.vacuum();
                        adotanteDao.vacuum();
                        voluntarioDao.vacuum();
                        adocaoDao.vacuum();
                        System.out.println(ANSI_GREEN + "Compactação concluída. É recomendado reiniciar o programa." + ANSI_RESET);
                    }
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "Erro: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    // =================================================================================
    // HELPERS DE ENTRADA DE DADOS
    // =================================================================================
    private static void preencherBasicoAnimal(Scanner sc, Animal a, int idOng) {
        a.setAtivo(true);
        a.setNome(perguntarString(sc, "Nome", null));
        a.setIdOng(idOng);
        a.setPorte(perguntarEnum(sc, "Porte (PEQUENO/MEDIO/GRANDE)", Porte.class, Porte.MEDIO));
        a.setSexo(perguntarChar(sc, "Sexo (M/F/U)", 'U'));
        a.setDataNascimentoAprox(perguntarData(sc, "Data de nascimento aprox (yyyy-mm-dd) ou enter"));
        a.setVacinado(perguntarBool(sc, "Já foi vacinado? (s/n): "));
        a.setDescricao(perguntarString(sc, "Condição de saúde (descrição, opcional)", null));
    }

    private static boolean perguntarBool(Scanner sc, String prompt) {
        System.out.print(prompt);
        String s = sc.nextLine().trim().toLowerCase();
        return s.startsWith("s") || s.equals("1") || s.equals("y");
    }

    private static String perguntarString(Scanner sc, String prompt, String padrao) {
        System.out.print(prompt + (padrao != null ? " [" + padrao + "]" : "") + ": ");
        String valor = sc.nextLine().trim();
        return valor.isEmpty() ? padrao : valor;
    }

    private static int perguntarInt(Scanner sc, String prompt) {
        while (true) {
            try {
                System.out.print(prompt + ": ");
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Valor inválido. Digite um número inteiro." + ANSI_RESET);
            }
        }
    }
    
    private static int perguntarInt(Scanner sc, String prompt, int padrao) {
        System.out.print(prompt + " [" + padrao + "]: ");
        String valor = sc.nextLine().trim();
        if (valor.isEmpty()) return padrao;
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "Valor inválido, usando padrão." + ANSI_RESET);
            return padrao;
        }
    }

    private static char perguntarChar(Scanner sc, String prompt, char padrao) {
        System.out.print(prompt + " [" + padrao + "]: ");
        String valor = sc.nextLine().trim().toUpperCase();
        return valor.isEmpty() ? padrao : valor.charAt(0);
    }

    private static LocalDate perguntarData(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String valor = sc.nextLine().trim();
            if (valor.isEmpty()) return null;
            try {
                return LocalDate.parse(valor);
            } catch (DateTimeParseException e) {
                System.out.println(ANSI_RED + "Formato de data inválido. Use yyyy-mm-dd." + ANSI_RESET);
            }
        }
    }

    private static <T extends Enum<T>> T perguntarEnum(Scanner sc, String prompt, Class<T> enumClass, T padrao) {
        System.out.print(prompt + " [" + padrao.name() + "]: ");
        String v = sc.nextLine().trim().toUpperCase();
        if (v.isEmpty()) return padrao;
        try {
            return Enum.valueOf(enumClass, v);
        } catch (IllegalArgumentException e) {
            System.out.println(ANSI_RED + "Valor inválido, usando padrão." + ANSI_RESET);
            return padrao;
        }
    }

    // =================================================================================
    // HELPERS DE IMPRESSÃO
    // =================================================================================
    private static void imprimirAnimal(Animal a) {
        String tipo = a.getClass().getSimpleName();
        String corTipo = tipo.equals("Cachorro") ? ANSI_BLUE : ANSI_PURPLE;

        String base = String.format(ANSI_BOLD + "[%s%s%s] ID=%d, Nome=%s, ONG=%d, Porte=%s, Sexo=%s, Vacinado=%s, Ativo=%s" + ANSI_RESET,
            corTipo, tipo, ANSI_RESET, a.getId(), a.getNome(), a.getIdOng(), a.getPorte(), a.getSexo(), a.isVacinado(), a.isAtivo());
        System.out.println(base);

        if (a instanceof Cachorro c) {
            System.out.printf("  > Raça: %s, Adestramento: %s, Socializa (Cães/Gatos/Crianças): %s/%s/%s\n",
                    c.getRaca(), c.getNivelAdestramento(), c.isSeDaBemComCachorros(), c.isSeDaBemComGatos(), c.isSeDaBemComCriancas());
        } else if (a instanceof Gato g) {
            System.out.printf("  > Raça: %s, Socializa (Cães/Gatos/Crianças): %s/%s/%s, Acesso Exterior: %s, Telamento: %s\n",
                    g.getRaca(), g.isSeDaBemComCachorros(), g.isSeDaBemComGatos(), g.isSeDaBemComCriancas(), g.isAcessoExterior(), g.isPossuiTelamento());
        }
        if(a.getDescricao() != null && !a.getDescricao().isBlank()) {
            System.out.println("  > Descrição: " + a.getDescricao());
        }
    }

    private static void imprimirOng(Ong ong) {
        System.out.printf(ANSI_BOLD + "[ONG] ID=%d, Nome=%s, CNPJ=%s, Ativo=%s\n" + ANSI_RESET,
                ong.getId(), ong.getNome(), ong.getCnpj(), ong.isAtivo());
        System.out.printf("  > Endereço: %s, Telefone: %s, Responsável CPF: %s\n",
                ong.getEndereco(), ong.getTelefone(), ong.getCpfResponsavel());
    }

    // Seleção de ONG existente para vincular animais e voluntários
    private static Integer escolherOng(Scanner sc, OngDataFileDao ongDao) throws IOException {
        List<Ong> ongs = ongDao.listAllActive();
        if (ongs.isEmpty()) {
            System.out.println(ANSI_YELLOW + "Não há ONGs cadastradas." + ANSI_RESET);
            return null;
        }
        System.out.println(ANSI_CYAN + "ONGs disponíveis:" + ANSI_RESET);
        ongs.forEach(o -> System.out.printf(" - ID=%d | %s\n", o.getId(), o.getNome()));
        int id = perguntarInt(sc, "Escolha o ID da ONG");
        Optional<Ong> opt = ongDao.read(id);
        if (opt.isEmpty() || !opt.get().isAtivo()) {
            System.out.println(ANSI_RED + "ONG inválida." + ANSI_RESET);
            return null;
        }
        return id;
    }

    private static String escolherVoluntarioCpf(Scanner sc, VoluntarioDataFileDao voluntarioDao) throws IOException {
        List<Voluntario> vols = voluntarioDao.listAllActive();
        if (vols.isEmpty()) {
            System.out.println(ANSI_YELLOW + "Não há voluntários cadastrados." + ANSI_RESET);
            return null;
        }
        System.out.println(ANSI_CYAN + "Voluntários disponíveis (CPF - Nome):" + ANSI_RESET);
        vols.forEach(v -> System.out.printf(" - %s - %s\n", v.getCpf(), v.getNome()));
        String cpf = perguntarString(sc, "Informe o CPF do responsável (vazio para nenhum)", null);
        if (cpf == null || cpf.isBlank()) return null;
        Optional<Voluntario> v = voluntarioDao.read(cpf);
        if (v.isEmpty() || !v.get().isAtivo()) {
            System.out.println(ANSI_RED + "CPF inválido." + ANSI_RESET);
            return null;
        }
        return cpf;
    }

    private static void preencherBasicoUsuario(Scanner sc, Usuario u) {
    u.setAtivo(true);
    u.setCpf(perguntarString(sc, "CPF (somente números)", null));
    u.setSenha(perguntarString(sc, "Senha", null));
    u.setTelefone(perguntarString(sc, "Telefone", null));
    }

    private static void imprimirAdotante(Adotante a) {
    System.out.printf(ANSI_BOLD + "[ADOTANTE] CPF=%s, Nome=%s, Ativo=%s\n" + ANSI_RESET,
        a.getCpf(), a.getNomeCompleto(), a.isAtivo());
    System.out.printf("  > Tel: %s, Senha: %s, Moradia: %s, Outros animais: %s, Motivo: %s\n",
        a.getTelefone(), a.getSenha(), a.getTipoMoradia(), a.isPossuiOutrosAnimais(), a.getMotivoAdocao());
    }

    private static void imprimirVoluntario(Voluntario v) {
    System.out.printf(ANSI_BOLD + "[VOLUNTÁRIO] CPF=%s, Nome=%s, ONG=%d, Cargo=%s, Ativo=%s\n" + ANSI_RESET,
        v.getCpf(), v.getNome(), v.getIdOng(), v.getCargo(), v.isAtivo());
    System.out.printf("  > Tel: %s, Senha: %s, Endereço: %s\n", v.getTelefone(), v.getSenha(), v.getEndereco());
    }

    // =================================================================================
    // BACKUP/RESTORE ZIP
    // =================================================================================
    private static void backupZip() throws IOException {
        System.out.println(ANSI_BLUE + "Iniciando backup..." + ANSI_RESET);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(ZIP_FILE))) {
            zipOne(zos, ANIMAIS_DATA_FILE, ANIMAIS_DATA_FILENAME);
            zipOne(zos, ANIMAIS_IDX_FILE, ANIMAIS_IDX_FILENAME);
            zipOne(zos, ONGS_DATA_FILE, ONGS_DATA_FILENAME);
            zipOne(zos, ONGS_IDX_FILE, ONGS_IDX_FILENAME);
            zipOne(zos, ADOTANTES_DATA_FILE, ADOTANTES_DATA_FILENAME);
            zipOne(zos, ADOTANTES_IDX_FILE, ADOTANTES_IDX_FILENAME);
            zipOne(zos, VOLUNTARIOS_DATA_FILE, VOLUNTARIOS_DATA_FILENAME);
            zipOne(zos, VOLUNTARIOS_IDX_FILE, VOLUNTARIOS_IDX_FILENAME);
            zipOne(zos, ADOCOES_DATA_FILE, ADOCOES_DATA_FILENAME);
        }
        System.out.println(ANSI_GREEN + "Backup gerado com sucesso em: " + ZIP_FILE.getAbsolutePath() + ANSI_RESET);
        listZipContents(ZIP_FILE);
    }

    private static void restoreZip() throws IOException {
        if (!ZIP_FILE.exists()) {
            System.out.println(ANSI_RED + "Arquivo de backup '" + ZIP_FILE.getPath() + "' não encontrado." + ANSI_RESET);
            return;
        }
        System.out.println(ANSI_BLUE + "Restaurando arquivos de: " + ZIP_FILE.getPath() + ANSI_RESET);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(ZIP_FILE))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File out = switch (e.getName()) {
                    case ANIMAIS_DATA_FILENAME -> ANIMAIS_DATA_FILE;
                    case ANIMAIS_IDX_FILENAME -> ANIMAIS_IDX_FILE;
                    case ONGS_DATA_FILENAME -> ONGS_DATA_FILE;
                    case ONGS_IDX_FILENAME -> ONGS_IDX_FILE;
                    case ADOTANTES_DATA_FILENAME -> ADOTANTES_DATA_FILE;
                    case ADOTANTES_IDX_FILENAME -> ADOTANTES_IDX_FILE;
                    case VOLUNTARIOS_DATA_FILENAME -> VOLUNTARIOS_DATA_FILE;
                    case VOLUNTARIOS_IDX_FILENAME -> VOLUNTARIOS_IDX_FILE;
                    case ADOCOES_DATA_FILENAME -> ADOCOES_DATA_FILE;
                    default -> null;
                };
                if (out != null) {
                    System.out.println("  -> Restaurando " + out.getName());
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        zis.transferTo(fos);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static void zipOne(ZipOutputStream zos, File file, String entryName) throws IOException {
        if (!file.exists()) {
            System.out.println(ANSI_YELLOW + "Aviso: Arquivo '" + file.getName() + "' não encontrado para backup." + ANSI_RESET);
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            fis.transferTo(zos);
            zos.closeEntry();
            System.out.println("  -> Adicionado ao backup: " + entryName);
        }
    }

    private static void listZipContents(File zip) throws IOException {
        if (!zip.exists()) return;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            System.out.println(ANSI_CYAN + "Conteúdo de " + zip.getName() + ":" + ANSI_RESET);
            ZipEntry e;
            int count = 0;
            while ((e = zis.getNextEntry()) != null) {
                System.out.printf(" - %s (%d bytes)\n", e.getName(), e.getSize());
                count++;
                zis.closeEntry();
            }
            if (count == 0) System.out.println("(vazio)");
        }
    }
}
