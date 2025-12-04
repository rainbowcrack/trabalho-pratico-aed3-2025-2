package br.com.mpet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import br.com.mpet.dto.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import br.com.mpet.model.Adotante;
import br.com.mpet.model.Animal;
import br.com.mpet.model.Cachorro;
import br.com.mpet.model.Gato;
import br.com.mpet.model.ChatMessage;
import br.com.mpet.model.ChatSender;
import br.com.mpet.model.ChatThread;
import br.com.mpet.model.Interesse;
import br.com.mpet.model.InteresseStatus;
import br.com.mpet.model.Ong;
import br.com.mpet.model.Role;
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
    private Gson gson;
    private boolean debugEnabled;

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
        this.gson = new Gson();
        this.debugEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("MPET_DEBUG", "true"));
        
        this.server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        setupRoutes();
    }

    private void setupRoutes() {
        // Serve arquivos estáticos (HTML, CSS, JS) do classpath
        server.createContext("/", wrap(new StaticFileHandler(), "static"));

        // Rota de health check
        server.createContext("/api/health", wrap(new HealthHandler(), "/api/health"));

        // Autenticação
        server.createContext("/api/auth/login", wrap(new LoginHandler(), "/api/auth/login"));

        // Animais
        server.createContext("/api/animais", wrap(new AnimaisHandler(), "/api/animais"));

        // ONGs
        server.createContext("/api/ongs", wrap(new OngsHandler(), "/api/ongs"));

        // Adotantes
        server.createContext("/api/adotantes", wrap(new AdotantesHandler(), "/api/adotantes"));

        // Voluntários
        server.createContext("/api/voluntarios", wrap(new VoluntariosHandler(), "/api/voluntarios"));

        // Interesses
        server.createContext("/api/interesses", wrap(new InteressesHandler(), "/api/interesses"));

        // Chats
        server.createContext("/api/chats", wrap(new ChatsHandler(), "/api/chats"));

        // Mensagens de Chat
        server.createContext("/api/chat-messages", wrap(new ChatMessagesHandler(), "/api/chat-messages"));

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
    private HttpHandler wrap(HttpHandler handler, String name) {
        if (!debugEnabled) return handler;
        return new LoggingHandler(name, handler);
    }

    private class LoggingHandler implements HttpHandler {
        private final String name;
        private final HttpHandler delegate;

        LoggingHandler(String name, HttpHandler delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long start = System.nanoTime();
            exchange.setAttribute("startNanos", start);
            if (debugEnabled) {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();
                String ua = exchange.getRequestHeaders().getFirst("User-Agent");
                System.out.printf("[HTTP ►] %s %s%s  ua=%s  ctx=%s%n",
                        method,
                        path,
                        (query != null ? ("?" + query) : ""),
                        (ua != null ? ua : "-"),
                        name);
            }
            try {
                delegate.handle(exchange);
            } finally {
                if (debugEnabled) {
                    long end = System.nanoTime();
                    long ms = (end - start) / 1_000_000L;
                    System.out.printf("[HTTP ◄] handled in %dms  ctx=%s%n", ms, name);
                }
            }
        }
    }

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
                } else if (method.equals("PUT")) {
                    // PUT /api/animais/:id
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        int id = Integer.parseInt(parts[3]);
                        String body = readRequestBody(exchange);
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        
                        Optional<Animal> animalOpt = animalDao.read(id);
                        if (animalOpt.isEmpty()) {
                            sendJsonResponse(exchange, "{\"error\":\"Animal não encontrado\"}", 404);
                            return;
                        }
                        
                        Animal animal = animalOpt.get();
                        if (json.has("nome")) animal.setNome(json.get("nome").getAsString());
                        if (json.has("descricao")) animal.setDescricao(json.get("descricao").getAsString());
                        if (json.has("imageUrl")) animal.setImageUrl(json.get("imageUrl").getAsString());
                        if (json.has("idOng")) animal.setIdOng(json.get("idOng").getAsInt());
                        
                        animalDao.update(animal);
                        String response = "{\"success\":true,\"message\":\"Animal atualizado com sucesso\",\"id\":" + id + "}";
                        sendJsonResponse(exchange, response, 200);
                    }
                } else if (method.equals("DELETE")) {
                    // DELETE /api/animais/:id
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        int id = Integer.parseInt(parts[3]);
                        boolean deleted = animalDao.delete(id);
                        if (deleted) {
                            String response = "{\"success\":true,\"message\":\"Animal removido com sucesso\"}";
                            sendJsonResponse(exchange, response, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"Animal não encontrado\"}", 404);
                        }
                    }
                } else if (method.equals("POST")) {
                    // POST /api/animais - Criar novo animal (CACHORRO/GATO)
                    String body = readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                    if (!json.has("tipo") || !json.has("idOng") || !json.has("nome") || !json.has("porte")) {
                        sendJsonResponse(exchange, "{\"error\":\"Campos obrigatórios: tipo, idOng, nome, porte\"}", 400);
                        return;
                    }

                    String tipo = json.get("tipo").getAsString();
                    int idOng = json.get("idOng").getAsInt();
                    String nome = json.get("nome").getAsString();
                    String porteStr = json.get("porte").getAsString();

                    Animal novo;
                    if ("CACHORRO".equalsIgnoreCase(tipo)) {
                        Cachorro c = new Cachorro();
                        if (json.has("raca")) c.setRaca(json.get("raca").getAsString()); else c.setRaca("SRD");
                        if (json.has("nivelAdestramento")) {
                            try {
                                c.setNivelAdestramento(br.com.mpet.model.NivelAdestramento.valueOf(json.get("nivelAdestramento").getAsString()));
                            } catch (Exception ignore) { c.setNivelAdestramento(br.com.mpet.model.NivelAdestramento.NENHUM); }
                        } else {
                            c.setNivelAdestramento(br.com.mpet.model.NivelAdestramento.NENHUM);
                        }
                        if (json.has("seDaBemComCachorros")) c.setSeDaBemComCachorros(json.get("seDaBemComCachorros").getAsBoolean());
                        if (json.has("seDaBemComGatos")) c.setSeDaBemComGatos(json.get("seDaBemComGatos").getAsBoolean());
                        if (json.has("seDaBemComCriancas")) c.setSeDaBemComCriancas(json.get("seDaBemComCriancas").getAsBoolean());
                        novo = c;
                    } else if ("GATO".equalsIgnoreCase(tipo)) {
                        Gato g = new Gato();
                        if (json.has("raca")) g.setRaca(json.get("raca").getAsString()); else g.setRaca("SRD");
                        if (json.has("seDaBemComCachorros")) g.setSeDaBemComCachorros(json.get("seDaBemComCachorros").getAsBoolean());
                        if (json.has("seDaBemComGatos")) g.setSeDaBemComGatos(json.get("seDaBemComGatos").getAsBoolean());
                        if (json.has("seDaBemComCriancas")) g.setSeDaBemComCriancas(json.get("seDaBemComCriancas").getAsBoolean());
                        if (json.has("acessoExterior")) g.setAcessoExterior(json.get("acessoExterior").getAsBoolean());
                        if (json.has("possuiTelamento")) g.setPossuiTelamento(json.get("possuiTelamento").getAsBoolean());
                        novo = g;
                    } else {
                        sendJsonResponse(exchange, "{\"error\":\"Tipo inválido. Use CACHORRO ou GATO\"}", 400);
                        return;
                    }

                    // Campos comuns
                    novo.setIdOng(idOng);
                    novo.setNome(nome);
                    try {
                        novo.setPorte(br.com.mpet.model.Porte.valueOf(porteStr));
                    } catch (Exception e) {
                        sendJsonResponse(exchange, "{\"error\":\"Porte inválido. Use PEQUENO, MEDIO, GRANDE\"}", 400);
                        return;
                    }
                    if (json.has("sexo")) {
                        String s = json.get("sexo").getAsString();
                        novo.setSexo(s != null && !s.isEmpty() ? s.charAt(0) : 'U');
                    } else {
                        novo.setSexo('U');
                    }
                    if (json.has("vacinado")) novo.setVacinado(json.get("vacinado").getAsBoolean()); else novo.setVacinado(false);
                    if (json.has("descricao")) novo.setDescricao(json.get("descricao").getAsString());
                    if (json.has("imageUrl")) novo.setImageUrl(json.get("imageUrl").getAsString());
                    if (json.has("dataNascimentoAprox")) {
                        try {
                            novo.setDataNascimentoAprox(java.time.LocalDate.parse(json.get("dataNascimentoAprox").getAsString()));
                        } catch (Exception ignore) { /* ignora formato inválido */ }
                    }
                    novo.setAtivo(true);

                    // Persiste
                    Animal criado = animalDao.create(novo);
                    String response = animalToJson(criado);
                    sendJsonResponse(exchange, response, 201);
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
                } else if (exchange.getRequestMethod().equals("POST")) {
                    // Criar nova ONG
                    String body = readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    
                    String nome = json.get("nome").getAsString();
                    String cnpj = json.get("cnpj").getAsString();
                    String endereco = json.get("endereco").getAsString();
                    String telefone = json.get("telefone").getAsString();
                    
                    Ong ong = new Ong();
                    ong.setNome(nome);
                    ong.setCnpj(cnpj);
                    ong.setEndereco(endereco);
                    ong.setTelefone(telefone);
                    ong.setAtivo(true);
                    
                    ongDao.create(ong);
                    String response = "{\"success\":true,\"message\":\"ONG criada com sucesso\",\"nome\":\"" + nome + "\"}";
                    sendJsonResponse(exchange, response, 201);
                } else if (exchange.getRequestMethod().equals("PUT")) {
                    // PUT /api/ongs/:id
                    String path = exchange.getRequestURI().getPath();
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        int id = Integer.parseInt(parts[3]);
                        String body = readRequestBody(exchange);
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        
                        Optional<Ong> ongOpt = ongDao.read(id);
                        if (ongOpt.isEmpty()) {
                            sendJsonResponse(exchange, "{\"error\":\"ONG não encontrada\"}", 404);
                            return;
                        }
                        
                        Ong ong = ongOpt.get();
                        if (json.has("nome")) ong.setNome(json.get("nome").getAsString());
                        if (json.has("cnpj")) ong.setCnpj(json.get("cnpj").getAsString());
                        if (json.has("endereco")) ong.setEndereco(json.get("endereco").getAsString());
                        if (json.has("telefone")) ong.setTelefone(json.get("telefone").getAsString());
                        
                        ongDao.update(ong);
                        String response = "{\"success\":true,\"message\":\"ONG atualizada com sucesso\"}";
                        sendJsonResponse(exchange, response, 200);
                    }
                } else if (exchange.getRequestMethod().equals("DELETE")) {
                    // DELETE /api/ongs/:id
                    String path = exchange.getRequestURI().getPath();
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        int id = Integer.parseInt(parts[3]);
                        boolean deleted = ongDao.delete(id);
                        if (deleted) {
                            String response = "{\"success\":true,\"message\":\"ONG removida com sucesso\"}";
                            sendJsonResponse(exchange, response, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"ONG não encontrada\"}", 404);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}" , 500);
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
                        
                        // GET /api/adotantes/:cpf/interesses
                        if (parts.length > 4 && parts[4].equals("interesses")) {
                            List<Interesse> allInteresses = interesseDao.listAllActive();
                            List<Interesse> userInteresses = allInteresses.stream()
                                .filter(i -> i.getCpfAdotante().equals(cpf))
                                .toList();
                            String json = interessesToJson(userInteresses);
                            sendJsonResponse(exchange, json, 200);
                            return;
                        }
                        
                        // GET /api/adotantes/:cpf/adocoes
                        if (parts.length > 4 && parts[4].equals("adocoes")) {
                            List<br.com.mpet.model.Adocao> allAdocoes = adocaoDao.listAllActive();
                            List<br.com.mpet.model.Adocao> userAdocoes = allAdocoes.stream()
                                .filter(a -> a.getCpfAdotante().equals(cpf))
                                .toList();
                            String json = adocoesToJson(userAdocoes);
                            sendJsonResponse(exchange, json, 200);
                            return;
                        }
                        
                        // GET /api/adotantes/:cpf
                        Optional<Adotante> adotanteOpt = adotanteDao.read(cpf);
                        if (adotanteOpt.isPresent()) {
                            String json = adotanteToJson(adotanteOpt.get());
                            sendJsonResponse(exchange, json, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"Adotante não encontrado\"}", 404);
                        }
                    }
                } else if (method.equals("POST")) {
                    // Criar novo adotante
                    String body = readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    
                    String cpf = json.get("cpf").getAsString();
                    String senha = json.get("senha").getAsString();
                    String nomeCompleto = json.get("nomeCompleto").getAsString();
                    
                    Adotante adotante = new Adotante();
                    adotante.setCpf(cpf);
                    adotante.setSenha(senha);
                    adotante.setNomeCompleto(nomeCompleto);
                    
                    if (json.has("telefone")) adotante.setTelefone(json.get("telefone").getAsString());
                    if (json.has("dataNascimento")) {
                        adotante.setDataNascimento(LocalDate.parse(json.get("dataNascimento").getAsString()));
                    }
                    
                    adotanteDao.create(adotante);
                    String response = "{\"success\":true,\"message\":\"Adotante criado com sucesso\",\"cpf\":\"" + cpf + "\"}";
                    sendJsonResponse(exchange, response, 201);
                } else if (method.equals("PUT")) {
                    // PUT /api/adotantes/:cpf
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String cpf = parts[3];
                        String body = readRequestBody(exchange);
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        
                        Optional<Adotante> adotanteOpt = adotanteDao.read(cpf);
                        if (adotanteOpt.isEmpty()) {
                            sendJsonResponse(exchange, "{\"error\":\"Adotante não encontrado\"}", 404);
                            return;
                        }
                        
                        Adotante adotante = adotanteOpt.get();
                        if (json.has("nomeCompleto")) adotante.setNomeCompleto(json.get("nomeCompleto").getAsString());
                        if (json.has("telefone")) adotante.setTelefone(json.get("telefone").getAsString());
                        if (json.has("senha")) adotante.setSenha(json.get("senha").getAsString());
                        
                        adotanteDao.update(adotante);
                        String response = "{\"success\":true,\"message\":\"Adotante atualizado com sucesso\"}";
                        sendJsonResponse(exchange, response, 200);
                    }
                } else if (method.equals("DELETE")) {
                    // DELETE /api/adotantes/:cpf
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String cpf = parts[3];
                        boolean deleted = adotanteDao.delete(cpf);
                        if (deleted) {
                            String response = "{\"success\":true,\"message\":\"Adotante removido com sucesso\"}";
                            sendJsonResponse(exchange, response, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"Adotante não encontrado\"}", 404);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}" , 500);
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
                } else if (method.equals("POST")) {
                    // Criar novo voluntário
                    String body = readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    
                    String cpf = json.get("cpf").getAsString();
                    String senha = json.get("senha").getAsString();
                    String nome = json.get("nome").getAsString();
                    int idOng = json.get("idOng").getAsInt();
                    
                    Voluntario voluntario = new Voluntario();
                    voluntario.setCpf(cpf);
                    voluntario.setSenha(senha);
                    voluntario.setNome(nome);
                    voluntario.setIdOng(idOng);
                    
                    if (json.has("telefone")) voluntario.setTelefone(json.get("telefone").getAsString());
                    if (json.has("endereco")) voluntario.setEndereco(json.get("endereco").getAsString());
                    if (json.has("cargo")) {
                        voluntario.setCargo(Role.valueOf(json.get("cargo").getAsString()));
                    }
                    
                    voluntarioDao.create(voluntario);
                    String response = "{\"success\":true,\"message\":\"Voluntário criado com sucesso\",\"cpf\":\"" + cpf + "\"}";
                    sendJsonResponse(exchange, response, 201);
                } else if (method.equals("PUT")) {
                    // PUT /api/voluntarios/:cpf
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String cpf = parts[3];
                        String body = readRequestBody(exchange);
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        
                        Optional<Voluntario> voluntarioOpt = voluntarioDao.read(cpf);
                        if (voluntarioOpt.isEmpty()) {
                            sendJsonResponse(exchange, "{\"error\":\"Voluntário não encontrado\"}", 404);
                            return;
                        }
                        
                        Voluntario voluntario = voluntarioOpt.get();
                        if (json.has("nome")) voluntario.setNome(json.get("nome").getAsString());
                        if (json.has("telefone")) voluntario.setTelefone(json.get("telefone").getAsString());
                        if (json.has("senha")) voluntario.setSenha(json.get("senha").getAsString());
                        if (json.has("endereco")) voluntario.setEndereco(json.get("endereco").getAsString());
                        
                        voluntarioDao.update(voluntario);
                        String response = "{\"success\":true,\"message\":\"Voluntário atualizado com sucesso\"}";
                        sendJsonResponse(exchange, response, 200);
                    }
                } else if (method.equals("DELETE")) {
                    // DELETE /api/voluntarios/:cpf
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        String cpf = parts[3];
                        boolean deleted = voluntarioDao.delete(cpf);
                        if (deleted) {
                            String response = "{\"success\":true,\"message\":\"Voluntário removido com sucesso\"}";
                            sendJsonResponse(exchange, response, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"Voluntário não encontrado\"}", 404);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}" , 500);
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
                } else if (exchange.getRequestMethod().equals("POST")) {
                    // Registrar novo interesse
                    String body = readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    
                    String cpfAdotante = json.get("cpfAdotante").getAsString();
                    int idAnimal = json.get("idAnimal").getAsInt();
                    
                    Interesse interesse = new Interesse();
                    interesse.setCpfAdotante(cpfAdotante);
                    interesse.setIdAnimal(idAnimal);
                    interesse.setStatus(InteresseStatus.PENDENTE);
                    
                    interesseDao.create(interesse);
                    String response = "{\"success\":true,\"message\":\"Interesse registrado com sucesso\",\"cpfAdotante\":\"" + cpfAdotante + "\",\"idAnimal\":" + idAnimal + "}";
                    sendJsonResponse(exchange, response, 201);
                } else if (exchange.getRequestMethod().equals("PUT")) {
                    // PUT /api/interesses/:id - aprovar/recusar interesse
                    String path = exchange.getRequestURI().getPath();
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        int id = Integer.parseInt(parts[3]);
                        String body = readRequestBody(exchange);
                        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                        
                        Optional<Interesse> interesseOpt = interesseDao.read(id);
                        if (interesseOpt.isEmpty()) {
                            sendJsonResponse(exchange, "{\"error\":\"Interesse não encontrado\"}", 404);
                            return;
                        }
                        
                        Interesse interesse = interesseOpt.get();
                        if (json.has("status")) {
                            String statusStr = json.get("status").getAsString();
                            interesse.setStatus(InteresseStatus.valueOf(statusStr));
                        }
                        
                        interesseDao.update(interesse);
                        String response = "{\"success\":true,\"message\":\"Interesse atualizado com sucesso\",\"status\":\"" + interesse.getStatus() + "\"}";
                        sendJsonResponse(exchange, response, 200);
                    }
                } else if (exchange.getRequestMethod().equals("DELETE")) {
                    // DELETE /api/interesses/:id
                    String path = exchange.getRequestURI().getPath();
                    String[] parts = path.split("/");
                    if (parts.length > 3) {
                        int id = Integer.parseInt(parts[3]);
                        boolean deleted = interesseDao.delete(id);
                        if (deleted) {
                            String response = "{\"success\":true,\"message\":\"Interesse removido com sucesso\"}";
                            sendJsonResponse(exchange, response, 200);
                        } else {
                            sendJsonResponse(exchange, "{\"error\":\"Interesse não encontrado\"}", 404);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendJsonResponse(exchange, "{\"error\":\"" + e.getMessage() + "\"}" , 500);
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

            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();

            try {
                if (method.equals("GET")) {
                    String[] parts = path.split("/");
                    
                    if (parts.length > 3 && parts[3].matches("\\d+")) {
                        // GET /api/chats/:threadId ou /api/chats/:threadId/messages
                        int threadId = Integer.parseInt(parts[3]);
                        
                        if (parts.length > 4 && parts[4].equals("messages")) {
                            // GET /api/chats/:threadId/messages - Lista mensagens da thread
                            List<ChatMessage> allMessages = chatMsgDao.listAllActive();
                            List<ChatMessage> threadMessages = allMessages.stream()
                                .filter(m -> m.getThreadId() == threadId)
                                .toList();
                            String json = messagesToJson(threadMessages);
                            sendJsonResponse(exchange, json, 200);
                        } else {
                            // GET /api/chats/:threadId - Busca thread específica
                            Optional<ChatThread> threadOpt = chatThreadDao.read(threadId);
                            if (threadOpt.isPresent()) {
                                String json = chatToJson(threadOpt.get());
                                sendJsonResponse(exchange, json, 200);
                            } else {
                                sendJsonResponse(exchange, "{\"error\":\"Thread não encontrada\"}", 404);
                            }
                        }
                    } else {
                        // GET /api/chats - Lista todas threads (com filtros opcionais)
                        List<ChatThread> threads = chatThreadDao.listAllActive();
                        
                        // Filtrar por cpfAdotante se fornecido
                        if (query != null && query.contains("cpfAdotante=")) {
                            String cpf = extractQueryParam(query, "cpfAdotante");
                            threads = threads.stream()
                                .filter(t -> t.getCpfAdotante().equals(cpf))
                                .toList();
                        }
                        
                        // Filtrar por idAnimal se fornecido
                        if (query != null && query.contains("idAnimal=")) {
                            String idStr = extractQueryParam(query, "idAnimal");
                            int idAnimal = Integer.parseInt(idStr);
                            threads = threads.stream()
                                .filter(t -> t.getIdAnimal() == idAnimal)
                                .toList();
                        }
                        
                        String json = chatsToJson(threads);
                        sendJsonResponse(exchange, json, 200);
                    }
                } else if (method.equals("POST")) {
                    // POST /api/chats - Criar nova thread
                    String body = readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    
                    int idAnimal = json.get("idAnimal").getAsInt();
                    String cpfAdotante = json.get("cpfAdotante").getAsString();
                    
                    // Verificar se já existe thread para este animal + adotante
                    List<ChatThread> existing = chatThreadDao.listAllActive().stream()
                        .filter(t -> t.getIdAnimal() == idAnimal && t.getCpfAdotante().equals(cpfAdotante))
                        .toList();
                    
                    if (!existing.isEmpty()) {
                        // Retorna a thread existente
                        String response = chatToJson(existing.get(0));
                        sendJsonResponse(exchange, response, 200);
                        return;
                    }
                    
                    ChatThread thread = new ChatThread();
                    thread.setIdAnimal(idAnimal);
                    thread.setCpfAdotante(cpfAdotante);
                    thread.setAberto(true);
                    thread.setCriadoEm(java.time.LocalDateTime.now());
                    thread.setZoneId("America/Sao_Paulo");
                    
                    chatThreadDao.create(thread);
                    String response = chatToJson(thread);
                    sendJsonResponse(exchange, response, 201);
                    
                } else if (method.equals("PUT")) {
                    // PUT /api/chats/:threadId/close - Fechar thread
                    String[] parts = path.split("/");
                    if (parts.length > 4 && parts[3].matches("\\d+") && parts[4].equals("close")) {
                        int threadId = Integer.parseInt(parts[3]);
                        Optional<ChatThread> threadOpt = chatThreadDao.read(threadId);
                        
                        if (threadOpt.isEmpty()) {
                            sendJsonResponse(exchange, "{\"error\":\"Thread não encontrada\"}", 404);
                            return;
                        }
                        
                        ChatThread thread = threadOpt.get();
                        thread.setAberto(false);
                        chatThreadDao.update(thread);
                        
                        String response = "{\"success\":true,\"message\":\"Thread fechada com sucesso\"}";
                        sendJsonResponse(exchange, response, 200);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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

            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();

            try {
                if (method.equals("GET")) {
                    List<ChatMessage> messages = chatMsgDao.listAllActive();
                    
                    // Filtrar por threadId se fornecido
                    if (query != null && query.contains("threadId=")) {
                        String threadIdStr = extractQueryParam(query, "threadId");
                        int threadId = Integer.parseInt(threadIdStr);
                        messages = messages.stream()
                            .filter(m -> m.getThreadId() == threadId)
                            .toList();
                    }
                    
                    String json = messagesToJson(messages);
                    sendJsonResponse(exchange, json, 200);
                    
                } else if (method.equals("POST")) {
                    // POST /api/chat-messages - Enviar nova mensagem
                    String body = readRequestBody(exchange);
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    
                    int threadId = json.get("threadId").getAsInt();
                    String senderStr = json.get("sender").getAsString();
                    String conteudo = json.get("conteudo").getAsString();
                    
                    // Verificar se thread existe e está aberta
                    Optional<ChatThread> threadOpt = chatThreadDao.read(threadId);
                    if (threadOpt.isEmpty()) {
                        sendJsonResponse(exchange, "{\"error\":\"Thread não encontrada\"}", 404);
                        return;
                    }
                    
                    if (!threadOpt.get().isAberto()) {
                        sendJsonResponse(exchange, "{\"error\":\"Thread está fechada\"}", 400);
                        return;
                    }
                    
                    ChatMessage message = new ChatMessage();
                    message.setThreadId(threadId);
                    message.setSender(ChatSender.valueOf(senderStr));
                    message.setConteudo(conteudo);
                    message.setEnviadoEm(java.time.LocalDateTime.now());
                    message.setZoneId("America/Sao_Paulo");
                    message.setAtivo(true);
                    
                    chatMsgDao.create(message);
                    String response = messageToJson(message);
                    sendJsonResponse(exchange, response, 201);
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        try {
            if (debugEnabled) {
                Long start = (Long) exchange.getAttribute("startNanos");
                long tookMs = start != null ? (System.nanoTime() - start) / 1_000_000L : -1L;
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();
                System.out.printf("[HTTP ✔] %s %s%s -> %d (%d bytes) %s%n",
                        method,
                        path,
                        (query != null ? ("?" + query) : ""),
                        statusCode,
                        response.length,
                        (tookMs >= 0 ? (tookMs + "ms") : ""));
            }
        } catch (Exception ignore) { }
        exchange.close();
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        return reader.lines().collect(Collectors.joining());
    }

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
    
    private String extractQueryParam(String query, String paramName) {
        String[] params = query.split("&");
        for (String param : params) {
            String[] kv = param.split("=");
            if (kv.length == 2 && kv[0].equals(paramName)) {
                return kv[1];
            }
        }
        return null;
    }

    // ============ JSON CONVERTERS ============

    private String animalsToJson(List<Animal> animais) {
        List<AnimalDto> dtos = animais.stream()
            .map(AnimalDto::fromEntity)
            .collect(java.util.stream.Collectors.toList());
        return gson.toJson(dtos);
    }

    private String animalToJson(Animal a) {
        AnimalDto dto = AnimalDto.fromEntity(a);
        return gson.toJson(dto);
    }

    private String ongsToJson(List<Ong> ongs) {
        List<OngDto> dtos = ongs.stream()
            .map(OngDto::fromEntity)
            .collect(java.util.stream.Collectors.toList());
        return gson.toJson(dtos);
    }

    private String adotanteToJson(Adotante a) {
        AdotanteDto dto = AdotanteDto.fromEntity(a);
        return gson.toJson(dto);
    }

    private String voluntarioToJson(Voluntario v) {
        VoluntarioDto dto = VoluntarioDto.fromEntity(v);
        return gson.toJson(dto);
    }

    private String interessesToJson(List<Interesse> interesses) {
        List<InteresseDto> dtos = interesses.stream()
            .map(InteresseDto::fromEntity)
            .collect(java.util.stream.Collectors.toList());
        return gson.toJson(dtos);
    }
    
    private String adocoesToJson(List<br.com.mpet.model.Adocao> adocoes) {
        List<AdocaoDto> dtos = adocoes.stream()
            .map(AdocaoDto::fromEntity)
            .collect(java.util.stream.Collectors.toList());
        return gson.toJson(dtos);
    }

    private String chatToJson(ChatThread ct) {
        ChatThreadDto dto = ChatThreadDto.fromEntity(ct);
        return gson.toJson(dto);
    }
    
    private String chatsToJson(List<ChatThread> threads) {
        List<ChatThreadDto> dtos = threads.stream()
            .map(ChatThreadDto::fromEntity)
            .collect(java.util.stream.Collectors.toList());
        return gson.toJson(dtos);
    }

    private String messageToJson(ChatMessage m) {
        ChatMessageDto dto = ChatMessageDto.fromEntity(m);
        return gson.toJson(dto);
    }
    
    private String messagesToJson(List<ChatMessage> messages) {
        List<ChatMessageDto> dtos = messages.stream()
            .map(ChatMessageDto::fromEntity)
            .collect(java.util.stream.Collectors.toList());
        return gson.toJson(dtos);
    }

    // ============ STATIC FILE HANDLER ============

    /**
     * Handler para servir arquivos estáticos (HTML, CSS, JS)
     * do classpath em public/
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Se for raiz, redireciona para index.html
            if (path.equals("/") || path.equals("")) {
                path = "/pages/index.html";
            }
            
            // Tenta carregar do classpath em public/
            String resourcePath = "public" + path;
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            
            if (is == null) {
                // Arquivo não encontrado
                String notFound = "<html><body><h1>404 - Not Found</h1><p>Path: " + path + "</p></body></html>";
                byte[] response = notFound.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
                return;
            }
            
            // Determina Content-Type
            String contentType = getContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            
            // Lê e envia arquivo
            byte[] fileBytes = is.readAllBytes();
            exchange.sendResponseHeaders(200, fileBytes.length);
            exchange.getResponseBody().write(fileBytes);
            exchange.close();
            is.close();
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".gif")) return "image/gif";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
