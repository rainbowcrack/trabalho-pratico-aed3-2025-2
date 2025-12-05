package br.com.mpet;

import java.io.File;
import java.io.IOException;

import br.com.mpet.persistence.dao.AdocaoDataFileDao;
import br.com.mpet.persistence.dao.AdotanteDataFileDao;
import br.com.mpet.persistence.dao.AnimalDataFileDao;
import br.com.mpet.persistence.dao.ChatMessageDataFileDao;
import br.com.mpet.persistence.dao.ChatThreadDataFileDao;
import br.com.mpet.persistence.dao.InteresseDataFileDao;
import br.com.mpet.persistence.dao.OngDataFileDao;
import br.com.mpet.persistence.dao.VoluntarioDataFileDao;

/**
 * InterfaceWithServer - Inicia tanto o CLI quanto o REST Server
 * 
 * Uso:
 *   java -cp "target/classes" br.com.mpet.InterfaceWithServer
 * 
 * O servidor REST ficará disponível em http://localhost:8080
 * O frontend poderá ser servido através de um servidor HTTP separado
 */
public class InterfaceWithServer {
    
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
    
    // Funções de debug com cores
    private static void showSuccess(String message) {
        System.out.println(ANSI_GREEN + "✅ " + message + ANSI_RESET);
    }
    
    private static void showError(String message) {
        System.err.println(ANSI_RED + "❌ " + message + ANSI_RESET);
    }
    
    private static void showWarning(String message) {
        System.out.println(ANSI_YELLOW + "⚠️  " + message + ANSI_RESET);
    }
    
    private static void showInfo(String message) {
        System.out.println(ANSI_CYAN + "ℹ️  " + message + ANSI_RESET);
    }
    
    private static void showDebug(String message) {
        System.out.println(ANSI_DIM + "🔧 DEBUG: " + message + ANSI_RESET);
    }
    
    private static void showSplashServer() {
        System.out.println(ANSI_BOLD + ANSI_PURPLE + "\n" +
            "╔══════════════════════════════════════════════════════════════╗\n" +
            "║                    🐾 MPet REST Server 🐾                   ║\n" +
            "║                  Sistema de Adoção de Pets                  ║\n" +
            "║                     Servidor REST API                       ║\n" +
            "╚══════════════════════════════════════════════════════════════╝" +
            ANSI_RESET + "\n");
    }
    
    private static RestServer restServer;
    
    public static void main(String[] args) {
        showSplashServer();
        showInfo("Iniciando sistema MPet com servidor REST...");
        
        // Resolve diretório de dados
        showDebug("Resolvendo diretório de dados...");
        File dataDir = resolveDataDir();
        
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            showError("Falha ao criar diretório de dados.");
            return;
        }
        
        showSuccess("Diretório de dados configurado: " + dataDir.getAbsolutePath());
        
        // Define arquivos de dados
        final byte VERSAO = 1;
        showDebug("Configurando arquivos de dados (.dat e .idx)...");
        final File ANIMAIS_DATA_FILE = new File(dataDir, "animais.dat");
        final File ANIMAIS_IDX_FILE = new File(dataDir, "animais.dat.idx");
        final File ONGS_DATA_FILE = new File(dataDir, "ongs.dat");
        final File ONGS_IDX_FILE = new File(dataDir, "ongs.dat.idx");
        final File ADOTANTES_DATA_FILE = new File(dataDir, "adotantes.dat");
        final File ADOTANTES_IDX_FILE = new File(dataDir, "adotantes.dat.idx");
        final File VOLUNTARIOS_DATA_FILE = new File(dataDir, "voluntarios.dat");
        final File VOLUNTARIOS_IDX_FILE = new File(dataDir, "voluntarios.dat.idx");
        final File ADOCOES_DATA_FILE = new File(dataDir, "adocoes.dat");
        final File ADOCOES_IDX_FILE = new File(dataDir, "adocoes.dat.idx");
        final File INTERESSES_DATA_FILE = new File(dataDir, "interesses.dat");
        final File INTERESSES_IDX_FILE = new File(dataDir, "interesses.dat.idx");
        final File CHAT_THREADS_DATA_FILE = new File(dataDir, "chat_threads.dat");
        final File CHAT_THREADS_IDX_FILE = new File(dataDir, "chat_threads.dat.idx");
        final File CHAT_MSGS_DATA_FILE = new File(dataDir, "chat_msgs.dat");
        final File CHAT_MSGS_IDX_FILE = new File(dataDir, "chat_msgs.dat.idx");
        
