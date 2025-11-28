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
    
    private static RestServer restServer;
    
    public static void main(String[] args) {
        System.out.println("DEBUG_START: main iniciada!");
        System.out.println("\n🚀 Iniciando PetMatch com servidor REST...\n");
        
        // Resolve diretório de dados
        File dataDir = resolveDataDir();
        
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            System.err.println("❌ Falha ao criar diretório de dados.");
            return;
        }
        
        System.out.println("📂 Diretório de dados: " + dataDir.getAbsolutePath());
        
        // Define arquivos de dados
        final byte VERSAO = 1;
        System.out.println("DEBUG: Definindo arquivos de dados...");
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
            // Inicia REST Server em thread separada
            try {
                restServer = new RestServer(8080, animalDao, ongDao, adotanteDao, voluntarioDao, adocaoDao, interesseDao, chatThreadDao, chatMsgDao);
                restServer.start();
                
                System.out.println("\n" + "=".repeat(60));
                System.out.println("✨ PetMatch está pronto!");
                System.out.println("=".repeat(60));
                System.out.println("🌐 Frontend:  http://localhost:8080/pages/index.html");
                System.out.println("🔌 API REST:  http://localhost:8080/api");
                System.out.println("=".repeat(60) + "\n");
                
                // Para manter o servidor rodando enquanto o CLI também funciona
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\n🛑 Encerrando servidor...");
                    if (restServer != null) {
                        restServer.stop();
                    }
                }));
                
                System.out.println("✅ Servidor iniciado com sucesso!\\n");
                
                // Mantém o processo em execução sem iniciar o CLI para evitar bloqueios
                // e garantir que o servidor REST continue respondendo.
                final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        if (restServer != null) restServer.stop();
                    } finally {
                        latch.countDown();
                    }
                }));
                try {
                    latch.await();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                System.err.println("❌ Erro ao iniciar servidor REST: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("❌ Erro ao iniciar aplicação: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
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
            if (cur.getName().equals("Codigo") && cur.getParentFile() != null) {
                return new File(cur.getParentFile(), "dats");
            }
            cur = cur.getParentFile();
        }
        
        // Padrão: usar "dats" na pasta atual
        return new File(wd, "dats");
    }
}
