package br.com.mpet;

import br.com.mpet.model.*;
import br.com.mpet.persistence.dao.AnimalDataFileDao;
import br.com.mpet.persistence.dao.OngDataFileDao;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
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
    private static final String ZIP_FILENAME = "backup.zip";

    private static final File ANIMAIS_DATA_FILE = new File(DATA_DIR, ANIMAIS_DATA_FILENAME);
    private static final File ANIMAIS_IDX_FILE = new File(DATA_DIR, ANIMAIS_IDX_FILENAME);
    private static final File ONGS_DATA_FILE = new File(DATA_DIR, ONGS_DATA_FILENAME);
    private static final File ONGS_IDX_FILE = new File(DATA_DIR, ONGS_IDX_FILENAME);
    private static final File ZIP_FILE = new File(DATA_DIR, ZIP_FILENAME);
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
        executarMenuPrincipal();
    }

    private static void executarMenuPrincipal() {
        try (
            Scanner sc = new Scanner(System.in);
            AnimalDataFileDao animalDao = new AnimalDataFileDao(ANIMAIS_DATA_FILE, VERSAO);
            OngDataFileDao ongDao = new OngDataFileDao(ONGS_DATA_FILE, VERSAO)
        ) {
            while (true) {
                System.out.println(ANSI_CYAN + ANSI_BOLD + "\n🐾 PetMatch - Menu Principal 🐾" + ANSI_RESET);
                System.out.println(ANSI_YELLOW + "---------------------------------" + ANSI_RESET);
                System.out.println("1) Gerenciar Animais");
                System.out.println("2) Gerenciar ONGs");
                System.out.println("3) Sistema (Backup/Restore/Vacuum)");
                System.out.println(ANSI_RED + "0) Sair" + ANSI_RESET);
                System.out.print("Escolha uma opção: ");

                String op = sc.nextLine().trim();
                switch (op) {
                    case "1" -> menuAnimais(sc, animalDao);
                    case "2" -> menuOngs(sc, ongDao);
                    case "3" -> menuSistema(sc, animalDao, ongDao);
                    case "0" -> {
                        System.out.println(ANSI_PURPLE + "Obrigado por usar o PetMatch! Até logo!" + ANSI_RESET);
                        return;
                    }
                    default -> System.out.println(ANSI_RED + "Opção inválida. Tente novamente." + ANSI_RESET);
                }
            }
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Ocorreu um erro fatal: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }

    // =================================================================================
    // MENU ANIMAIS
    // =================================================================================
    private static void menuAnimais(Scanner sc, AnimalDataFileDao dao) throws IOException {
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
                    case "1" -> criarAnimal(sc, dao);
                    case "2" -> lerAnimal(sc, dao);
                    case "3" -> listarAnimais(dao);
                    case "4" -> editarAnimal(sc, dao);
                    case "5" -> removerAnimal(sc, dao);
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "Erro: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void criarAnimal(Scanner sc, AnimalDataFileDao dao) throws IOException {
        System.out.print("Tipo (C=cachorro, G=gato): ");
        String t = sc.nextLine().trim().toUpperCase();
        Animal a;
        if (t.equals("C")) a = new Cachorro(); else if (t.equals("G")) a = new Gato(); else { System.out.println(ANSI_RED + "Tipo inválido." + ANSI_RESET); return; }

        preencherBasicoAnimal(sc, a);

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

    private static void editarAnimal(Scanner sc, AnimalDataFileDao dao) throws IOException {
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
    private static void menuOngs(Scanner sc, OngDataFileDao dao) throws IOException {
         while (true) {
            System.out.println(ANSI_CYAN + "\n--- Gerenciar ONGs ---" + ANSI_RESET);
            System.out.println("1) Criar ONG");
            System.out.println("2) Ler ONG por ID");
            System.out.println("3) Listar Todas as Ativas");
            System.out.println("4) Editar ONG");
            System.out.println("5) Remover ONG");
            System.out.println(ANSI_RED + "0) Voltar ao Menu Principal" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();

            try {
                switch (op) {
                    case "1" -> criarOng(sc, dao);
                    case "2" -> lerOng(sc, dao);
                    case "3" -> listarOngs(dao);
                    case "4" -> editarOng(sc, dao);
                    case "5" -> removerOng(sc, dao);
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception ex) {
                System.out.println(ANSI_RED + "Erro: " + ex.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void criarOng(Scanner sc, OngDataFileDao dao) throws IOException {
        System.out.println(ANSI_BLUE + "--- Cadastro de Nova ONG ---" + ANSI_RESET);
        Ong ong = new Ong();
        ong.setNome(perguntarString(sc, "Nome da ONG", null));
        ong.setCnpj(perguntarString(sc, "CNPJ", null));
        ong.setEndereco(perguntarString(sc, "Endereço", null));
        ong.setTelefone(perguntarString(sc, "Telefone", null));
        ong.setIdResponsavel(perguntarInt(sc, "ID do Voluntário Responsável"));
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

    private static void editarOng(Scanner sc, OngDataFileDao dao) throws IOException {
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
        ong.setIdResponsavel(perguntarInt(sc, "ID do Responsável", ong.getIdResponsavel()));

        boolean ok = dao.update(ong);
        System.out.println(ok ? ANSI_GREEN + "ONG atualizada com sucesso." + ANSI_RESET : ANSI_RED + "Falha ao atualizar." + ANSI_RESET);
    }

    private static void removerOng(Scanner sc, OngDataFileDao dao) throws IOException {
        int id = perguntarInt(sc, "ID da ONG a remover: ");
        boolean ok = dao.delete(id);
        System.out.println(ok ? ANSI_GREEN + "ONG removida com sucesso (tombstone)." + ANSI_RESET : ANSI_YELLOW + "ONG não encontrada." + ANSI_RESET);
    }


    // =================================================================================
    // MENU SISTEMA
    // =================================================================================
    private static void menuSistema(Scanner sc, AnimalDataFileDao animalDao, OngDataFileDao ongDao) {
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
                            restoreZip();
                            System.out.println(ANSI_GREEN + "Restauração concluída. Por favor, reinicie o programa para carregar os novos dados." + ANSI_RESET);
                            System.exit(0);
                        }
                    }
                    case "3" -> {
                        System.out.println(ANSI_YELLOW + "Iniciando compactação (vacuum)..." + ANSI_RESET);
                        animalDao.vacuum();
                        ongDao.vacuum();
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
    private static void preencherBasicoAnimal(Scanner sc, Animal a) {
        a.setAtivo(true);
        a.setNome(perguntarString(sc, "Nome", null));
        a.setIdOng(perguntarInt(sc, "ID da ONG"));
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
        System.out.printf("  > Endereço: %s, Telefone: %s, Responsável ID: %d\n",
                ong.getEndereco(), ong.getTelefone(), ong.getIdResponsavel());
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
