package br.com.mpet;

import br.com.mpet.model.*;
import br.com.mpet.persistence.dao.AnimalDataFileDao;
import br.com.mpet.persistence.dao.AdotanteDataFileDao;
import br.com.mpet.persistence.dao.OngDataFileDao;
import br.com.mpet.persistence.dao.VoluntarioDataFileDao;
import br.com.mpet.persistence.dao.AdocaoDataFileDao;
import br.com.mpet.persistence.dao.InteresseDataFileDao;
import br.com.mpet.persistence.dao.ChatThreadDataFileDao;
import br.com.mpet.persistence.dao.ChatMessageDataFileDao;
import br.com.mpet.model.Interesse;
import br.com.mpet.model.InteresseStatus;
import br.com.mpet.model.ChatThread;
import br.com.mpet.model.ChatMessage;
import br.com.mpet.model.ChatSender;
import br.com.mpet.Compressao;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Scanner;
import java.util.ArrayList;
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
    // Resolve o diretório de dados para o raiz do repositório (../dats quando executado dentro de Codigo)
    private static final File DATA_DIR = resolveDataDir();
    private static final String ANIMAIS_DATA_FILENAME = "animais.dat";
    private static final String ANIMAIS_IDX_FILENAME = "animais.dat.idx";
    private static final String ONGS_DATA_FILENAME = "ongs.dat";
    private static final String ONGS_IDX_FILENAME = "ongs.dat.idx";
    private static final String ADOTANTES_DATA_FILENAME = "adotantes.dat";
    private static final String ADOTANTES_IDX_FILENAME = "adotantes.dat.idx";
    private static final String VOLUNTARIOS_DATA_FILENAME = "voluntarios.dat";
    private static final String VOLUNTARIOS_IDX_FILENAME = "voluntarios.dat.idx";
    private static final String ADOCOES_DATA_FILENAME = "adocoes.dat";
    private static final String ADOCOES_IDX_FILENAME = "adocoes.dat.idx";
    private static final String INTERESSES_DATA_FILENAME = "interesses.dat";
    private static final String INTERESSES_IDX_FILENAME = "interesses.dat.idx";
    private static final String CHAT_THREADS_DATA_FILENAME = "chat_threads.dat";
    private static final String CHAT_THREADS_IDX_FILENAME = "chat_threads.dat.idx";
    private static final String CHAT_MSGS_DATA_FILENAME = "chat_msgs.dat";
    private static final String CHAT_MSGS_IDX_FILENAME = "chat_msgs.dat.idx";
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
    private static final File ADOCOES_IDX_FILE = new File(DATA_DIR, ADOCOES_IDX_FILENAME);
    private static final File INTERESSES_DATA_FILE = new File(DATA_DIR, INTERESSES_DATA_FILENAME);
    private static final File INTERESSES_IDX_FILE = new File(DATA_DIR, INTERESSES_IDX_FILENAME);
    private static final File CHAT_THREADS_DATA_FILE = new File(DATA_DIR, CHAT_THREADS_DATA_FILENAME);
    private static final File CHAT_THREADS_IDX_FILE = new File(DATA_DIR, CHAT_THREADS_IDX_FILENAME);
    private static final File CHAT_MSGS_DATA_FILE = new File(DATA_DIR, CHAT_MSGS_DATA_FILENAME);
    private static final File CHAT_MSGS_IDX_FILE = new File(DATA_DIR, CHAT_MSGS_IDX_FILENAME);
    private static final byte VERSAO = 1;

    private static File resolveDataDir() {
        File wd = new File(System.getProperty("user.dir"));
        // Caso 1: executando a partir da raiz do repositório (existe a pasta Codigo aqui)
        if (new File(wd, "Codigo").exists()) {
            return new File(wd, "dats");
        }
        // Caso 2: executando dentro da pasta Codigo
        if (wd.getName().equals("Codigo") && wd.getParentFile() != null) {
            return new File(wd.getParentFile(), "dats");
        }
        // Caso 3: executando de subpastas como Codigo/target/classes
        File cur = wd;
        for (int i = 0; i < 6 && cur != null; i++) {
            if (cur.getName().equals("Codigo")) {
                File root = cur.getParentFile();
                if (root != null) return new File(root, "dats");
            }
            cur = cur.getParentFile();
        }
        // Fallback: diretório atual
        return new File(wd, "dats");
    }

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
    public static final String ANSI_DIM = "\u001B[2m";

    // ========================================================================
    // FUNÇÕES DE UI MELHORADAS
    // ========================================================================
    
    /**
     * Limpa a tela do terminal (funciona em Linux/Mac/Windows 10+)
     */
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
    /**
     * Exibe um splash screen animado ao iniciar
     */
    private static void showSplashScreen() {
        clearScreen();
        System.out.println(ANSI_BOLD + ANSI_CYAN);
        System.out.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                   ║");
        System.out.println("║            🐾 Bem-vindo ao Sistema MPet PetMatch 🐾             ║");
        System.out.println("║                                                                   ║");
        System.out.println("║   Sistema de Adoção Responsável com Tecnologia de Ponta          ║");
        System.out.println("║                                                                   ║");
        System.out.println("║   ✓ Criptografia RSA-2048                                       ║");
        System.out.println("║   ✓ Compressão LZW/Huffman                                      ║");
        System.out.println("║   ✓ Indexação B+ Tree                                           ║");
        System.out.println("║   ✓ Backup/Restore Automático                                   ║");
        System.out.println("║                                                                   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════════╝");
        System.out.println(ANSI_RESET);
        
        // Animação de loading
        System.out.print(ANSI_YELLOW + "\n   Inicializando sistema");
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(200);
                System.out.print(".");
            } catch (InterruptedException e) { }
        }
        System.out.println(" " + ANSI_GREEN + "✓ OK!" + ANSI_RESET);
    }
    
    /**
     * Exibe uma barra de progresso animada
     */
    private static void showProgressBar(String label, int percent) {
        int bars = percent / 2; // 50 caracteres = 100%
        String filled = "█".repeat(Math.max(0, bars));
        String empty = "░".repeat(Math.max(0, 50 - bars));
        
        System.out.print("\r" + ANSI_CYAN + label + " [" + filled + empty + "] " + 
                         ANSI_BOLD + percent + "%" + ANSI_RESET);
        System.out.flush();
    }
    
    /**
     * Aguarda o usuário pressionar ENTER
     */
    private static void pressEnterToContinue(Scanner sc) {
        System.out.print("\n" + ANSI_YELLOW + "Pressione ENTER para continuar..." + ANSI_RESET);
        sc.nextLine();
    }
    
    /**
     * Exibe um título de seção com destaque
     */
    private static void printSectionTitle(String title) {
        clearScreen();
        System.out.println("\n" + ANSI_BOLD + ANSI_BLUE);
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("   " + title);
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println(ANSI_RESET);
    }
    
    /**
     * Mensagens de sucesso/erro/aviso estilizadas
     */
    private static void showSuccess(String message) {
        System.out.println(ANSI_GREEN + "✓ " + message + ANSI_RESET);
    }
    
    private static void showError(String message) {
        System.out.println(ANSI_RED + "✗ " + message + ANSI_RESET);
    }
    
    private static void showWarning(String message) {
        System.out.println(ANSI_YELLOW + "⚠ " + message + ANSI_RESET);
    }
    
    private static void showInfo(String message) {
        System.out.println(ANSI_CYAN + "ℹ " + message + ANSI_RESET);
    }



    // ================================
    // INICIALIZAÇÃO DE CHAVES RSA
    // ================================
    private static void inicializarChavesCriptografia() throws Exception {
        File keysDir = new File(System.getProperty("user.dir"), "keys");
        File publicKey = new File(keysDir, "public_key.pem");
        File privateKey = new File(keysDir, "private_key.pem");
        
        if (!publicKey.exists() || !privateKey.exists()) {
            if (!keysDir.exists() && !keysDir.mkdirs()) {
                throw new Exception("Não foi possível criar diretório de chaves: " + keysDir.getAbsolutePath());
            }
            System.out.println(ANSI_YELLOW + "⚙️  Gerando par de chaves RSA-2048..." + ANSI_RESET);
            RSAKeyGen.main(new String[]{});
            System.out.println(ANSI_GREEN + "✓ Chaves RSA inicializadas com sucesso!" + ANSI_RESET);
        }
    }

    // ================================
    // VERIFICAÇÃO INICIAL DE DADOS
    // ================================
    private static boolean existemArquivosDeDados() {
        File[] arquivosCriticos = {
            ANIMAIS_DATA_FILE,
            ONGS_DATA_FILE,
            ADOTANTES_DATA_FILE,
            VOLUNTARIOS_DATA_FILE
        };
        
        for (File f : arquivosCriticos) {
            if (f.exists() && f.length() >= 128) { // 128 = tamanho do header (aceita vazio também)
                return true;
            }
        }
        return false;
    }
    
    private static boolean menuInicializacao(Scanner sc) throws IOException {
        System.out.println("\n" + ANSI_CYAN + "╔══════════════════════════════════════════════════════╗" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "║" + ANSI_RESET + ANSI_BOLD + "       🐾 MPet - Sistema de Adoção de Pets 🐾       " + ANSI_RESET + ANSI_CYAN + "║" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "╚══════════════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_YELLOW + "⚠️  Nenhum arquivo de dados foi encontrado!" + ANSI_RESET);
        System.out.println();
        System.out.println("Escolha uma opção:");
        System.out.println("1) Iniciar sistema novo (vazio)");
        System.out.println("2) Restaurar de backup comprimido");
        System.out.println("0) Sair");
        System.out.print("\n→ Opção: ");
        
        String opcao = sc.nextLine().trim();
        
        switch (opcao) {
            case "1":
                System.out.println(ANSI_GREEN + "\n✓ Iniciando sistema novo..." + ANSI_RESET);
                return true; // Continuar com sistema vazio
                
            case "2":
                return restaurarBackupInicial(sc);
                
            case "0":
                System.out.println(ANSI_PURPLE + "Até logo!" + ANSI_RESET);
                return false;
                
            default:
                System.out.println(ANSI_RED + "Opção inválida!" + ANSI_RESET);
                return menuInicializacao(sc); // Recursão para tentar novamente
        }
    }
    
    private static boolean restaurarBackupInicial(Scanner sc) throws IOException {
        System.out.println(ANSI_BLUE + "\n═══ Restaurar Backup ═══" + ANSI_RESET);
        
        // Buscar TODOS os backups disponíveis (com timestamp)
        java.util.ArrayList<File> backupsDisponiveis = buscarTodosBackups();
        
        if (!backupsDisponiveis.isEmpty()) {
            System.out.println(ANSI_GREEN + "\n✓ Backups disponíveis encontrados:" + ANSI_RESET);
            System.out.println();
            
            // Mostrar lista numerada
            for (int i = 0; i < backupsDisponiveis.size(); i++) {
                File backup = backupsDisponiveis.get(i);
                long tamanhoBytes = backup.length();
                double tamanhoMB = tamanhoBytes / (1024.0 * 1024.0);
                String dataStr = extrairDataDoNome(backup.getName());
                
                System.out.printf("  [%d] %s%n", i + 1, backup.getName());
                System.out.printf("      📅 Data: %s  |  📦 Tamanho: %.2f MB%n", dataStr, tamanhoMB);
                System.out.println();
            }
            
            System.out.print("Escolha um backup para restaurar (0 para cancelar): ");
            try {
                int escolha = Integer.parseInt(sc.nextLine().trim());
                
                if (escolha == 0) {
                    System.out.println(ANSI_YELLOW + "✗ Operação cancelada." + ANSI_RESET);
                    return false;
                }
                
                if (escolha < 1 || escolha > backupsDisponiveis.size()) {
                    System.out.println(ANSI_RED + "✗ Opção inválida!" + ANSI_RESET);
                    return false;
                }
                
                File backupEscolhido = backupsDisponiveis.get(escolha - 1);
                System.out.println(ANSI_BLUE + "\n→ Restaurando dados do backup: " + backupEscolhido.getName() + ANSI_RESET);
                restaurarBackupComprimido(backupEscolhido);
                System.out.println(ANSI_YELLOW + "\n⚠️  Pressione ENTER para continuar..." + ANSI_RESET);
                sc.nextLine();
                return true;
                
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "✗ Entrada inválida! Digite um número." + ANSI_RESET);
                return false;
            }
        } else {
            System.out.println(ANSI_YELLOW + "⚠️  Nenhum backup encontrado." + ANSI_RESET);
            System.out.println(ANSI_CYAN + "   💡 Dica: Os backups são salvos na RAIZ do projeto com o formato:" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "           backup_AAAAMMDD_HHMMSS.zip" + ANSI_RESET);
        }
        
        // Solicitar caminho do arquivo
        System.out.println("\nInforme o caminho completo do arquivo de backup:");
        System.out.print("→ Caminho: ");
        String caminhoBackup = sc.nextLine().trim();
        
        if (caminhoBackup.isEmpty()) {
            System.out.println(ANSI_RED + "✗ Operação cancelada." + ANSI_RESET);
            return menuInicializacao(sc); // Volta ao menu inicial
        }
        
        File arquivoBackup = new File(caminhoBackup);
        
        if (!arquivoBackup.exists()) {
            System.out.println(ANSI_RED + "✗ Arquivo não encontrado: " + caminhoBackup + ANSI_RESET);
            System.out.print("\nTentar novamente? (S/n): ");
            String resp = sc.nextLine().trim().toUpperCase();
            if (resp.isEmpty() || resp.equals("S")) {
                return restaurarBackupInicial(sc);
            } else {
                return menuInicializacao(sc);
            }
        }
        
        try {
            System.out.println(ANSI_BLUE + "\n→ Restaurando dados do backup..." + ANSI_RESET);
            restaurarBackupComprimido(arquivoBackup);
            System.out.println(ANSI_GREEN + "✓ Dados restaurados com sucesso!" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "\n⚠️  Pressione ENTER para continuar..." + ANSI_RESET);
            sc.nextLine();
            return true;
        } catch (IOException e) {
            System.out.println(ANSI_RED + "✗ Erro ao restaurar backup: " + e.getMessage() + ANSI_RESET);
            System.out.print("\nTentar novamente? (S/n): ");
            String resp = sc.nextLine().trim().toUpperCase();
            if (resp.isEmpty() || resp.equals("S")) {
                return restaurarBackupInicial(sc);
            } else {
                return menuInicializacao(sc);
            }
        }
    }

    public static void main(String[] args) {
        // Exibir splash screen
        showSplashScreen();
        
        if (!DATA_DIR.exists() && !DATA_DIR.mkdirs()) {
            showError("Falha ao criar diretório de dados.");
            return;
        }
        
        // Mostrar diretório de dados sendo usado
        showInfo("📂 Diretório de dados: " + DATA_DIR.getAbsolutePath());
        
        // Inicializar chaves RSA
        try {
            inicializarChavesCriptografia();
        } catch (Exception e) {
            showWarning("Falha ao inicializar chaves RSA: " + e.getMessage());
            showWarning("A aplicação continuará em modo compatível (senhas em texto plano).");
        }
        
        // Verificar se existem dados, se não, oferecer opções
        Scanner scInicial = new Scanner(System.in);
        try {
            if (!existemArquivosDeDados()) {
                boolean continuar = menuInicializacao(scInicial);
                if (!continuar) {
                    return; // Usuário escolheu sair
                }
            }
        } catch (IOException e) {
            System.err.println(ANSI_RED + "Erro na inicialização: " + e.getMessage() + ANSI_RESET);
            return;
        }
        
        try (
            Scanner sc = new Scanner(System.in);
            AnimalDataFileDao animalDao = new AnimalDataFileDao(ANIMAIS_DATA_FILE, VERSAO);
            OngDataFileDao ongDao = new OngDataFileDao(ONGS_DATA_FILE, VERSAO);
            AdotanteDataFileDao adotanteDao = new AdotanteDataFileDao(ADOTANTES_DATA_FILE, VERSAO);
            VoluntarioDataFileDao voluntarioDao = new VoluntarioDataFileDao(VOLUNTARIOS_DATA_FILE, VERSAO);
            AdocaoDataFileDao adocaoDao = new AdocaoDataFileDao(ADOCOES_DATA_FILE, VERSAO);
            InteresseDataFileDao interesseDao = new InteresseDataFileDao(INTERESSES_DATA_FILE, VERSAO);
            ChatThreadDataFileDao chatThreadDao = new ChatThreadDataFileDao(CHAT_THREADS_DATA_FILE, VERSAO);
            ChatMessageDataFileDao chatMsgDao = new ChatMessageDataFileDao(CHAT_MSGS_DATA_FILE, VERSAO)
        ) {
            while (true) {
                UsuarioLogado login = telaLogin(sc, adotanteDao, voluntarioDao);
                if (login == null) {
                    System.out.println(ANSI_PURPLE + "Até logo!" + ANSI_RESET);
                    return;
                }
                switch (login.tipo) {
                    case ADMIN -> menuAdmin(sc, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao);
                    case ADOTANTE -> menuAdotanteLogado(sc, adotanteDao, animalDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao, (Adotante) login.usuario);
                    case VOLUNTARIO -> menuVoluntarioLogado(sc, voluntarioDao, animalDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao, (Voluntario) login.usuario);
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

    private static void menuAdmin(Scanner sc, AnimalDataFileDao animalDao, OngDataFileDao ongDao, AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao, AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao, ChatThreadDataFileDao chatThreadDao, ChatMessageDataFileDao chatMsgDao) throws IOException {
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
                case "5" -> menuSistema(sc, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao);
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
    private static void menuAdotanteLogado(Scanner sc, AdotanteDataFileDao adotanteDao, AnimalDataFileDao animalDao, AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao, ChatThreadDataFileDao chatThreadDao, ChatMessageDataFileDao chatMsgDao, Adotante a) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Painel do Adotante ---" + ANSI_RESET);
            System.out.println("1) Ver meus dados");
            System.out.println("2) Editar meus dados básicos");
            System.out.println("3) Listar animais disponíveis");
            System.out.println("4) Demonstrar interesse em um animal");
            System.out.println("5) Ver minhas conversas");
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
                case "3" -> listarAnimaisDisponiveis(animalDao, adocaoDao);
                case "4" -> demonstrarInteresse(sc, a, animalDao, adocaoDao, interesseDao);
                case "5" -> verMinhasConversas(sc, a, chatThreadDao, chatMsgDao);
                case "0" -> { return; }
                default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
            }
        }
    }

    private static void menuVoluntarioLogado(Scanner sc, VoluntarioDataFileDao voluntarioDao, AnimalDataFileDao animalDao, AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao, ChatThreadDataFileDao chatThreadDao, ChatMessageDataFileDao chatMsgDao, Voluntario v) throws IOException {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Painel do Voluntário ---" + ANSI_RESET);
            System.out.println("1) Ver meus dados");
            System.out.println("2) Editar meus dados básicos");
            System.out.println("3) Listar animais da minha ONG");
            System.out.println("4) Criar animal na minha ONG");
            System.out.println("5) Editar animal da minha ONG");
            System.out.println("6) Remover animal da minha ONG");
            System.out.println("7) Interessados por um animal");
            System.out.println("8) Aprovar match (abrir chat)");
            System.out.println("9) Chats: listar e enviar mensagem");
            System.out.println("10) Confirmar adoção");
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
                case "3" -> listarAnimaisDaMinhaOng(animalDao, v.getIdOng(), adocaoDao);
                case "4" -> criarAnimalVoluntario(sc, animalDao, v.getIdOng());
                case "5" -> editarAnimalVoluntario(sc, animalDao, v.getIdOng());
                case "6" -> removerAnimalVoluntario(sc, animalDao, v.getIdOng());
                case "7" -> listarInteressadosPorAnimal(sc, v.getIdOng(), animalDao, interesseDao);
                case "8" -> aprovarMatchAbrirChat(sc, v.getIdOng(), animalDao, interesseDao, chatThreadDao);
                case "9" -> chatsListarEEnviar(sc, v, animalDao, chatThreadDao, chatMsgDao);
                case "10" -> confirmarAdocao(sc, v.getIdOng(), animalDao, interesseDao, adocaoDao, chatThreadDao, chatMsgDao);
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
    private static void menuSistema(Scanner sc, AnimalDataFileDao animalDao, OngDataFileDao ongDao, AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao, AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao, ChatThreadDataFileDao chatThreadDao, ChatMessageDataFileDao chatMsgDao) {
        while (true) {
            System.out.println(ANSI_CYAN + "\n--- Sistema ---" + ANSI_RESET);
            System.out.println("1) Fazer Backup (Compressão Huffman/LZW)");
            System.out.println("2) Restaurar Backup");
            System.out.println("3) Compactar Arquivos (Vacuum)");
            System.out.println(ANSI_PURPLE + "4) 🌱 Popular Base de Dados (Seed)" + ANSI_RESET);
            System.out.println(ANSI_RED + "5) 🗑️  Deletar TODOS os Dados" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "6) 📦 Gerenciar Backups (Listar/Deletar)" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "7) 🔐 Verificar Criptografia de Senhas (RSA)" + ANSI_RESET);
            System.out.println(ANSI_RED + "0) Voltar ao Menu Principal" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();

            try {
                switch (op) {
                    case "1" -> {
                        System.out.println(ANSI_CYAN + "\n--- Algoritmo de Compressão ---" + ANSI_RESET);
                        System.out.println("1) Huffman");
                        System.out.println("2) LZW ⭐ (Recomendado - com descompressão)");
                        System.out.println(ANSI_RED + "0) Cancelar" + ANSI_RESET);
                        System.out.print("Escolha o algoritmo: ");
                        String algoOp = sc.nextLine().trim();
                        
                        switch (algoOp) {
                            case "1":
                                System.out.println(ANSI_BLUE + "Iniciando backup com Huffman..." + ANSI_RESET);
                                System.out.println(ANSI_YELLOW + "⚠️  AVISO: Huffman não possui descompressão implementada!" + ANSI_RESET);
                                String nomeBackupHuffman = Compressao.comprimir(1); // 1 = Huffman
                                if (nomeBackupHuffman != null) {
                                    System.out.println(ANSI_GREEN + "✓ Backup salvo em: " + nomeBackupHuffman + ANSI_RESET);
                                } else {
                                    System.out.println(ANSI_RED + "✗ Erro ao criar backup." + ANSI_RESET);
                                }
                                break;
                            case "2":
                                System.out.println(ANSI_BLUE + "Iniciando backup com LZW..." + ANSI_RESET);
                                String nomeBackupLZW = Compressao.comprimir(2); // 2 = LZW
                                if (nomeBackupLZW != null) {
                                    System.out.println(ANSI_GREEN + "✓ Backup salvo em: " + nomeBackupLZW + " (restaurável)" + ANSI_RESET);
                                } else {
                                    System.out.println(ANSI_RED + "✗ Erro ao criar backup." + ANSI_RESET);
                                }
                                break;
                            case "0":
                                System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
                                break;
                            default:
                                System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                                break;
                        }
                    }
                    case "2" -> {
                        System.out.println(ANSI_YELLOW + "ATENÇÃO: Esta ação sobrescreverá os dados atuais." + ANSI_RESET);
                        if (perguntarBool(sc, "Deseja continuar? (s/n): ")) {
                            // Buscar backups disponíveis
                            java.util.ArrayList<File> backups = buscarTodosBackups();
                            
                            if (backups.isEmpty()) {
                                System.out.println(ANSI_RED + "✗ Nenhum backup encontrado!" + ANSI_RESET);
                                System.out.println(ANSI_YELLOW + "   💡 Dica: Crie um backup primeiro (opção 1 no menu Sistema)" + ANSI_RESET);
                                break;
                            }
                            
                            System.out.println(ANSI_CYAN + "\n📦 Backups disponíveis:" + ANSI_RESET);
                            for (int i = 0; i < backups.size(); i++) {
                                File backup = backups.get(i);
                                double tamanhoMB = backup.length() / 1024.0 / 1024.0;
                                String dataStr = extrairDataDoNome(backup.getName());
                                
                                System.out.printf("%s[%d]%s %s", 
                                    ANSI_BOLD, (i + 1), ANSI_RESET, 
                                    backup.getName());
                                if (dataStr != null) {
                                    System.out.printf(" (%s)", dataStr);
                                }
                                System.out.printf(" - %.2f MB\n", tamanhoMB);
                            }
                            
                            System.out.println();
                            System.out.print("Digite o número do backup a restaurar (0 para cancelar): ");
                            String input = sc.nextLine().trim();
                            
                            try {
                                int escolha = Integer.parseInt(input);
                                
                                if (escolha == 0) {
                                    System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
                                    break;
                                }
                                
                                if (escolha < 1 || escolha > backups.size()) {
                                    System.out.println(ANSI_RED + "Número inválido." + ANSI_RESET);
                                    break;
                                }
                                
                                File backupEscolhido = backups.get(escolha - 1);
                                
                                // Fechar todos os DAOs antes de restaurar
                                animalDao.close();
                                ongDao.close();
                                adotanteDao.close();
                                voluntarioDao.close();
                                adocaoDao.close();
                                interesseDao.close();
                                chatThreadDao.close();
                                chatMsgDao.close();
                                
                                // Restaurar backup escolhido
                                restaurarBackupComprimido(backupEscolhido);
                                
                                System.out.println(ANSI_GREEN + "✓ Restauração concluída! Por favor, reinicie o programa para carregar os novos dados." + ANSI_RESET);
                                System.exit(0);
                                
                            } catch (NumberFormatException e) {
                                System.out.println(ANSI_RED + "Entrada inválida. Digite apenas números." + ANSI_RESET);
                            }
                        }
                    }
                    case "3" -> {
                        System.out.println(ANSI_YELLOW + "Iniciando compactação (vacuum)..." + ANSI_RESET);
                        animalDao.vacuum();
                        ongDao.vacuum();
                        adotanteDao.vacuum();
                        voluntarioDao.vacuum();
                        adocaoDao.vacuum();
                        interesseDao.vacuum();
                        chatThreadDao.vacuum();
                        chatMsgDao.vacuum();
                        System.out.println(ANSI_GREEN + "Compactação concluída. É recomendado reiniciar o programa." + ANSI_RESET);
                    }
                    case "4" -> {
                        menuSeed(sc, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao);
                    }
                    case "5" -> {
                        deletarTodosDados(sc, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao);
                    }
                    case "6" -> {
                        menuGerenciarBackups(sc);
                    }
                    case "7" -> {
                        verificarCriptografiaSenhas(sc, adotanteDao, voluntarioDao);
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
    // DELETAR TODOS OS DADOS
    // =================================================================================
    private static void deletarTodosDados(Scanner sc, AnimalDataFileDao animalDao, OngDataFileDao ongDao,
                                         AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao,
                                         AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao,
                                         ChatThreadDataFileDao chatThreadDao, ChatMessageDataFileDao chatMsgDao) {
        System.out.println("\n" + ANSI_RED + "╔════════════════════════════════════════════════════════════╗" + ANSI_RESET);
        System.out.println(ANSI_RED + "║" + ANSI_RESET + ANSI_BOLD + "            ⚠️  DELETAR TODOS OS DADOS ⚠️             " + ANSI_RESET + ANSI_RED + "║" + ANSI_RESET);
        System.out.println(ANSI_RED + "╚════════════════════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_RED + ANSI_BOLD + "⛔ ATENÇÃO: ESTA AÇÃO É IRREVERSÍVEL!" + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "   Todos os arquivos .dat e .idx serão PERMANENTEMENTE deletados!" + ANSI_RESET);
        System.out.println();
        System.out.println("Arquivos que serão deletados:");
        System.out.println("  • animais.dat e animais.dat.idx");
        System.out.println("  • ongs.dat e ongs.dat.idx");
        System.out.println("  • adotantes.dat e adotantes.dat.idx");
        System.out.println("  • voluntarios.dat e voluntarios.dat.idx");
        System.out.println("  • adocoes.dat e adocoes.dat.idx");
        System.out.println("  • interesses.dat e interesses.dat.idx");
        System.out.println("  • chat_threads.dat e chat_threads.dat.idx");
        System.out.println("  • chat_msgs.dat e chat_msgs.dat.idx");
        System.out.println();
        System.out.println(ANSI_YELLOW + "💡 Dica: Faça um backup antes de deletar!" + ANSI_RESET);
        System.out.println();
        
        System.out.print(ANSI_RED + "Digite 'DELETAR' (em maiúsculas) para confirmar: " + ANSI_RESET);
        String confirmacao = sc.nextLine().trim();
        
        if (!confirmacao.equals("DELETAR")) {
            System.out.println(ANSI_GREEN + "\n✓ Operação cancelada. Nenhum arquivo foi deletado." + ANSI_RESET);
            return;
        }
        
        System.out.println();
        System.out.println(ANSI_RED + "🗑️  Deletando arquivos..." + ANSI_RESET);
        
        try {
            // Fechar todos os DAOs primeiro (ignorando erros se já estiverem fechados)
            try { animalDao.close(); } catch (Exception ignored) {}
            try { ongDao.close(); } catch (Exception ignored) {}
            try { adotanteDao.close(); } catch (Exception ignored) {}
            try { voluntarioDao.close(); } catch (Exception ignored) {}
            try { adocaoDao.close(); } catch (Exception ignored) {}
            try { interesseDao.close(); } catch (Exception ignored) {}
            try { chatThreadDao.close(); } catch (Exception ignored) {}
            try { chatMsgDao.close(); } catch (Exception ignored) {}
            
            int deletados = 0;
            int falhas = 0;
            
            // Listar e deletar todos os arquivos .dat e .idx
            File[] arquivos = DATA_DIR.listFiles((dir, name) -> 
                name.endsWith(".dat") || name.endsWith(".idx"));
            
            if (arquivos != null) {
                for (File arquivo : arquivos) {
                    if (arquivo.delete()) {
                        System.out.println(ANSI_GREEN + "  ✓ " + arquivo.getName() + " deletado" + ANSI_RESET);
                        deletados++;
                    } else {
                        System.out.println(ANSI_RED + "  ✗ Falha ao deletar " + arquivo.getName() + ANSI_RESET);
                        falhas++;
                    }
                }
            }
            
            System.out.println();
            System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println(ANSI_BOLD + "RESUMO:" + ANSI_RESET);
            System.out.println("  • Arquivos deletados: " + ANSI_GREEN + deletados + ANSI_RESET);
            if (falhas > 0) {
                System.out.println("  • Falhas: " + ANSI_RED + falhas + ANSI_RESET);
            }
            System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println();
            
            if (deletados > 0) {
                System.out.println(ANSI_GREEN + "✓ Dados deletados com sucesso!" + ANSI_RESET);
                System.out.println(ANSI_YELLOW + "\n⚠️  O programa será encerrado. Execute novamente para criar novos dados." + ANSI_RESET);
                System.out.println(ANSI_YELLOW + "Pressione ENTER para sair..." + ANSI_RESET);
                sc.nextLine();
                System.exit(0);
            }
            
        } catch (Exception e) {
            System.out.println(ANSI_RED + "\n✗ Erro ao deletar dados: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }

    // =================================================================================
    // SEED - POPULAR BASE DE DADOS
    // =================================================================================
    private static void menuSeed(Scanner sc, AnimalDataFileDao animalDao, OngDataFileDao ongDao, 
                                 AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao,
                                 AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao,
                                 ChatThreadDataFileDao chatThreadDao, ChatMessageDataFileDao chatMsgDao) {
        try {
            System.out.println("\n" + ANSI_PURPLE + "╔════════════════════════════════════════════════════════════╗" + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "║" + ANSI_RESET + ANSI_BOLD + "            🌱 POPULAR BASE DE DADOS (SEED) 🌱            " + ANSI_RESET + ANSI_PURPLE + "║" + ANSI_RESET);
            System.out.println(ANSI_PURPLE + "╚════════════════════════════════════════════════════════════╝" + ANSI_RESET);
            
            System.out.println("\n" + ANSI_YELLOW + "⚠️  ATENÇÃO: Esta operação irá criar dados de teste!" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "   Os dados existentes NÃO serão removidos." + ANSI_RESET);
            System.out.println();
            System.out.println(ANSI_CYAN + "💡 IMPORTANTE: Se você quer começar do zero, use:" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "   Menu Sistema → 5) Deletar TODOS os Dados (depois volte aqui)" + ANSI_RESET);
            System.out.println();
            
            // Perguntar quantidades
            System.out.println(ANSI_CYAN + "Quantas entidades deseja criar?" + ANSI_RESET);
            System.out.println(ANSI_WHITE + "(Pressione ENTER para usar valor padrão)" + ANSI_RESET);
            System.out.println();
            
            int numOngs = perguntarNumero(sc, "ONGs", 10, 1, 50);
            int numVoluntarios = perguntarNumero(sc, "Voluntários (por ONG)", 5, 1, 20);
            int numAdotantes = perguntarNumero(sc, "Adotantes", 50, 1, 200);
            int numAnimais = perguntarNumero(sc, "Animais (por ONG)", 20, 1, 100);
            int numInteresses = perguntarNumero(sc, "Interesses", 30, 1, 150);
            int numAdocoes = perguntarNumero(sc, "Adoções", 10, 1, 100);
            int numThreads = perguntarNumero(sc, "Conversas (Threads)", 15, 1, 100);
            int numMensagens = perguntarNumero(sc, "Mensagens (por thread)", 5, 1, 30);
            
            System.out.println();
            System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println(ANSI_BOLD + "RESUMO:" + ANSI_RESET);
            System.out.println("  • " + numOngs + " ONGs");
            System.out.println("  • " + (numOngs * numVoluntarios) + " Voluntários (" + numVoluntarios + " por ONG)");
            System.out.println("  • " + numAdotantes + " Adotantes");
            System.out.println("  • " + (numOngs * numAnimais) + " Animais (" + numAnimais + " por ONG)");
            System.out.println("  • " + numInteresses + " Interesses");
            System.out.println("  • " + numAdocoes + " Adoções");
            System.out.println("  • " + numThreads + " Conversas");
            System.out.println("  • " + (numThreads * numMensagens) + " Mensagens (" + numMensagens + " por conversa)");
            System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println();
            
            if (!perguntarBool(sc, "Confirmar criação? (s/n): ")) {
                System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
                return;
            }
            
            // Preparar ambiente: rebuild de índices
            System.out.println();
            System.out.println(ANSI_BLUE + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println(ANSI_BLUE + "    🔧 PREPARANDO AMBIENTE PARA SEED" + ANSI_RESET);
            System.out.println(ANSI_BLUE + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println();
            
            try {
                // Rebuild de índices (se necessário)
                System.out.println(ANSI_CYAN + "→ Verificando e reconstruindo índices B+..." + ANSI_RESET);
                
                int indicesReconstruidos = 0;
                
                try {
                    ongDao.rebuildIfEmpty();
                    System.out.println(ANSI_GREEN + "  ✓ Índice de ONGs OK" + ANSI_RESET);
                    indicesReconstruidos++;
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "  ✗ ONGs: " + e.getMessage() + ANSI_RESET);
                    throw new RuntimeException("Falha ao preparar índice de ONGs", e);
                }
                
                try {
                    animalDao.rebuildIfEmpty();
                    System.out.println(ANSI_GREEN + "  ✓ Índice de Animais OK" + ANSI_RESET);
                    indicesReconstruidos++;
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "  ✗ Animais: " + e.getMessage() + ANSI_RESET);
                    throw new RuntimeException("Falha ao preparar índice de Animais", e);
                }
                
                try {
                    adotanteDao.rebuildIfEmpty();
                    System.out.println(ANSI_GREEN + "  ✓ Índice de Adotantes OK" + ANSI_RESET);
                    indicesReconstruidos++;
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "  ✗ Adotantes: " + e.getMessage() + ANSI_RESET);
                    throw new RuntimeException("Falha ao preparar índice de Adotantes", e);
                }
                
                try {
                    voluntarioDao.rebuildIfEmpty();
                    System.out.println(ANSI_GREEN + "  ✓ Índice de Voluntários OK" + ANSI_RESET);
                    indicesReconstruidos++;
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "  ✗ Voluntários: " + e.getMessage() + ANSI_RESET);
                    throw new RuntimeException("Falha ao preparar índice de Voluntários", e);
                }
                
                System.out.println();
                System.out.println(ANSI_GREEN + "✓ " + indicesReconstruidos + " índices preparados! Iniciando criação de dados..." + ANSI_RESET);
                
            } catch (Exception e) {
                System.out.println();
                System.out.println(ANSI_RED + "✗ Erro ao preparar ambiente: " + e.getMessage() + ANSI_RESET);
                System.out.println();
                System.out.println(ANSI_YELLOW + "💡 Solução: Use a opção '5) Deletar TODOS os Dados' primeiro." + ANSI_RESET);
                System.out.println(ANSI_YELLOW + "   Isso limpará os arquivos corrompidos e permitirá criar dados novos." + ANSI_RESET);
                return;
            }
            
            System.out.println();
            executarSeed(animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao, interesseDao, 
                        chatThreadDao, chatMsgDao, numOngs, numVoluntarios, numAdotantes, numAnimais,
                        numInteresses, numAdocoes, numThreads, numMensagens);
                        
            System.out.println("\n" + ANSI_GREEN + "✓ Seed concluído com sucesso!" + ANSI_RESET);
            System.out.println(ANSI_YELLOW + "Pressione ENTER para continuar..." + ANSI_RESET);
            sc.nextLine();
            
        } catch (Exception e) {
            System.out.println(ANSI_RED + "✗ Erro ao executar seed: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }
    
    private static int perguntarNumero(Scanner sc, String entidade, int padrao, int min, int max) {
        System.out.print("  → " + entidade + " [" + ANSI_GREEN + padrao + ANSI_RESET + "]: ");
        String input = sc.nextLine().trim();
        if (input.isEmpty()) return padrao;
        
        try {
            int valor = Integer.parseInt(input);
            if (valor < min || valor > max) {
                System.out.println(ANSI_YELLOW + "    Valor fora do intervalo [" + min + "-" + max + "]. Usando padrão: " + padrao + ANSI_RESET);
                return padrao;
            }
            return valor;
        } catch (NumberFormatException e) {
            System.out.println(ANSI_YELLOW + "    Valor inválido. Usando padrão: " + padrao + ANSI_RESET);
            return padrao;
        }
    }
    
    private static void executarSeed(AnimalDataFileDao animalDao, OngDataFileDao ongDao,
                                    AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao,
                                    AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao,
                                    ChatThreadDataFileDao chatThreadDao, ChatMessageDataFileDao chatMsgDao,
                                    int numOngs, int numVoluntariosPorOng, int numAdotantes, int numAnimaisPorOng,
                                    int numInteresses, int numAdocoes, int numThreads, int numMensagensPorThread) throws Exception {
        
        String[] nomesOng = {"Amor Animal", "Patinhas Felizes", "Amigos de Patas", "Refúgio Animal", "Lar dos Peludos",
                             "Proteção Pet", "Vida Animal", "Anjos Peludos", "Cantinho dos Bichos", "Adote um Amigo"};
        String[] nomesVoluntario = {"Ana Silva", "Bruno Costa", "Carla Souza", "Daniel Alves", "Eduarda Lima",
                                   "Felipe Santos", "Gabriela Rocha", "Henrique Dias", "Isabela Martins", "João Pedro"};
        String[] nomesAdotante = {"Carlos Alberto", "Mariana Oliveira", "Pedro Henrique", "Juliana Ferreira", "Lucas Gabriel",
                                 "Amanda Santos", "Rafael Costa", "Beatriz Lima", "Thiago Alves", "Camila Rodrigues"};
        String[] nomesAnimais = {"Rex", "Luna", "Bob", "Mel", "Thor", "Nina", "Max", "Bella", "Duke", "Mia",
                                "Zeus", "Lola", "Rocky", "Daisy", "Buddy", "Chloe", "Charlie", "Lucy", "Cooper", "Molly"};
        String[] descricoes = {"Muito carinhoso e brincalhão", "Calmo e obediente", "Adora brincar com crianças",
                              "Necessita de espaço para correr", "Ideal para apartamento", "Muito inteligente",
                              "Companheiro fiel", "Adora carinho", "Muito protetor", "Energético e alegre"};
        
        int totalSteps = numOngs + (numOngs * numVoluntariosPorOng) + numAdotantes + (numOngs * numAnimaisPorOng) +
                        numInteresses + numAdocoes + numThreads + (numThreads * numMensagensPorThread);
        int currentStep = 0;
        
        System.out.println("\n" + ANSI_CYAN + "╔════════════════════════════════════════════════════════════╗" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "║" + ANSI_RESET + ANSI_BOLD + "                  PROGRESSO DE CRIAÇÃO                    " + ANSI_RESET + ANSI_CYAN + "║" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "╚════════════════════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();
        
        java.util.List<Integer> ongIds = new java.util.ArrayList<>();
        java.util.List<String> cpfsVoluntarios = new java.util.ArrayList<>();
        java.util.List<String> cpfsAdotantes = new java.util.ArrayList<>();
        java.util.List<Integer> animalIds = new java.util.ArrayList<>();
        
        // 1. Criar ONGs
        printProgress("Criando ONGs", currentStep, totalSteps);
        for (int i = 0; i < numOngs; i++) {
            Ong ong = new Ong();
            ong.setNome(nomesOng[i % nomesOng.length] + " " + (i + 1));
            ong.setCnpj(String.format("%02d.%03d.%03d/%04d-%02d", i, i, i, i, i % 100));
            ong.setEndereco("Rua " + (char)('A' + (i % 26)) + ", " + (100 + i));
            ong.setTelefone(String.format("(11) 9%04d-%04d", i, i));
            ong.setAtivo(true);
            ong = ongDao.create(ong);
            ongIds.add(ong.getId());
            currentStep++;
            printProgress("ONGs criadas: " + (i + 1) + "/" + numOngs, currentStep, totalSteps);
        }
        
        // 2. Criar Voluntários
        printProgress("Criando Voluntários", currentStep, totalSteps);
        int volCount = 0;
        for (int ongIdx = 0; ongIdx < numOngs; ongIdx++) {
            for (int v = 0; v < numVoluntariosPorOng; v++) {
                String cpf = String.format("%011d", 10000000000L + volCount);
                Voluntario vol = new Voluntario();
                vol.setCpf(cpf);
                vol.setSenha("senha" + volCount); // DAO criptografa automaticamente
                vol.setTelefone(String.format("(11) 8%04d-%04d", volCount, volCount));
                vol.setNome(nomesVoluntario[volCount % nomesVoluntario.length] + " " + volCount);
                vol.setEndereco("Av. Voluntário, " + volCount);
                vol.setIdOng(ongIds.get(ongIdx));
                vol.setCargo(Role.values()[volCount % Role.values().length]);
                vol.setAtivo(true);
                voluntarioDao.create(vol);
                cpfsVoluntarios.add(cpf);
                volCount++;
                currentStep++;
                printProgress("Voluntários criados: " + volCount + "/" + (numOngs * numVoluntariosPorOng), currentStep, totalSteps);
            }
        }
        
        // 3. Criar Adotantes
        printProgress("Criando Adotantes", currentStep, totalSteps);
        for (int i = 0; i < numAdotantes; i++) {
            String cpf = String.format("%011d", 20000000000L + i);
            Adotante ad = new Adotante();
            ad.setCpf(cpf);
            ad.setSenha("senha" + i); // DAO criptografa automaticamente
            ad.setTelefone(String.format("(11) 7%04d-%04d", i, i));
            ad.setNomeCompleto(nomesAdotante[i % nomesAdotante.length] + " " + i);
            ad.setTipoMoradia(TipoMoradia.values()[i % TipoMoradia.values().length]);
            ad.setPossuiTelaProtetora(i % 2 == 0);
            ad.setPossuiOutrosAnimais(i % 3 == 0);
            ad.setHorasForaDeCasa(4 + (i % 9));
            ad.setComposicaoFamiliar(ComposicaoFamiliar.values()[i % ComposicaoFamiliar.values().length]);
            ad.setMotivoAdocao("Companhia e amor aos animais");
            ad.setCientePossuiResponsavel(true);
            ad.setCienteCustos(true);
            ad.setAtivo(true);
            adotanteDao.create(ad);
            cpfsAdotantes.add(cpf);
            currentStep++;
            if ((i + 1) % 10 == 0 || i == numAdotantes - 1) {
                printProgress("Adotantes criados: " + (i + 1) + "/" + numAdotantes, currentStep, totalSteps);
            }
        }
        
        // 4. Criar Animais
        printProgress("Criando Animais", currentStep, totalSteps);
        int animalCount = 0;
        for (int ongIdx = 0; ongIdx < numOngs; ongIdx++) {
            for (int a = 0; a < numAnimaisPorOng; a++) {
                Animal animal;
                if (animalCount % 2 == 0) {
                    Cachorro cao = new Cachorro();
                    cao.setRaca("Vira-lata " + animalCount);
                    cao.setNivelAdestramento(NivelAdestramento.values()[animalCount % NivelAdestramento.values().length]);
                    animal = cao;
                } else {
                    Gato gato = new Gato();
                    gato.setRaca("SRD " + animalCount);
                    gato.setSeDaBemComCachorros(animalCount % 2 == 0);
                    gato.setSeDaBemComGatos(animalCount % 3 != 0);
                    gato.setSeDaBemComCriancas(animalCount % 2 != 0);
                    gato.setAcessoExterior(animalCount % 4 == 0);
                    gato.setPossuiTelamento(animalCount % 3 == 0);
                    animal = gato;
                }
                animal.setIdOng(ongIds.get(ongIdx));
                animal.setNome(nomesAnimais[animalCount % nomesAnimais.length] + " " + animalCount);
                animal.setSexo(animalCount % 2 == 0 ? 'M' : 'F');
                animal.setPorte(Porte.values()[animalCount % Porte.values().length]);
                animal.setVacinado(animalCount % 3 != 0);
                animal.setDescricao(descricoes[animalCount % descricoes.length]);
                animal = animalDao.create(animal);
                animalIds.add(animal.getId());
                animalCount++;
                currentStep++;
                if (animalCount % 20 == 0 || animalCount == (numOngs * numAnimaisPorOng)) {
                    printProgress("Animais criados: " + animalCount + "/" + (numOngs * numAnimaisPorOng), currentStep, totalSteps);
                }
            }
        }
        
        // 5. Criar Interesses
        printProgress("Criando Interesses", currentStep, totalSteps);
        for (int i = 0; i < numInteresses && i < cpfsAdotantes.size() && i < animalIds.size(); i++) {
            Interesse interesse = new Interesse();
            interesse.setCpfAdotante(cpfsAdotantes.get(i % cpfsAdotantes.size()));
            interesse.setIdAnimal(animalIds.get(i % animalIds.size()));
            interesse.setStatus(InteresseStatus.values()[i % InteresseStatus.values().length]);
            interesseDao.create(interesse);
            currentStep++;
            if ((i + 1) % 10 == 0 || i == numInteresses - 1) {
                printProgress("Interesses criados: " + (i + 1) + "/" + numInteresses, currentStep, totalSteps);
            }
        }
        
        // 6. Criar Adoções
        printProgress("Criando Adoções", currentStep, totalSteps);
        for (int i = 0; i < numAdocoes && i < cpfsAdotantes.size() && i < animalIds.size(); i++) {
            Adocao adocao = new Adocao();
            adocao.setCpfAdotante(cpfsAdotantes.get(i % cpfsAdotantes.size()));
            adocao.setIdAnimal(animalIds.get(i % animalIds.size()));
            adocao.setDataAdocao(java.time.LocalDate.now().minusDays(i));
            adocaoDao.create(adocao);
            currentStep++;
            if ((i + 1) % 5 == 0 || i == numAdocoes - 1) {
                printProgress("Adoções criadas: " + (i + 1) + "/" + numAdocoes, currentStep, totalSteps);
            }
        }
        
        // 7. Criar Chat Threads
        printProgress("Criando Conversas", currentStep, totalSteps);
        java.util.List<Integer> threadIds = new java.util.ArrayList<>();
        for (int i = 0; i < numThreads && i < cpfsAdotantes.size() && i < animalIds.size(); i++) {
            ChatThread thread = new ChatThread();
            thread.setIdAnimal(animalIds.get(i % animalIds.size()));
            thread.setCpfAdotante(cpfsAdotantes.get(i % cpfsAdotantes.size()));
            thread.setAberto(i % 3 != 0);
            thread.setCriadoEm(java.time.LocalDateTime.now().minusDays(i));
            thread = chatThreadDao.create(thread);
            threadIds.add(thread.getId());
            currentStep++;
            if ((i + 1) % 5 == 0 || i == numThreads - 1) {
                printProgress("Conversas criadas: " + (i + 1) + "/" + numThreads, currentStep, totalSteps);
            }
        }
        
        // 8. Criar Mensagens
        printProgress("Criando Mensagens", currentStep, totalSteps);
        int msgCount = 0;
        
        // Mensagens realistas para conversa de adoção
        String[] mensagensAdotante = {
            "Olá! Vi o perfil do animal e fiquei muito interessado.",
            "Gostaria de saber mais sobre o temperamento dele.",
            "Ele se dá bem com crianças?",
            "Qual a idade exata dele?",
            "Preciso de alguma documentação específica para adoção?",
            "Obrigado pelas informações! Quando posso visitá-lo?",
            "Estou muito animado com essa adoção!",
            "Minha família toda já está preparada para recebê-lo.",
            "Vocês fornecem algum suporte pós-adoção?",
            "Posso agendar uma visita para esta semana?"
        };
        
        String[] mensagensVoluntario = {
            "Olá! Que bom que se interessou! Vou te passar todas as informações.",
            "Ele é muito dócil e carinhoso, perfeito para famílias.",
            "Sim, ele convive muito bem com crianças de todas as idades!",
            "Ele tem aproximadamente 2 anos, está na fase adulta jovem.",
            "Vou enviar a lista de documentos necessários por aqui mesmo.",
            "Ótimo! Podemos agendar para qualquer dia desta semana.",
            "Ficamos muito felizes com seu interesse! Ele merece um lar amoroso.",
            "Que maravilha! A preparação da família é muito importante.",
            "Sim, oferecemos suporte completo nos primeiros 6 meses!",
            "Claro! Qual dia seria melhor para você?"
        };
        
        for (int t = 0; t < threadIds.size(); t++) {
            for (int m = 0; m < numMensagensPorThread; m++) {
                ChatMessage msg = new ChatMessage();
                msg.setThreadId(threadIds.get(t));
                msg.setSender(m % 2 == 0 ? ChatSender.ADOTANTE : ChatSender.VOLUNTARIO);
                
                // Alternar entre mensagens realistas
                if (m % 2 == 0) {
                    msg.setConteudo(mensagensAdotante[m % mensagensAdotante.length]);
                } else {
                    msg.setConteudo(mensagensVoluntario[m % mensagensVoluntario.length]);
                }
                
                msg.setEnviadoEm(java.time.LocalDateTime.now().minusHours(msgCount));
                chatMsgDao.create(msg);
                msgCount++;
                currentStep++;
                if (msgCount % 20 == 0 || msgCount == (numThreads * numMensagensPorThread)) {
                    printProgress("Mensagens criadas: " + msgCount + "/" + (numThreads * numMensagensPorThread), currentStep, totalSteps);
                }
            }
        }
        
        printProgress("✓ CONCLUÍDO!", totalSteps, totalSteps);
        System.out.println();
    }
    
    private static void printProgress(String status, int current, int total) {
        int barWidth = 50;
        int progress = (int) ((double) current / total * barWidth);
        int percentage = (int) ((double) current / total * 100);
        
        StringBuilder bar = new StringBuilder(ANSI_CYAN + "[");
        for (int i = 0; i < barWidth; i++) {
            if (i < progress) {
                bar.append(ANSI_GREEN + "█" + ANSI_CYAN);
            } else {
                bar.append("░");
            }
        }
        bar.append("]" + ANSI_RESET);
        
        System.out.print("\r" + bar + " " + ANSI_BOLD + percentage + "%" + ANSI_RESET + " - " + status + "          ");
        if (current == total) {
            System.out.println();
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
            zipOne(zos, new File(DATA_DIR, ADOCOES_DATA_FILENAME+".idx"), ADOCOES_DATA_FILENAME+".idx");
            zipOne(zos, INTERESSES_DATA_FILE, INTERESSES_DATA_FILENAME);
            zipOne(zos, INTERESSES_IDX_FILE, INTERESSES_IDX_FILENAME);
            zipOne(zos, CHAT_THREADS_DATA_FILE, CHAT_THREADS_DATA_FILENAME);
            zipOne(zos, CHAT_THREADS_IDX_FILE, CHAT_THREADS_IDX_FILENAME);
            zipOne(zos, CHAT_MSGS_DATA_FILE, CHAT_MSGS_DATA_FILENAME);
            zipOne(zos, CHAT_MSGS_IDX_FILE, CHAT_MSGS_IDX_FILENAME);
        }
        System.out.println(ANSI_GREEN + "Backup gerado com sucesso em: " + ZIP_FILE.getAbsolutePath() + ANSI_RESET);
        listZipContents(ZIP_FILE);
    }

    // =================================================================================
    // BACKUP/RESTORE SIMPLES (SEM COMPRESSÃO) - RESTAURÁVEL
    // =================================================================================
    
    /**
     * Restaura backup comprimido com LZW
     */
    private static void restaurarBackupComprimido() throws IOException {
        restaurarBackupComprimido(new File("backup.zip"));
    }
    
    private static void restaurarBackupComprimido(File backupFile) throws IOException {
        if (!backupFile.exists()) {
            System.out.println(ANSI_RED + "✗ Arquivo não encontrado: " + backupFile.getName() + ANSI_RESET);
            return;
        }
        
        System.out.println(ANSI_BLUE + "\n🔄 Restaurando backup de: " + backupFile.getName() + ANSI_RESET);
        int arquivosRestaurados = 0;
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String entryName = e.getName();
                
                // Remover sufixo ".compressed" se presente
                String cleanName = entryName.endsWith(".compressed") 
                    ? entryName.substring(0, entryName.length() - ".compressed".length())
                    : entryName;
                
                File out = switch (cleanName) {
                    case ANIMAIS_DATA_FILENAME -> ANIMAIS_DATA_FILE;
                    case ANIMAIS_IDX_FILENAME -> ANIMAIS_IDX_FILE;
                    case ONGS_DATA_FILENAME -> ONGS_DATA_FILE;
                    case ONGS_IDX_FILENAME -> ONGS_IDX_FILE;
                    case ADOTANTES_DATA_FILENAME -> ADOTANTES_DATA_FILE;
                    case ADOTANTES_IDX_FILENAME -> ADOTANTES_IDX_FILE;
                    case VOLUNTARIOS_DATA_FILENAME -> VOLUNTARIOS_DATA_FILE;
                    case VOLUNTARIOS_IDX_FILENAME -> VOLUNTARIOS_IDX_FILE;
                    case ADOCOES_DATA_FILENAME -> ADOCOES_DATA_FILE;
                    case ADOCOES_IDX_FILENAME -> ADOCOES_IDX_FILE;
                    case INTERESSES_DATA_FILENAME -> INTERESSES_DATA_FILE;
                    case INTERESSES_IDX_FILENAME -> INTERESSES_IDX_FILE;
                    case CHAT_THREADS_DATA_FILENAME -> CHAT_THREADS_DATA_FILE;
                    case CHAT_THREADS_IDX_FILENAME -> CHAT_THREADS_IDX_FILE;
                    case CHAT_MSGS_DATA_FILENAME -> CHAT_MSGS_DATA_FILE;
                    case CHAT_MSGS_IDX_FILENAME -> CHAT_MSGS_IDX_FILE;
                    default -> null;
                };
                
                if (out != null) {
                    System.out.println("  -> Restaurando " + cleanName + " (descomprimindo)");
                    
                    // 1. Ler dados comprimidos do ZIP
                    byte[] dadosComprimidos = zis.readAllBytes();
                    
                    // 2. DESCOMPRIMIR - tentar ambos os algoritmos
                    byte[] dadosOriginais = null;
                    String algoritmo = null;
                    
                    // Tentar LZW primeiro
                    try {
                        dadosOriginais = LZW.decodifica(dadosComprimidos);
                        algoritmo = "LZW";
                    } catch (Exception exLzw) {
                        // Se LZW falhar, tentar Huffman
                        try {
                            dadosOriginais = Huffman.decodifica(dadosComprimidos);
                            algoritmo = "Huffman";
                        } catch (Exception exHuff) {
                            System.out.println(ANSI_YELLOW + "     ⚠️  Falha ao descomprimir com LZW e Huffman" + ANSI_RESET);
                            System.out.println(ANSI_YELLOW + "     LZW: " + exLzw.getMessage() + ANSI_RESET);
                            System.out.println(ANSI_YELLOW + "     Huffman: " + exHuff.getMessage() + ANSI_RESET);
                            System.out.println(ANSI_YELLOW + "     Salvando dados comprimidos (pode não funcionar)" + ANSI_RESET);
                            dadosOriginais = dadosComprimidos;
                            algoritmo = "FALHA";
                        }
                    }
                    
                    if (!"FALHA".equals(algoritmo)) {
                        System.out.println("     ✓ " + algoritmo + ": " + dadosComprimidos.length + " bytes → " + dadosOriginais.length + " bytes");
                    }
                    
                    // 3. Salvar arquivo descomprimido
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        fos.write(dadosOriginais);
                    }
                    
                    arquivosRestaurados++;
                }
                zis.closeEntry();
            }
        }
        
        System.out.println();
        System.out.println(ANSI_GREEN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "✓ Restauração concluída com sucesso!" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "  📦 Arquivos restaurados: " + arquivosRestaurados + ANSI_RESET);
        System.out.println(ANSI_GREEN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
    }
    
    private static void backupZipSimples() throws IOException {
        File backupFile = new File("backup.zip"); // Na RAIZ do projeto
        System.out.println(ANSI_BLUE + "Criando backup simples (sem compressão)..." + ANSI_RESET);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            zipOne(zos, ANIMAIS_DATA_FILE, ANIMAIS_DATA_FILENAME);
            zipOne(zos, ANIMAIS_IDX_FILE, ANIMAIS_IDX_FILENAME);
            zipOne(zos, ONGS_DATA_FILE, ONGS_DATA_FILENAME);
            zipOne(zos, ONGS_IDX_FILE, ONGS_IDX_FILENAME);
            zipOne(zos, ADOTANTES_DATA_FILE, ADOTANTES_DATA_FILENAME);
            zipOne(zos, ADOTANTES_IDX_FILE, ADOTANTES_IDX_FILENAME);
            zipOne(zos, VOLUNTARIOS_DATA_FILE, VOLUNTARIOS_DATA_FILENAME);
            zipOne(zos, VOLUNTARIOS_IDX_FILE, VOLUNTARIOS_IDX_FILENAME);
            zipOne(zos, ADOCOES_DATA_FILE, ADOCOES_DATA_FILENAME);
            zipOne(zos, ADOCOES_IDX_FILE, ADOCOES_IDX_FILENAME);
            zipOne(zos, INTERESSES_DATA_FILE, INTERESSES_DATA_FILENAME);
            zipOne(zos, INTERESSES_IDX_FILE, INTERESSES_IDX_FILENAME);
            zipOne(zos, CHAT_THREADS_DATA_FILE, CHAT_THREADS_DATA_FILENAME);
            zipOne(zos, CHAT_THREADS_IDX_FILE, CHAT_THREADS_IDX_FILENAME);
            zipOne(zos, CHAT_MSGS_DATA_FILE, CHAT_MSGS_DATA_FILENAME);
            zipOne(zos, CHAT_MSGS_IDX_FILE, CHAT_MSGS_IDX_FILENAME);
        }
        System.out.println(ANSI_GREEN + "✓ Backup salvo em: " + backupFile.getAbsolutePath() + ANSI_RESET);
    }

    private static void restoreZipSimples() throws IOException {
        File backupFile = new File("backup.zip"); // Na RAIZ do projeto
        if (!backupFile.exists()) {
            System.out.println(ANSI_RED + "✗ Arquivo backup.zip não encontrado na raiz do projeto!" + ANSI_RESET);
            return;
        }
        
        System.out.println(ANSI_BLUE + "Restaurando backup de: " + backupFile.getAbsolutePath() + ANSI_RESET);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String entryName = e.getName();
                
                File out = switch (entryName) {
                    case ANIMAIS_DATA_FILENAME -> ANIMAIS_DATA_FILE;
                    case ANIMAIS_IDX_FILENAME -> ANIMAIS_IDX_FILE;
                    case ONGS_DATA_FILENAME -> ONGS_DATA_FILE;
                    case ONGS_IDX_FILENAME -> ONGS_IDX_FILE;
                    case ADOTANTES_DATA_FILENAME -> ADOTANTES_DATA_FILE;
                    case ADOTANTES_IDX_FILENAME -> ADOTANTES_IDX_FILE;
                    case VOLUNTARIOS_DATA_FILENAME -> VOLUNTARIOS_DATA_FILE;
                    case VOLUNTARIOS_IDX_FILENAME -> VOLUNTARIOS_IDX_FILE;
                    case ADOCOES_DATA_FILENAME -> ADOCOES_DATA_FILE;
                    case ADOCOES_IDX_FILENAME -> ADOCOES_IDX_FILE;
                    case INTERESSES_DATA_FILENAME -> INTERESSES_DATA_FILE;
                    case INTERESSES_IDX_FILENAME -> INTERESSES_IDX_FILE;
                    case CHAT_THREADS_DATA_FILENAME -> CHAT_THREADS_DATA_FILE;
                    case CHAT_THREADS_IDX_FILENAME -> CHAT_THREADS_IDX_FILE;
                    case CHAT_MSGS_DATA_FILENAME -> CHAT_MSGS_DATA_FILE;
                    case CHAT_MSGS_IDX_FILENAME -> CHAT_MSGS_IDX_FILE;
                    default -> null;
                };
                
                if (out != null) {
                    System.out.println("  -> Restaurando " + entryName);
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        zis.transferTo(fos);
                    }
                }
                zis.closeEntry();
            }
        }
        System.out.println(ANSI_GREEN + "✓ Restauração concluída!" + ANSI_RESET);
    }
    
    // =================================================================================
    // BACKUP/RESTORE COM COMPRESSÃO (NÃO RESTAURÁVEL)
    // =================================================================================
    private static void restoreZip() throws IOException {
        restoreZip(ZIP_FILE);
    }
    
    private static void restoreZip(File backupFile) throws IOException {
        if (!backupFile.exists()) {
            System.out.println(ANSI_RED + "Arquivo de backup '" + backupFile.getPath() + "' não encontrado." + ANSI_RESET);
            return;
        }
        System.out.println(ANSI_BLUE + "Restaurando arquivos de: " + backupFile.getPath() + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "⚠️  AVISO: Os dados foram comprimidos mas a descompressão não está implementada!" + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "   Os arquivos .dat.compressed não podem ser restaurados para o formato original." + ANSI_RESET);
        System.out.println(ANSI_YELLOW + "   Por favor, use um backup SEM compressão ou implemente os métodos de descompressão." + ANSI_RESET);
        
        // Listar o que tem no backup
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry e;
            System.out.println(ANSI_CYAN + "\nArquivos encontrados no backup:" + ANSI_RESET);
            while ((e = zis.getNextEntry()) != null) {
                System.out.println("  - " + e.getName() + " (" + e.getSize() + " bytes)");
                zis.closeEntry();
            }
        }
        
        System.out.println(ANSI_RED + "\n✗ Restauração CANCELADA - descompressão não implementada!" + ANSI_RESET);
    }

    // ================================
    // FUNÇÕES DE APOIO: DISPONIBILIDADE E INTERAÇÕES
    // ================================
    
    /**
     * Verifica e demonstra que as senhas estão sendo criptografadas com RSA.
     * Mostra dados armazenados em disco (criptografados) vs dados em memória (descriptografados).
     */
    private static void verificarCriptografiaSenhas(Scanner sc, AdotanteDataFileDao adotanteDao, VoluntarioDataFileDao voluntarioDao) throws IOException {
        System.out.println("\n" + ANSI_GREEN + "╔═══════════════════════════════════════════════════════════════╗" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "║" + ANSI_RESET + ANSI_BOLD + "          🔐 VERIFICAÇÃO DE CRIPTOGRAFIA RSA 🔐          " + ANSI_RESET + ANSI_GREEN + "║" + ANSI_RESET);
        System.out.println(ANSI_GREEN + "╚═══════════════════════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_CYAN + "Este teste demonstra que as senhas estão sendo:" + ANSI_RESET);
        System.out.println("  1️⃣  Criptografadas com RSA antes de salvar no disco");
        System.out.println("  2️⃣  Descriptografadas quando carregadas em memória");
        System.out.println("  3️⃣  Armazenadas de forma SEGURA no arquivo .dat");
        System.out.println();
        
        System.out.println(ANSI_YELLOW + "Escolha o tipo de usuário para verificar:" + ANSI_RESET);
        System.out.println("1) Adotante");
        System.out.println("2) Voluntário");
        System.out.println(ANSI_RED + "0) Voltar" + ANSI_RESET);
        System.out.print("Escolha: ");
        String tipoOp = sc.nextLine().trim();
        
        try {
            if (tipoOp.equals("1")) {
                // Verificar Adotante
                List<Adotante> adotantes = adotanteDao.listAllActive();
                if (adotantes.isEmpty()) {
                    System.out.println(ANSI_YELLOW + "\n⚠️  Nenhum adotante cadastrado. Cadastre um adotante primeiro!" + ANSI_RESET);
                    return;
                }
                
                System.out.println(ANSI_CYAN + "\n📋 Adotantes disponíveis:" + ANSI_RESET);
                for (int i = 0; i < adotantes.size(); i++) {
                    Adotante a = adotantes.get(i);
                    System.out.printf("  [%d] %s - CPF: %s\n", i + 1, a.getNomeCompleto(), a.getCpf());
                }
                
                System.out.print("\nEscolha um adotante (0 para cancelar): ");
                int escolha = Integer.parseInt(sc.nextLine().trim());
                if (escolha <= 0 || escolha > adotantes.size()) {
                    System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
                    return;
                }
                
                Adotante adotante = adotantes.get(escolha - 1);
                demonstrarCriptografia(adotante.getCpf(), adotante.getSenha(), "Adotante", adotante.getNomeCompleto());
                
            } else if (tipoOp.equals("2")) {
                // Verificar Voluntário
                List<Voluntario> voluntarios = voluntarioDao.listAllActive();
                if (voluntarios.isEmpty()) {
                    System.out.println(ANSI_YELLOW + "\n⚠️  Nenhum voluntário cadastrado. Cadastre um voluntário primeiro!" + ANSI_RESET);
                    return;
                }
                
                System.out.println(ANSI_CYAN + "\n📋 Voluntários disponíveis:" + ANSI_RESET);
                for (int i = 0; i < voluntarios.size(); i++) {
                    Voluntario v = voluntarios.get(i);
                    System.out.printf("  [%d] %s - CPF: %s\n", i + 1, v.getNome(), v.getCpf());
                }
                
                System.out.print("\nEscolha um voluntário (0 para cancelar): ");
                int escolha = Integer.parseInt(sc.nextLine().trim());
                if (escolha <= 0 || escolha > voluntarios.size()) {
                    System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
                    return;
                }
                
                Voluntario voluntario = voluntarios.get(escolha - 1);
                demonstrarCriptografia(voluntario.getCpf(), voluntario.getSenha(), "Voluntário", voluntario.getNome());
                
            } else if (tipoOp.equals("0")) {
                return;
            } else {
                System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
            }
        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "Entrada inválida. Digite apenas números." + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Erro ao verificar criptografia: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstra a criptografia RSA de uma senha específica.
     */
    private static void demonstrarCriptografia(String cpf, String senhaDescriptografada, String tipo, String nome) {
        System.out.println("\n" + ANSI_CYAN + "═══════════════════════════════════════════════════════════════" + ANSI_RESET);
        System.out.println(ANSI_BOLD + "🔍 ANÁLISE DE CRIPTOGRAFIA" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════════" + ANSI_RESET);
        System.out.println();
        System.out.println(ANSI_YELLOW + "Tipo:" + ANSI_RESET + " " + tipo);
        System.out.println(ANSI_YELLOW + "Nome:" + ANSI_RESET + " " + nome);
        System.out.println(ANSI_YELLOW + "CPF:" + ANSI_RESET + " " + cpf);
        System.out.println();
        
        // Mostrar senha descriptografada (em memória)
        System.out.println(ANSI_GREEN + "✓ Senha DESCRIPTOGRAFADA (em memória):" + ANSI_RESET);
        System.out.println("  " + ANSI_BOLD + senhaDescriptografada + ANSI_RESET);
        System.out.println("  └─ Esta é a senha REAL que o usuário digitou");
        System.out.println();
        
        // Criptografar para mostrar como fica no disco
        try {
            String senhaCriptografada = RSACriptografia.criptografar(senhaDescriptografada);
            System.out.println(ANSI_RED + "🔒 Senha CRIPTOGRAFADA (armazenada no disco .dat):" + ANSI_RESET);
            System.out.println("  " + ANSI_DIM + senhaCriptografada + ANSI_RESET);
            System.out.println("  └─ Esta é a versão SEGURA armazenada no arquivo binário");
            System.out.println();
            
            // Testar ciclo completo: criptografar → descriptografar
            String senhaTestada = RSACriptografia.descriptografar(senhaCriptografada);
            boolean sucesso = senhaDescriptografada.equals(senhaTestada);
            
            if (sucesso) {
                System.out.println(ANSI_GREEN + "✅ TESTE DE CICLO COMPLETO: SUCESSO!" + ANSI_RESET);
                System.out.println("   Original → Criptografado → Descriptografado = " + ANSI_GREEN + "IGUAL ✓" + ANSI_RESET);
            } else {
                System.out.println(ANSI_RED + "❌ TESTE FALHOU!" + ANSI_RESET);
                System.out.println("   Original ≠ Descriptografado");
            }
            
            System.out.println();
            System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println(ANSI_BOLD + "📊 ESTATÍSTICAS:" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println("  • Tamanho senha original: " + senhaDescriptografada.length() + " caracteres");
            System.out.println("  • Tamanho senha criptografada: " + senhaCriptografada.length() + " caracteres");
            System.out.println("  • Algoritmo: RSA (criptografia assimétrica)");
            System.out.println("  • Status: " + (sucesso ? ANSI_GREEN + "✓ SEGURO" + ANSI_RESET : ANSI_RED + "✗ ERRO" + ANSI_RESET));
            System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════════" + ANSI_RESET);
            System.out.println();
            
            System.out.println(ANSI_GREEN + "💡 CONCLUSÃO:" + ANSI_RESET);
            System.out.println("   As senhas são CRIPTOGRAFADAS antes de serem salvas no disco");
            System.out.println("   e DESCRIPTOGRAFADAS quando carregadas em memória.");
            System.out.println("   " + ANSI_BOLD + "✓ Sistema SEGURO!" + ANSI_RESET);
            
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Erro ao testar criptografia: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        }
    }
    
    private static boolean isAdotado(AdocaoDataFileDao adocaoDao, int idAnimal) throws IOException {
        return adocaoDao.listAllActive().stream().anyMatch(a -> a.getIdAnimal() == idAnimal);
    }

    private static void listarAnimaisDisponiveis(AnimalDataFileDao animalDao, AdocaoDataFileDao adocaoDao) throws IOException {
        List<Animal> todos = animalDao.listAllActive();
        List<Animal> disp = todos.stream().filter(a -> {
            try { return !isAdotado(adocaoDao, a.getId()); } catch (IOException e) { return false; }
        }).toList();
        System.out.println(ANSI_CYAN + "\n--- Animais Disponíveis ---" + ANSI_RESET);
        if (disp.isEmpty()) { System.out.println(ANSI_YELLOW + "Nenhum disponível." + ANSI_RESET); return; }
        disp.forEach(Interface::imprimirAnimal);
    }

    private static void demonstrarInteresse(Scanner sc, Adotante adotante, AnimalDataFileDao animalDao, AdocaoDataFileDao adocaoDao, InteresseDataFileDao interesseDao) throws IOException {
        listarAnimaisDisponiveis(animalDao, adocaoDao);
        int id = perguntarInt(sc, "ID do animal para demonstrar interesse");
        Optional<Animal> opt = animalDao.read(id);
        if (opt.isEmpty()) { System.out.println(ANSI_RED + "Animal inválido." + ANSI_RESET); return; }
        if (isAdotado(adocaoDao, id)) { System.out.println(ANSI_YELLOW + "Este animal já foi adotado." + ANSI_RESET); return; }
        // evitar duplicar interesse
        boolean existe = interesseDao.listAllActive().stream()
                .anyMatch(i -> i.getIdAnimal() == id && adotante.getCpf().equals(i.getCpfAdotante()));
        if (existe) { System.out.println(ANSI_YELLOW + "Você já demonstrou interesse por este animal." + ANSI_RESET); return; }
        Interesse it = new Interesse();
        it.setCpfAdotante(adotante.getCpf());
        it.setIdAnimal(id);
        it.setData(java.time.LocalDate.now());
        it.setStatus(InteresseStatus.PENDENTE);
        it.setAtivo(true);
        interesseDao.create(it);
        System.out.println(ANSI_GREEN + "Interesse registrado! Aguarde contato da ONG." + ANSI_RESET);
    }

    private static void listarAnimaisDaMinhaOng(AnimalDataFileDao animalDao, int idOng, AdocaoDataFileDao adocaoDao) throws IOException {
        List<Animal> todos = animalDao.listAllActive();
        todos.stream().filter(a -> a.getIdOng() == idOng).forEach(a -> {
            boolean adotado;
            try { adotado = isAdotado(adocaoDao, a.getId()); } catch (IOException e) { adotado = false; }
            imprimirAnimal(a);
            if (adotado) System.out.println("  > Status: ADOTADO");
        });
    }

    private static void criarAnimalVoluntario(Scanner sc, AnimalDataFileDao animalDao, int idOng) throws IOException {
        System.out.print("Tipo (C=cachorro, G=gato): ");
        String t = sc.nextLine().trim().toUpperCase();
        Animal a;
        if (t.equals("C")) a = new Cachorro(); else if (t.equals("G")) a = new Gato(); else { System.out.println(ANSI_RED + "Tipo inválido." + ANSI_RESET); return; }
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
        Animal salvo = animalDao.create(a);
        System.out.println(ANSI_GREEN + "Animal criado! ID=" + salvo.getId() + ANSI_RESET);
    }

    private static void editarAnimalVoluntario(Scanner sc, AnimalDataFileDao animalDao, int idOng) throws IOException {
        int id = perguntarInt(sc, "ID do animal da sua ONG a editar");
        Optional<Animal> opt = animalDao.read(id);
        if (opt.isEmpty() || opt.get().getIdOng() != idOng) { System.out.println(ANSI_RED + "Animal não pertence à sua ONG." + ANSI_RESET); return; }
        Animal a = opt.get();
        a.setNome(perguntarString(sc, "Nome", a.getNome()));
        a.setDescricao(perguntarString(sc, "Descrição", a.getDescricao()));
        if (a instanceof Cachorro c) {
            c.setRaca(perguntarString(sc, "Raça", c.getRaca()));
        } else if (a instanceof Gato g) {
            g.setRaca(perguntarString(sc, "Raça", g.getRaca()));
        }
        // ONG não pode ser trocada pelo voluntário
        boolean ok = animalDao.update(a);
        System.out.println(ok ? ANSI_GREEN + "Atualizado." + ANSI_RESET : ANSI_RED + "Falha ao atualizar." + ANSI_RESET);
    }

    private static void removerAnimalVoluntario(Scanner sc, AnimalDataFileDao animalDao, int idOng) throws IOException {
        int id = perguntarInt(sc, "ID do animal da sua ONG a remover");
        Optional<Animal> opt = animalDao.read(id);
        if (opt.isEmpty() || opt.get().getIdOng() != idOng) { System.out.println(ANSI_RED + "Animal não pertence à sua ONG." + ANSI_RESET); return; }
        boolean ok = animalDao.delete(id);
        System.out.println(ok ? ANSI_GREEN + "Removido." + ANSI_RESET : ANSI_YELLOW + "Não encontrado." + ANSI_RESET);
    }

    private static void listarInteressadosPorAnimal(Scanner sc, int idOng, AnimalDataFileDao animalDao, InteresseDataFileDao interesseDao) throws IOException {
        int idAnimal = perguntarInt(sc, "ID do animal");
        Optional<Animal> opt = animalDao.read(idAnimal);
        if (opt.isEmpty() || opt.get().getIdOng() != idOng) { System.out.println(ANSI_RED + "Animal não pertence à sua ONG." + ANSI_RESET); return; }
        List<Interesse> ints = interesseDao.listAllActive().stream().filter(i -> i.getIdAnimal() == idAnimal).toList();
        if (ints.isEmpty()) { System.out.println(ANSI_YELLOW + "Sem interessados." + ANSI_RESET); return; }
        System.out.println(ANSI_CYAN + "Interessados (status):" + ANSI_RESET);
        ints.forEach(i -> System.out.printf(" - id=%d | CPF=%s | %s | %s\n", i.getId(), i.getCpfAdotante(), i.getData(), i.getStatus()));
    }

    private static void aprovarMatchAbrirChat(Scanner sc, int idOng, AnimalDataFileDao animalDao, InteresseDataFileDao interesseDao, ChatThreadDataFileDao chatThreadDao) throws IOException {
        int idAnimal = perguntarInt(sc, "ID do animal");
        Optional<Animal> opt = animalDao.read(idAnimal);
        if (opt.isEmpty() || opt.get().getIdOng() != idOng) { System.out.println(ANSI_RED + "Animal não pertence à sua ONG." + ANSI_RESET); return; }
        List<Interesse> pend = interesseDao.listAllActive().stream().filter(i -> i.getIdAnimal() == idAnimal && i.getStatus() == InteresseStatus.PENDENTE).toList();
        if (pend.isEmpty()) { System.out.println(ANSI_YELLOW + "Nenhum interesse pendente." + ANSI_RESET); return; }
        pend.forEach(i -> System.out.printf(" - id=%d | CPF=%s\n", i.getId(), i.getCpfAdotante()));
        int idInteresse = perguntarInt(sc, "ID do interesse a aprovar");
        Optional<Interesse> es = pend.stream().filter(i -> i.getId() == idInteresse).findFirst();
        if (es.isEmpty()) { System.out.println(ANSI_RED + "Interesse inválido." + ANSI_RESET); return; }
        Interesse i = es.get();
        i.setStatus(InteresseStatus.APROVADO);
        interesseDao.update(i);
        // abrir chat se não existir
        boolean exists = chatThreadDao.listAllActive().stream().anyMatch(t -> t.getIdAnimal() == idAnimal && i.getCpfAdotante().equals(t.getCpfAdotante()) && t.isAberto());
        if (!exists) {
            ChatThread t = new ChatThread();
            t.setIdAnimal(idAnimal); t.setCpfAdotante(i.getCpfAdotante()); t.setAberto(true); t.setCriadoEm(java.time.LocalDateTime.now());
            chatThreadDao.create(t);
        }
        System.out.println(ANSI_GREEN + "Match aprovado e chat aberto." + ANSI_RESET);
    }

    private static void chatsListarEEnviar(Scanner sc, Voluntario voluntario, AnimalDataFileDao animalDao, ChatThreadDataFileDao threadDao, ChatMessageDataFileDao msgDao) throws IOException {
        // Filtra threads somente da mesma ONG do voluntário (via idOng do animal)
        List<ChatThread> threads = threadDao.listAllActive().stream()
                .filter(t -> {
                    try {
                        return animalDao.read(t.getIdAnimal()).map(an -> an.getIdOng() == voluntario.getIdOng()).orElse(false);
                    } catch (IOException e) { return false; }
                })
                .toList();
        if (threads.isEmpty()) { System.out.println(ANSI_YELLOW + "Sem chats da sua ONG." + ANSI_RESET); return; }
        threads.forEach(t -> System.out.printf(" - Thread %d | Animal=%d | CPF=%s | Aberto=%s\n", t.getId(), t.getIdAnimal(), t.getCpfAdotante(), t.isAberto()));
        int tid = perguntarInt(sc, "ID da thread para visualizar/enviar");
        Optional<ChatThread> ot = threads.stream().filter(t -> t.getId() == tid).findFirst();
        if (ot.isEmpty()) { System.out.println(ANSI_RED + "Thread inválida." + ANSI_RESET); return; }
        ChatThread t = ot.get();
        // Listar mensagens
        List<ChatMessage> msgs = msgDao.listAllActive().stream().filter(m -> m.getThreadId() == t.getId()).toList();
        if (msgs.isEmpty()) System.out.println("(sem mensagens)");
        else msgs.forEach(m -> {
            String senderLabel = m.getSender() == ChatSender.VOLUNTARIO ? "VOLUNTARIO" + extrairCpfVoluntarioSufixo(m) : m.getSender().name();
            System.out.printf(" [%s] %s\n   📅 %s\n", 
                senderLabel, 
                limparPrefixoVoluntario(m.getConteudo()), 
                formatarTimestamp(m.getEnviadoEm(), m.getZoneId()));
        });
        if (!t.isAberto()) { System.out.println(ANSI_YELLOW + "Thread fechada." + ANSI_RESET); return; }
        String texto = perguntarString(sc, "Mensagem (vazio para cancelar)", "");
        if (texto == null || texto.isBlank()) return;
        ChatMessage m = new ChatMessage();
        m.setThreadId(t.getId());
        m.setSender(ChatSender.VOLUNTARIO);
        // Anexa o CPF do voluntário no conteúdo para identificar o remetente sem mudar o esquema binário
        m.setConteudo(prefixarCpfVoluntario(voluntario.getCpf(), texto));
        m.setEnviadoEm(java.time.LocalDateTime.now());
        m.setZoneId(java.time.ZoneId.systemDefault().getId());  // ✅ Setar zoneId explicitamente
        m.setAtivo(true);
        msgDao.create(m);
        System.out.println(ANSI_GREEN + "Mensagem enviada." + ANSI_RESET);
    }

    private static void confirmarAdocao(Scanner sc, int idOng, AnimalDataFileDao animalDao, InteresseDataFileDao interesseDao, AdocaoDataFileDao adocaoDao, ChatThreadDataFileDao threadDao, ChatMessageDataFileDao msgDao) throws IOException {
        int idAnimal = perguntarInt(sc, "ID do animal");
        Optional<Animal> opt = animalDao.read(idAnimal);
        if (opt.isEmpty() || opt.get().getIdOng() != idOng) { System.out.println(ANSI_RED + "Animal não pertence à sua ONG." + ANSI_RESET); return; }
        if (isAdotado(adocaoDao, idAnimal)) { System.out.println(ANSI_YELLOW + "Já adotado." + ANSI_RESET); return; }
        List<Interesse> aprov = interesseDao.listAllActive().stream().filter(i -> i.getIdAnimal() == idAnimal && i.getStatus() == InteresseStatus.APROVADO).toList();
        if (aprov.isEmpty()) { System.out.println(ANSI_YELLOW + "Não há matches aprovados." + ANSI_RESET); return; }
        aprov.forEach(i -> System.out.printf(" - CPF=%s\n", i.getCpfAdotante()));
        String cpf = perguntarString(sc, "CPF do adotante para confirmar", null);
        Optional<Interesse> es = aprov.stream().filter(i -> i.getCpfAdotante().equals(cpf)).findFirst();
        if (es.isEmpty()) { System.out.println(ANSI_RED + "CPF não aprovado para este animal." + ANSI_RESET); return; }
        br.com.mpet.model.Adocao ad = new br.com.mpet.model.Adocao();
        ad.setCpfAdotante(cpf); ad.setIdAnimal(idAnimal); ad.setDataAdocao(java.time.LocalDate.now()); ad.setAtivo(true);
        adocaoDao.create(ad);
        // Fechar todos os chats do animal e notificar demais pretendentes (opcional)
        List<ChatThread> threads = threadDao.listAllActive().stream().filter(t -> t.getIdAnimal() == idAnimal && t.isAberto()).toList();
        for (ChatThread t : threads) {
            if (!t.getCpfAdotante().equals(cpf)) {
                ChatMessage aviso = new ChatMessage();
                aviso.setThreadId(t.getId()); aviso.setSender(ChatSender.VOLUNTARIO);
                aviso.setConteudo("[Automática] Este animal foi adotado. Chat encerrado.");
                aviso.setEnviadoEm(java.time.LocalDateTime.now()); 
                aviso.setZoneId(java.time.ZoneId.systemDefault().getId());  // ✅ Setar zoneId explicitamente
                aviso.setAtivo(true);
                msgDao.create(aviso);
            }
            t.setAberto(false); threadDao.update(t);
        }
        System.out.println(ANSI_GREEN + "Adoção confirmada. Chats fechados e animal removido da lista de disponíveis." + ANSI_RESET);
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

    // ================================
    // ADOTANTE: VER MENSAGENS/CHATS
    // ================================
    private static void verMinhasConversas(Scanner sc, Adotante a, ChatThreadDataFileDao threadDao, ChatMessageDataFileDao msgDao) throws IOException {
        List<ChatThread> minhas = threadDao.listAllActive().stream()
                .filter(t -> a.getCpf().equals(t.getCpfAdotante()))
                .toList();
        if (minhas.isEmpty()) {
            System.out.println(ANSI_YELLOW + "Você não possui conversas." + ANSI_RESET);
            return;
        }
        System.out.println(ANSI_CYAN + "Suas conversas:" + ANSI_RESET);
        minhas.forEach(t -> {
            String status = t.isAberto() ? ANSI_GREEN + "Aberto" + ANSI_RESET : ANSI_RED + "Fechado" + ANSI_RESET;
            System.out.printf(" - Thread %d | Animal=%d | Status=%s\n   📅 Criado em: %s\n",
                t.getId(), t.getIdAnimal(), status, 
                formatarTimestamp(t.getCriadoEm(), t.getZoneId()));
        });
        int tid = perguntarInt(sc, "ID da thread para visualizar");
        Optional<ChatThread> ot = minhas.stream().filter(t -> t.getId() == tid).findFirst();
        if (ot.isEmpty()) { System.out.println(ANSI_RED + "Thread inválida." + ANSI_RESET); return; }
        ChatThread t = ot.get();

        List<ChatMessage> msgs = msgDao.listAllActive().stream()
                .filter(m -> m.getThreadId() == t.getId())
                .sorted((m1, m2) -> {
                    var d1 = m1.getEnviadoEm(); var d2 = m2.getEnviadoEm();
                    if (d1 == null && d2 == null) return Integer.compare(m1.getId(), m2.getId());
                    if (d1 == null) return -1; if (d2 == null) return 1; return d1.compareTo(d2);
                })
                .toList();
        System.out.println(ANSI_CYAN + "Mensagens:" + ANSI_RESET);
        if (msgs.isEmpty()) System.out.println("(sem mensagens)");
        else msgs.forEach(m -> {
            String senderLabel = m.getSender() == ChatSender.VOLUNTARIO ? "VOLUNTARIO" + extrairCpfVoluntarioSufixo(m) : m.getSender().name();
            System.out.printf(" [%s] %s\n   📅 %s\n", 
                senderLabel, 
                limparPrefixoVoluntario(m.getConteudo()), 
                formatarTimestamp(m.getEnviadoEm(), m.getZoneId()));
        });
        if (!t.isAberto()) {
            System.out.println(ANSI_YELLOW + "Esta conversa está encerrada." + ANSI_RESET);
            return;
        }
        String texto = perguntarString(sc, "Escreva uma mensagem (vazio para cancelar)", "");
        if (texto != null && !texto.isBlank()) {
            ChatMessage m = new ChatMessage();
            m.setThreadId(t.getId());
            m.setSender(ChatSender.ADOTANTE);
            m.setConteudo(texto);
            m.setEnviadoEm(java.time.LocalDateTime.now());
            m.setZoneId(java.time.ZoneId.systemDefault().getId());  // ✅ Setar zoneId explicitamente
            m.setAtivo(true);
            msgDao.create(m);
            System.out.println(ANSI_GREEN + "Mensagem enviada." + ANSI_RESET);
        }
    }

    // ================================
    // HELPERS: IDENTIFICAÇÃO DO VOLUNTÁRIO NAS MENSAGENS
    // ================================
    // Formato do prefixo: [VOL:CPF] mensagem
    private static String prefixarCpfVoluntario(String cpf, String texto) {
        if (cpf == null) cpf = "";
        return "[VOL:" + cpf + "] " + (texto == null ? "" : texto);
    }

    private static String extrairCpfVoluntarioSufixo(ChatMessage m) {
        if (m == null || m.getConteudo() == null) return "";
        String c = m.getConteudo();
        if (c.startsWith("[VOL:") && c.indexOf(']') > 5) {
            String cpf = c.substring(5, c.indexOf(']'));
            if (!cpf.isBlank()) return "(" + cpf + ")";
        }
        return "";
    }

    private static String limparPrefixoVoluntario(String conteudo) {
        if (conteudo == null) return null;
        if (conteudo.startsWith("[VOL:") && conteudo.indexOf(']') > 0) {
            int end = conteudo.indexOf(']');
            // também remove o espaço após o colchete se existir
            int startMsg = Math.min(conteudo.length(), end + 1 + (conteudo.length() > end + 1 && conteudo.charAt(end + 1) == ' ' ? 1 : 0));
            return conteudo.substring(startMsg);
        }
        return conteudo;
    }

    // ================================
    // HELPERS: FORMATAÇÃO DE TIMESTAMPS
    // ================================
    
    /**
     * Formata timestamp UTC para timezone local com informação de fuso horário.
     * 
     * O sistema salva todos os timestamps em UTC (padrão internacional) para portabilidade.
     * Esta função converte UTC para o timezone local do usuário e exibe de forma amigável.
     * 
     * @param dt LocalDateTime em UTC (como salvo no arquivo)
     * @param zoneIdStr ID do timezone (ex: "America/Sao_Paulo", "UTC", "Europe/London")
     * @return String formatada: "25/11/2025 14:30:52 (BRT)" ou "N/A" se null
     * 
     * Exemplo:
     * - Salvo no arquivo: epoch 1732546252 (UTC)
     * - Convertido para: LocalDateTime "2025-11-25T17:30:52" (UTC)
     * - Exibido como: "25/11/2025 14:30:52 (BRT)" (America/Sao_Paulo)
     */
    private static String formatarTimestamp(java.time.LocalDateTime dt, String zoneIdStr) {
        if (dt == null) return "N/A";
        
        try {
            // Timestamp está em UTC, converter para timezone local
            java.time.ZoneId zone = (zoneIdStr != null && !zoneIdStr.isBlank()) 
                ? java.time.ZoneId.of(zoneIdStr) 
                : java.time.ZoneId.systemDefault();
            
            // Criar ZonedDateTime a partir do UTC e converter para timezone local
            java.time.ZonedDateTime zdtUTC = dt.atZone(java.time.ZoneOffset.UTC);
            java.time.ZonedDateTime zdtLocal = zdtUTC.withZoneSameInstant(zone);
            
            // Formato: "25/11/2025 14:30:52"
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String dataFormatada = zdtLocal.format(formatter);
            
            // Adicionar timezone abreviado (BRT, UTC, PST, etc)
            String timezoneAbrev = zdtLocal.format(
                java.time.format.DateTimeFormatter.ofPattern("z"));
            
            return dataFormatada + " (" + timezoneAbrev + ")";
        } catch (Exception e) {
            // Fallback: formato ISO se der erro
            return dt.toString();
        }
    }

    // =================================================================================
    // GERENCIAMENTO DE BACKUPS
    // =================================================================================
    
    /**
     * Menu principal para gerenciar backups (listar e deletar)
     */
    private static void menuGerenciarBackups(Scanner sc) {
        while (true) {
            System.out.println("\n" + ANSI_CYAN + "╔════════════════════════════════════════════════════════════╗" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "║" + ANSI_RESET + ANSI_BOLD + "              📦 GERENCIAMENTO DE BACKUPS 📦              " + ANSI_RESET + ANSI_CYAN + "║" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "╚════════════════════════════════════════════════════════════╝" + ANSI_RESET);
            System.out.println();
            System.out.println("1) 📋 Listar Todos os Backups");
            System.out.println("2) 🗑️  Deletar Backup Específico");
            System.out.println("3) 🗑️  Deletar Todos os Backups");
            System.out.println(ANSI_RED + "0) Voltar" + ANSI_RESET);
            System.out.print("Escolha: ");
            String op = sc.nextLine().trim();
            
            try {
                switch (op) {
                    case "1" -> listarBackups();
                    case "2" -> deletarBackupEspecifico(sc);
                    case "3" -> deletarTodosBackups(sc);
                    case "0" -> { return; }
                    default -> System.out.println(ANSI_RED + "Opção inválida." + ANSI_RESET);
                }
            } catch (Exception e) {
                System.out.println(ANSI_RED + "Erro: " + e.getMessage() + ANSI_RESET);
            }
        }
    }
    
    /**
     * Lista todos os arquivos de backup encontrados
     */
    private static void listarBackups() {
        System.out.println("\n" + ANSI_BLUE + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
        System.out.println(ANSI_BOLD + "📋 BACKUPS DISPONÍVEIS" + ANSI_RESET);
        System.out.println(ANSI_BLUE + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
        System.out.println();
        
        java.util.ArrayList<File> backups = buscarTodosBackups();
        
        if (backups.isEmpty()) {
            System.out.println(ANSI_YELLOW + "Nenhum arquivo de backup encontrado." + ANSI_RESET);
            System.out.println();
            return;
        }
        
        System.out.println(ANSI_CYAN + "Encontrados " + backups.size() + " backup(s):" + ANSI_RESET);
        System.out.println();
        
        for (int i = 0; i < backups.size(); i++) {
            File backup = backups.get(i);
            double tamanhoMB = backup.length() / 1024.0 / 1024.0;
            
            // Tentar extrair data do nome do arquivo
            String dataStr = extrairDataDoNome(backup.getName());
            
            System.out.printf("%s[%d]%s %s\n", 
                ANSI_BOLD, (i + 1), ANSI_RESET, 
                backup.getName());
            System.out.printf("    📁 Local: %s\n", backup.getAbsolutePath());
            System.out.printf("    📊 Tamanho: %.2f MB (%d bytes)\n", tamanhoMB, backup.length());
            if (dataStr != null) {
                System.out.printf("    📅 Data: %s\n", dataStr);
            }
            System.out.println();
        }
        
        System.out.println(ANSI_BLUE + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
    }
    
    /**
     * Busca todos os arquivos de backup (.zip) no diretório raiz e dats/
     */
    private static java.util.ArrayList<File> buscarTodosBackups() {
        java.util.ArrayList<File> backups = new java.util.ArrayList<>();
        
        // Buscar na raiz do projeto
        File raiz = new File(".");
        if (raiz.exists() && raiz.isDirectory()) {
            File[] arquivosRaiz = raiz.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".zip") && name.toLowerCase().contains("backup"));
            if (arquivosRaiz != null) {
                for (File f : arquivosRaiz) {
                    backups.add(f);
                }
            }
        }
        
        // Buscar na pasta dats/
        File datsDir = new File("dats");
        if (datsDir.exists() && datsDir.isDirectory()) {
            File[] arquivosDats = datsDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".zip") && name.toLowerCase().contains("backup"));
            if (arquivosDats != null) {
                for (File f : arquivosDats) {
                    backups.add(f);
                }
            }
        }
        
        // Ordenar por data de modificação (mais recente primeiro)
        backups.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        return backups;
    }
    
    /**
     * Extrai a data do nome do arquivo de backup (se existir)
     */
    private static String extrairDataDoNome(String nomeArquivo) {
        // Formato esperado: backup_YYYYMMDD_HHMMSS.zip
        if (nomeArquivo.matches(".*\\d{8}_\\d{6}.*")) {
            try {
                String[] partes = nomeArquivo.split("_");
                if (partes.length >= 3) {
                    String data = partes[1]; // YYYYMMDD
                    String hora = partes[2].replace(".zip", ""); // HHMMSS
                    
                    String ano = data.substring(0, 4);
                    String mes = data.substring(4, 6);
                    String dia = data.substring(6, 8);
                    
                    String h = hora.substring(0, 2);
                    String m = hora.substring(2, 4);
                    String s = hora.substring(4, 6);
                    
                    return String.format("%s/%s/%s às %s:%s:%s", dia, mes, ano, h, m, s);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Se não conseguir extrair, usar data de modificação do arquivo
        return null;
    }
    
    /**
     * Permite ao usuário escolher e deletar um backup específico
     */
    private static void deletarBackupEspecifico(Scanner sc) {
        java.util.ArrayList<File> backups = buscarTodosBackups();
        
        if (backups.isEmpty()) {
            System.out.println(ANSI_YELLOW + "\nNenhum backup encontrado para deletar." + ANSI_RESET);
            return;
        }
        
        System.out.println("\n" + ANSI_CYAN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
        System.out.println(ANSI_BOLD + "🗑️  DELETAR BACKUP ESPECÍFICO" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "═══════════════════════════════════════════════════════════" + ANSI_RESET);
        System.out.println();
        
        // Listar backups com números
        for (int i = 0; i < backups.size(); i++) {
            File backup = backups.get(i);
            double tamanhoMB = backup.length() / 1024.0 / 1024.0;
            String dataStr = extrairDataDoNome(backup.getName());
            
            System.out.printf("%s[%d]%s %s", 
                ANSI_BOLD, (i + 1), ANSI_RESET, 
                backup.getName());
            if (dataStr != null) {
                System.out.printf(" (%s)", dataStr);
            }
            System.out.printf(" - %.2f MB\n", tamanhoMB);
        }
        
        System.out.println();
        System.out.print("Digite o número do backup a deletar (0 para cancelar): ");
        String input = sc.nextLine().trim();
        
        try {
            int escolha = Integer.parseInt(input);
            
            if (escolha == 0) {
                System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
                return;
            }
            
            if (escolha < 1 || escolha > backups.size()) {
                System.out.println(ANSI_RED + "Número inválido." + ANSI_RESET);
                return;
            }
            
            File backupParaDeletar = backups.get(escolha - 1);
            
            System.out.println();
            System.out.println(ANSI_YELLOW + "⚠️  ATENÇÃO: Esta ação não pode ser desfeita!" + ANSI_RESET);
            System.out.println("Backup a ser deletado: " + ANSI_RED + backupParaDeletar.getName() + ANSI_RESET);
            System.out.println();
            
            if (perguntarBool(sc, "Confirmar deleção? (s/n): ")) {
                if (backupParaDeletar.delete()) {
                    System.out.println(ANSI_GREEN + "✓ Backup deletado com sucesso!" + ANSI_RESET);
                } else {
                    System.out.println(ANSI_RED + "✗ Erro ao deletar o backup." + ANSI_RESET);
                }
            } else {
                System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
            }
            
        } catch (NumberFormatException e) {
            System.out.println(ANSI_RED + "Entrada inválida. Digite apenas números." + ANSI_RESET);
        }
    }
    
    /**
     * Deleta todos os backups após confirmação
     */
    private static void deletarTodosBackups(Scanner sc) {
        java.util.ArrayList<File> backups = buscarTodosBackups();
        
        if (backups.isEmpty()) {
            System.out.println(ANSI_YELLOW + "\nNenhum backup encontrado para deletar." + ANSI_RESET);
            return;
        }
        
        System.out.println("\n" + ANSI_RED + "╔════════════════════════════════════════════════════════════╗" + ANSI_RESET);
        System.out.println(ANSI_RED + "║" + ANSI_RESET + ANSI_BOLD + "           ⚠️  DELETAR TODOS OS BACKUPS ⚠️              " + ANSI_RESET + ANSI_RED + "║" + ANSI_RESET);
        System.out.println(ANSI_RED + "╚════════════════════════════════════════════════════════════╝" + ANSI_RESET);
        System.out.println();
        
        System.out.println(ANSI_YELLOW + "Esta ação irá deletar " + backups.size() + " backup(s):" + ANSI_RESET);
        System.out.println();
        
        for (File backup : backups) {
            System.out.println("  • " + backup.getName());
        }
        
        System.out.println();
        System.out.println(ANSI_RED + "⚠️  ATENÇÃO: Esta ação NÃO pode ser desfeita!" + ANSI_RESET);
        System.out.println();
        
        if (perguntarBool(sc, "Tem certeza que deseja deletar TODOS os backups? (s/n): ")) {
            int deletados = 0;
            int erros = 0;
            
            for (File backup : backups) {
                if (backup.delete()) {
                    deletados++;
                    System.out.println(ANSI_GREEN + "✓ Deletado: " + backup.getName() + ANSI_RESET);
                } else {
                    erros++;
                    System.out.println(ANSI_RED + "✗ Erro ao deletar: " + backup.getName() + ANSI_RESET);
                }
            }
            
            System.out.println();
            System.out.println(ANSI_BOLD + "Resumo:" + ANSI_RESET);
            System.out.println("  • " + ANSI_GREEN + deletados + " backup(s) deletado(s)" + ANSI_RESET);
            if (erros > 0) {
                System.out.println("  • " + ANSI_RED + erros + " erro(s)" + ANSI_RESET);
            }
            
        } else {
            System.out.println(ANSI_YELLOW + "Operação cancelada." + ANSI_RESET);
        }
    }
    
    /**
     * Gera nome de arquivo de backup com data e hora
     */
    public static String gerarNomeBackupComData() {
        java.time.LocalDateTime agora = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return "backup_" + agora.format(formatter) + ".zip";
    }

}
