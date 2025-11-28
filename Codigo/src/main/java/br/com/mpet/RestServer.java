package br.com.mpet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import br.com.mpet.model.Adotante;
import br.com.mpet.model.Animal;
import br.com.mpet.model.Cachorro;
import br.com.mpet.model.ChatMessage;
import br.com.mpet.model.ChatThread;
import br.com.mpet.model.Interesse;
import br.com.mpet.model.Ong;
import br.com.mpet.model.Voluntario;
import br.com.mpet.persistence.dao.AdocaoDataFileDao;
import br.com.mpet.persistence.dao.AdotanteDataFileDao;
import br.com.mpet.persistence.dao.AnimalDataFileDao;
import br.com.mpet.persistence.dao.ChatMessageDataFileDao;
import br.com.mpet.persistence.dao.ChatThreadDataFileDao;
import br.com.mpet.persistence.dao.InteresseDataFileDao;
import br.com.mpet.persistence.dao.OngDataFileDao;
import br.com.mpet.persistence.dao.VoluntarioDataFileDao;

/**
 * RestServer - Servidor HTTP REST para a API PetMatch
 * 
 * Endpoints disponíveis:
 * - GET /api/ongs - Lista todas as ONGs
 * - GET /api/animais - Lista todos os animais
 * - GET /api/animais/:id - Obtém animal por ID
 * - POST /api/auth/login - Login do usuário
 * - POST /api/adotantes - Criar adotante
 * - GET /api/adotantes/:cpf - Obter adotante por CPF
 * - E muito mais...
 */
public class RestServer {
    private HttpServer server;
    private int port;
    private AnimalDataFileDao animalDao;
    private OngDataFileDao ongDao;
    private AdotanteDataFileDao adotanteDao;
    private VoluntarioDataFileDao voluntarioDao;
    private AdocaoDataFileDao adocaoDao;
    private InteresseDataFileDao interesseDao;
    private ChatThreadDataFileDao chatThreadDao;
    private ChatMessageDataFileDao chatMsgDao;

    public RestServer(int port, 
                      AnimalDataFileDao animalDao,
                      OngDataFileDao ongDao,
                      AdotanteDataFileDao adotanteDao,
                      VoluntarioDataFileDao voluntarioDao,
                      AdocaoDataFileDao adocaoDao,
                      InteresseDataFileDao interesseDao,
                      ChatThreadDataFileDao chatThreadDao,
                      ChatMessageDataFileDao chatMsgDao) throws IOException {
        this.port = port;
        this.animalDao = animalDao;
        this.ongDao = ongDao;
        this.adotanteDao = adotanteDao;
        this.voluntarioDao = voluntarioDao;
        this.adocaoDao = adocaoDao;
        this.interesseDao = interesseDao;
        this.chatThreadDao = chatThreadDao;
        this.chatMsgDao = chatMsgDao;
        
        this.server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        setupRoutes();
    }