        showDebug("Inicializando DAOs (Data Access Objects)...");
        try (
            AnimalDataFileDao animalDao = new AnimalDataFileDao(ANIMAIS_DATA_FILE, VERSAO);
            OngDataFileDao ongDao = new OngDataFileDao(ONGS_DATA_FILE, VERSAO);
            AdotanteDataFileDao adotanteDao = new AdotanteDataFileDao(ADOTANTES_DATA_FILE, VERSAO);
            VoluntarioDataFileDao voluntarioDao = new VoluntarioDataFileDao(VOLUNTARIOS_DATA_FILE, VERSAO);
            AdocaoDataFileDao adocaoDao = new AdocaoDataFileDao(ADOCOES_DATA_FILE, VERSAO);
            InteresseDataFileDao interesseDao = new InteresseDataFileDao(INTERESSES_DATA_FILE, VERSAO);
            ChatThreadDataFileDao chatThreadDao = new ChatThreadDataFileDao(CHAT_THREADS_DATA_FILE, VERSAO);
            ChatMessageDataFileDao chatMsgDao = new ChatMessageDataFileDao(CHAT_MSGS_DATA_FILE, VERSAO)
        ) {
            showSuccess("Todos os DAOs inicializados com sucesso!");
            
            // Inicia REST Server em thread separada
            showInfo("Configurando servidor REST na porta 8080...");
            try {
                restServer = new RestServer(8080, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao);
                showDebug("Servidor REST configurado, iniciando...");
                restServer.start();
                
                System.out.println("\n" + ANSI_BOLD + ANSI_GREEN + "=".repeat(70) + ANSI_RESET);
                System.out.println(ANSI_BOLD + ANSI_GREEN + "🎉 MPet REST Server está ONLINE! 🎉" + ANSI_RESET);
                System.out.println(ANSI_BOLD + ANSI_GREEN + "=".repeat(70) + ANSI_RESET);
                System.out.println(ANSI_BOLD + ANSI_CYAN + "🌐 Frontend Web:  " + ANSI_WHITE + "http://localhost:8080/pages/index.html" + ANSI_RESET);
                System.out.println(ANSI_BOLD + ANSI_BLUE + "🔌 API REST:      " + ANSI_WHITE + "http://localhost:8080/api" + ANSI_RESET);
                System.out.println(ANSI_BOLD + ANSI_PURPLE + "🐶 Endpoint Pets: " + ANSI_WHITE + "http://localhost:8080/api/animais" + ANSI_RESET);
                System.out.println(ANSI_BOLD + ANSI_PURPLE + "🐱 Chat System:   " + ANSI_WHITE + "http://localhost:8080/api/chat" + ANSI_RESET);
                System.out.println(ANSI_BOLD + ANSI_GREEN + "=".repeat(70) + ANSI_RESET + "\n");
                
                // Para manter o servidor rodando enquanto o CLI também funciona
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    showWarning("Encerrando servidor...");
                    if (restServer != null) {
                        restServer.stop();
                        showSuccess("Servidor encerrado com sucesso!");
                    }
                }));
                
                showSuccess("Servidor REST iniciado e funcionando!");
                showInfo("Pressione CTRL+C para encerrar o servidor.");
                
                // Mantém o processo em execução sem iniciar o CLI para evitar bloqueios
                // e garantir que o servidor REST continue respondendo.
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (restServer != null) {
                            showDebug("Parando servidor REST...");
                            restServer.stop();
                        }
                    } finally {
                        latch.countDown();
                    }
                }));
                try {
                    showDebug("Aguardando sinal de encerramento...");
                    latch.await();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    showWarning("Servidor interrompido por sinal externo.");
                }
            } catch (IOException e) {
                showError("Erro ao iniciar servidor REST: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            showError("Erro fatal ao iniciar aplicação: " + e.getMessage());
            showDebug("Stack trace completo:");
            e.printStackTrace();
        }
    }
    
    private static File resolveDataDir() {
        showDebug("Resolvendo diretório de dados...");
        File wd = new File(System.getProperty("user.dir"));
        showDebug("Diretório de trabalho atual: " + wd.getAbsolutePath());
        
        // Caso 1: executando a partir da raiz do repositório (existe a pasta Codigo aqui)
        if (new File(wd, "Codigo").exists()) {
            showDebug("Caso 1: Executando da raiz do repositório");
            return new File(wd, "dats");
        }
        
        // Caso 2: executando dentro da pasta Codigo
        if (wd.getName().equals("Codigo") && wd.getParentFile() != null) {
            showDebug("Caso 2: Executando de dentro da pasta Codigo");
            return new File(wd.getParentFile(), "dats");
        }
        
        // Caso 3: executando de subpastas como Codigo/target/classes
        File cur = wd;
        for (int i = 0; i < 6 && cur != null; i++) {
            if (cur.getName().equals("Codigo") && cur.getParentFile() != null) {
                showDebug("Caso 3: Encontrado pasta Codigo em " + cur.getAbsolutePath());
                return new File(cur.getParentFile(), "dats");
            }
            cur = cur.getParentFile();
        }
        
        // Padrão: usar "dats" na pasta atual
        showDebug("Caso padrão: Usando pasta 'dats' no diretório atual");
        return new File(wd, "dats");
    }
}