    private void setupRoutes() {
        // Rota de health check
        server.createContext("/api/health", new HealthHandler());

        // Autenticação
        server.createContext("/api/auth/login", new LoginHandler());

        // Animais
        server.createContext("/api/animais", new AnimaisHandler());

        // ONGs
        server.createContext("/api/ongs", new OngsHandler());

        // Adotantes
        server.createContext("/api/adotantes", new AdotantesHandler());

        // Voluntários
        server.createContext("/api/voluntarios", new VoluntariosHandler());

        // Interesses
        server.createContext("/api/interesses", new InteressesHandler());

        // Chats
        server.createContext("/api/chats", new ChatsHandler());

        // Mensagens de Chat
        server.createContext("/api/chat-messages", new ChatMessagesHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
    }

    public void start() {
        try {
            System.out.println("\n🔧 Iniciando HttpServer na porta " + port + "...");
            server.start();
            System.out.println("✅ HttpServer iniciado com sucesso!");
            System.out.println("✅ Servidor REST iniciado em http://localhost:" + port);
            System.out.println("   Frontend em: http://localhost:" + port + "/pages/index.html");
        } catch (Exception e) {
            System.err.println("❌ Erro ao iniciar HttpServer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha ao iniciar servidor", e);
        }
    }

    public void stop() {
        server.stop(0);
        System.out.println("❌ Servidor REST parado");
    }

    // ============ HANDLERS ============

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String response = "{\"status\":\"ok\",\"timestamp\":\"" + new Date() + "\"}";
            sendJsonResponse(exchange, response, 200);
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            if (!exchange.getRequestMethod().equals("POST")) {
                sendJsonResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> params = parseJson(body);
                String cpf = params.get("cpf");
                String senha = params.get("senha");

                if (cpf == null || senha == null) {
                    sendJsonResponse(exchange, "{\"error\":\"CPF e senha são obrigatórios\"}", 400);
                    return;
                }

                // Tenta login como admin
                if ("admin".equals(cpf) && "admin".equals(senha)) {
                    String response = "{" +
                        "\"success\":true," +
                        "\"token\":\"mock_token_admin\"," +
                        "\"user\":{" +
                        "\"cpf\":\"admin\"," +
                        "\"nome\":\"Administrador\"," +
                        "\"role\":\"ADMIN\"," +
                        "\"email\":\"admin@mpet.com\"" +
                        "}" +
                        "}";
                    sendJsonResponse(exchange, response, 200);
                    return;
                }

                // Tenta login como adotante
                Optional<Adotante> adotanteOpt = adotanteDao.read(cpf);
                if (adotanteOpt.isPresent()) {
                    Adotante a = adotanteOpt.get();
                    if (a.getSenha().equals(senha)) {
                        String response = "{" +
                            "\"success\":true," +
                            "\"token\":\"mock_token_" + cpf + "\"," +
                            "\"user\":{" +
                            "\"cpf\":\"" + a.getCpf() + "\"," +
                            "\"nome\":\"" + (a.getNomeCompleto() != null ? a.getNomeCompleto() : "Usuário") + "\"," +
                            "\"role\":\"ADOTANTE\"," +
                            "\"telefone\":\"" + (a.getTelefone() != null ? a.getTelefone() : "") + "\"" +
                            "}" +
                            "}";
                        sendJsonResponse(exchange, response, 200);
                        return;
                    }
                }

                // Tenta login como voluntário
                Optional<Voluntario> voluntarioOpt = voluntarioDao.read(cpf);
                if (voluntarioOpt.isPresent()) {
                    Voluntario v = voluntarioOpt.get();
                    if (v.getSenha().equals(senha)) {
                        String response = "{" +
                            "\"success\":true," +
                            "\"token\":\"mock_token_" + cpf + "\"," +
                            "\"user\":{" +
                            "\"cpf\":\"" + v.getCpf() + "\"," +
                            "\"nome\":\"" + (v.getNome() != null ? v.getNome() : "Voluntário") + "\"," +
                            "\"role\":\"VOLUNTARIO\"," +
                            "\"idOng\":" + v.getIdOng() + "," +
                            "\"cargo\":\"" + (v.getCargo() != null ? v.getCargo().name() : "ATENDIMENTO") + "\"," +
                            "\"telefone\":\"" + (v.getTelefone() != null ? v.getTelefone() : "") + "\"" +
                            "}" +
                            "}";
                        sendJsonResponse(exchange, response, 200);
                        return;
                    }
                }

                sendJsonResponse(exchange, "{\"error\":\"CPF ou senha incorretos\"}", 401);

            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    private class AnimaisHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                if (method.equals("GET")) {
                    if (path.equals("/api/animais")) {
                        // Lista todos os animais
                        List<Animal> animais = animalDao.listAllActive();
                        String json = animalsToJson(animais);
                        sendJsonResponse(exchange, json, 200);
                    } else {
                        // GET /api/animais/:id
                        String[] parts = path.split("/");
                        if (parts.length > 3) {
                            int id = Integer.parseInt(parts[3]);
                            Optional<Animal> animalOpt = animalDao.read(id);
                            if (animalOpt.isPresent()) {
                                String json = animalToJson(animalOpt.get());
                                sendJsonResponse(exchange, json, 200);
                            } else {
                                sendJsonResponse(exchange, "{\"error\":\"Animal não encontrado\"}", 404);
                            }
                        }
                    }
                } else if (method.equals("POST")) {
                    String body = readRequestBody(exchange);
                    // POST para criar animal
                    // Futuro: implementar criação via JSON
                    sendJsonResponse(exchange, "{\"error\":\"POST não implementado\"}", 501);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    private class OngsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            try {
                if (exchange.getRequestMethod().equals("GET")) {
                    List<Ong> ongs = ongDao.listAllActive();
                    String json = ongsToJson(ongs);
                    sendJsonResponse(exchange, json, 200);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    private class AdotantesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                if (method.equals("GET")) {
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String cpf = parts[3];
                        Optional<Adotante> adotanteOpt = adotanteDao.read(cpf);
                        if (adotanteOpt.isPresent()) {
                            String json = adotanteToJson(adotanteOpt.get());
                            sendJsonResponse(exchange, json, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"Adotante não encontrado\"}", 404);
                        }
                    }
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    private class VoluntariosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                if (method.equals("GET")) {
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String cpf = parts[3];
                        Optional<Voluntario> voluntarioOpt = voluntarioDao.read(cpf);
                        if (voluntarioOpt.isPresent()) {
                            String json = voluntarioToJson(voluntarioOpt.get());
                            sendJsonResponse(exchange, json, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"Voluntário não encontrado\"}", 404);
                        }
                    }
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    private class InteressesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            try {
                if (exchange.getRequestMethod().equals("GET")) {
                    List<Interesse> interesses = interesseDao.listAllActive();
                    String json = interessesToJson(interesses);
                    sendJsonResponse(exchange, json, 200);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    private class ChatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            try {
                if (exchange.getRequestMethod().equals("GET")) {
                    List<ChatThread> threads = chatThreadDao.listAllActive();
                    String json = chatsToJson(threads);
                    sendJsonResponse(exchange, json, 200);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    private class ChatMessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }

            try {
                if (exchange.getRequestMethod().equals("GET")) {
                    List<ChatMessage> messages = chatMsgDao.listAllActive();
                    String json = messagesToJson(messages);
                    sendJsonResponse(exchange, json, 200);
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    // ============ HELPER METHODS ============

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }

    private void sendJsonResponse(HttpExchange exchange, String json, int statusCode) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJson(String json) {
        // Parser simples JSON
        Map<String, String> map = new HashMap<>();
        json = json.replace("{", "").replace("}", "").replace("\"", "");
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    // ============ JSON CONVERTERS ============

    private String animalsToJson(List<Animal> animais) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < animais.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(animalToJson(animais.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String animalToJson(Animal a) {
        return "{" +
            "\"id\":" + a.getId() + "," +
            "\"idOng\":" + a.getIdOng() + "," +
            "\"nome\":\"" + escapeJson(a.getNome()) + "\"," +
            "\"tipo\":\"" + (a instanceof Cachorro ? "CACHORRO" : "GATO") + "\"," +
            "\"porte\":\"" + a.getPorte() + "\"," +
            "\"sexo\":\"" + a.getSexo() + "\"," +
            "\"vacinado\":" + a.isVacinado() + "," +
            "\"descricao\":\"" + escapeJson(a.getDescricao() != null ? a.getDescricao() : "") + "\"" +
            "}";
    }

    private String ongsToJson(List<Ong> ongs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ongs.size(); i++) {
            if (i > 0) sb.append(",");
            Ong o = ongs.get(i);
            sb.append("{" +
                "\"id\":" + o.getId() + "," +
                "\"nome\":\"" + escapeJson(o.getNome()) + "\"," +
                "\"cnpj\":\"" + o.getCnpj() + "\"," +
                "\"endereco\":\"" + escapeJson(o.getEndereco()) + "\"," +
                "\"telefone\":\"" + o.getTelefone() + "\"," +
                "\"ativo\":" + o.isAtivo() +
                "}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String adotanteToJson(Adotante a) {
        return "{" +
            "\"cpf\":\"" + a.getCpf() + "\"," +
            "\"nome\":\"" + escapeJson(a.getNomeCompleto() != null ? a.getNomeCompleto() : "") + "\"," +
            "\"telefone\":\"" + escapeJson(a.getTelefone() != null ? a.getTelefone() : "") + "\"" +
            "}";
    }

    private String voluntarioToJson(Voluntario v) {
        return "{" +
            "\"cpf\":\"" + v.getCpf() + "\"," +
            "\"nome\":\"" + escapeJson(v.getNome() != null ? v.getNome() : "") + "\"," +
            "\"telefone\":\"" + escapeJson(v.getTelefone() != null ? v.getTelefone() : "") + "\"," +
            "\"idOng\":" + v.getIdOng() + "," +
            "\"cargo\":\"" + (v.getCargo() != null ? v.getCargo().name() : "ATENDIMENTO") + "\"" +
            "}";
    }

    private String interessesToJson(List<Interesse> interesses) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < interesses.size(); i++) {
            if (i > 0) sb.append(",");
            Interesse in = interesses.get(i);
            sb.append("{" +
                "\"id\":" + in.getId() + "," +
                "\"cpfAdotante\":\"" + in.getCpfAdotante() + "\"," +
                "\"idAnimal\":" + in.getIdAnimal() + "," +
                "\"status\":\"" + in.getStatus() + "\"" +
                "}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String chatsToJson(List<ChatThread> threads) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < threads.size(); i++) {
            if (i > 0) sb.append(",");
            ChatThread ct = threads.get(i);
            sb.append("{" +
                "\"id\":" + ct.getId() + "," +
                "\"idAnimal\":" + ct.getIdAnimal() + "," +
                "\"cpfAdotante\":\"" + ct.getCpfAdotante() + "\"," +
                "\"aberto\":" + ct.isAberto() + "," +
                "\"criadoEm\":" + ct.getCriadoEm() +
                "}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String messagesToJson(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            ChatMessage m = messages.get(i);
            sb.append("{" +
                "\"id\":" + m.getId() + "," +
                "\"threadId\":" + m.getThreadId() + "," +
                "\"sender\":\"" + m.getSender().name() + "\"," +
                "\"conteudo\":\"" + escapeJson(m.getConteudo()) + "\"," +
                "\"enviadoEm\":" + m.getEnviadoEm() +
                "}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}
