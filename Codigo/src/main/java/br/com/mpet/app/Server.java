package br.com.mpet.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import br.com.mpet.api.Repositories;
import br.com.mpet.api.Json;
import br.com.mpet.domain.*;
import br.com.mpet.util.XorCipher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Server {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("PORT", "8080"));
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // Static files (frontend)
        httpServer.createContext("/", new StaticHandler("/public/pages/index.html"));
        httpServer.createContext("/assets/", new StaticHandler("/public"));

        // Health
        httpServer.createContext("/health", exchange -> {
            String body = "{\"status\":\"ok\"}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });

    // API handlers
        Repositories repos = new Repositories(Path.of("Codigo/data"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { repos.close(); } catch (Exception ignored) {}
        }));
    httpServer.createContext("/api/voluntarios", exchange -> handleVoluntarios(exchange, repos));
    httpServer.createContext("/api/adotantes", exchange -> handleAdotantes(exchange, repos));
    httpServer.createContext("/api/ongs", exchange -> handleOngs(exchange, repos));
    httpServer.createContext("/api/animais", exchange -> handleAnimais(exchange, repos));
    httpServer.createContext("/api/login", exchange -> handleLogin(exchange, repos));
    httpServer.createContext("/api/admin/vacuum", exchange -> handleVacuum(exchange, repos));

        httpServer.start();
        System.out.println("MPet backend running on http://localhost:" + port);
    }

    static class StaticHandler implements HttpHandler {
        private final String base;
        public StaticHandler(String base) { this.base = base; }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) path = base;
            Path res = Path.of("Codigo/src/main/resources" + (path.startsWith("/assets/") ? path.replace("/assets", "/public/assets") : base));
            if (!Files.exists(res)) {
                String notFound = "404";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) { os.write(notFound.getBytes()); }
                return;
            }
            String contentType = guessContentType(res);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            byte[] data = Files.readAllBytes(res);
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(data); }
        }
        private String guessContentType(Path res) throws IOException {
            String type = Files.probeContentType(res);
            if (type != null) return type;
            String name = res.getFileName().toString();
            if (name.endsWith(".html")) return "text/html; charset=utf-8";
            if (name.endsWith(".css")) return "text/css; charset=utf-8";
            if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
            return "application/octet-stream";
        }
    }

    // -------- Helpers
    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private static void respond(HttpExchange ex, int code, String body, String contentType) throws IOException {
        ex.getResponseHeaders().add("Content-Type", contentType);
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
    private static void respondJson(HttpExchange ex, int code, String json) throws IOException { respond(ex, code, json, "application/json; charset=utf-8"); }

    // -------- Voluntarios
    private static void handleVoluntarios(HttpExchange ex, Repositories r) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        if ("GET".equals(method)) {
            if (parts.length == 4) { // /api/voluntarios/{cpf}
                String cpf = parts[3];
                var v = r.voluntario.read(cpf);
                if (v.isEmpty()) { respondJson(ex, 404, "{\"error\":\"not found\"}"); return; }
                respondJson(ex, 200, toJson(v.get()));
            } else { // search by nome?nome=frag
                String query = ex.getRequestURI().getQuery();
                String nome = param(query, "nome");
                var list = r.voluntario.searchByNomeFragment(nome == null ? "" : nome);
                respondJson(ex, 200, Json.arr(list.stream().map(Server::toJson).toList()));
            }
        } else if ("POST".equals(method)) {
            Voluntario v = fromJsonVoluntario(readBody(ex));
            boolean ok = r.voluntario.create(v);
            respondJson(ex, ok?201:409, ok?toJson(v):"{\"error\":\"duplicate key\"}");
        } else if ("PUT".equals(method) && parts.length == 4) {
            Voluntario v = fromJsonVoluntario(readBody(ex));
            v.cpf = parts[3];
            boolean ok = r.voluntario.update(v);
            respondJson(ex, ok?200:404, ok?toJson(v):"{\"error\":\"not found\"}");
        } else if ("DELETE".equals(method) && parts.length == 4) {
            boolean ok = r.voluntario.delete(parts[3]);
            respondJson(ex, ok?204:404, ok?"{}":"{\"error\":\"not found\"}");
        } else respondJson(ex, 400, "{\"error\":\"bad request\"}");
    }

    // -------- Adotantes
    private static void handleAdotantes(HttpExchange ex, Repositories r) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        if ("GET".equals(method)) {
            if (parts.length == 4) {
                String cpf = parts[3];
                var o = r.adotante.read(cpf);
                if (o.isEmpty()) { respondJson(ex, 404, "{\"error\":\"not found\"}"); return; }
                respondJson(ex, 200, toJson(o.get()));
            } else {
                String nome = param(ex.getRequestURI().getQuery(), "nome");
                var list = r.adotante.searchByNomeFragment(nome == null ? "" : nome);
                respondJson(ex, 200, Json.arr(list.stream().map(Server::toJson).toList()));
            }
        } else if ("POST".equals(method)) {
            Adotante a = fromJsonAdotante(readBody(ex));
            boolean ok = r.adotante.create(a);
            respondJson(ex, ok?201:409, ok?toJson(a):"{\"error\":\"duplicate key\"}");
        } else if ("PUT".equals(method) && parts.length == 4) {
            Adotante a = fromJsonAdotante(readBody(ex));
            a.cpf = parts[3];
            boolean ok = r.adotante.update(a);
            respondJson(ex, ok?200:404, ok?toJson(a):"{\"error\":\"not found\"}");
        } else if ("DELETE".equals(method) && parts.length == 4) {
            boolean ok = r.adotante.delete(parts[3]);
            respondJson(ex, ok?204:404, ok?"{}":"{\"error\":\"not found\"}");
        } else respondJson(ex, 400, "{\"error\":\"bad request\"}");
    }

    // -------- ONGs
    private static void handleOngs(HttpExchange ex, Repositories r) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        if ("GET".equals(method)) {
            if (parts.length == 4) {
                String nome = parts[3];
                var o = r.ong.read(nome);
                if (o.isEmpty()) { respondJson(ex, 404, "{\"error\":\"not found\"}"); return; }
                respondJson(ex, 200, toJson(o.get()));
            } else {
                // list ordered by nome
                List<String> arr = new java.util.ArrayList<>();
                for (Ong o : r.ong.listAllOrdered()) arr.add(toJson(o));
                respondJson(ex, 200, Json.arr(arr));
            }
        } else if ("POST".equals(method)) {
            Ong o = fromJsonOng(readBody(ex));
            boolean ok = r.ong.create(o);
            respondJson(ex, ok?201:409, ok?toJson(o):"{\"error\":\"duplicate key\"}");
        } else if ("PUT".equals(method) && parts.length == 4) {
            Ong o = fromJsonOng(readBody(ex));
            o.nome = parts[3];
            boolean ok = r.ong.update(o);
            respondJson(ex, ok?200:404, ok?toJson(o):"{\"error\":\"not found\"}");
        } else if ("DELETE".equals(method) && parts.length == 4) {
            boolean ok = r.ong.delete(parts[3]);
            respondJson(ex, ok?204:404, ok?"{}":"{\"error\":\"not found\"}");
        } else respondJson(ex, 400, "{\"error\":\"bad request\"}");
    }

    // -------- Animais
    private static void handleAnimais(HttpExchange ex, Repositories r) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        if ("GET".equals(method)) {
            if (parts.length == 4) {
                String id = parts[3];
                var o = r.animal.read(id);
                if (o.isEmpty()) { respondJson(ex, 404, "{\"error\":\"not found\"}"); return; }
                respondJson(ex, 200, toJson(o.get()));
            } else {
                String q = ex.getRequestURI().getQuery();
                String nome = param(q, "nome");
                String especie = param(q, "especie");
                String cas = param(q, "castrado");
                Boolean b = cas == null ? null : Boolean.valueOf(cas);
                var list = r.animal.search(nome, especie, b);
                respondJson(ex, 200, Json.arr(list.stream().map(Server::toJson).toList()));
            }
        } else if ("POST".equals(method)) {
            Animal a = fromJsonAnimal(readBody(ex));
            boolean ok = r.animal.create(a);
            respondJson(ex, ok?201:409, ok?toJson(a):"{\"error\":\"duplicate key\"}");
        } else if ("PUT".equals(method) && parts.length == 4) {
            Animal a = fromJsonAnimal(readBody(ex));
            a.id = parts[3];
            boolean ok = r.animal.update(a);
            respondJson(ex, ok?200:404, ok?toJson(a):"{\"error\":\"not found\"}");
        } else if ("DELETE".equals(method) && parts.length == 4) {
            boolean ok = r.animal.delete(parts[3]);
            respondJson(ex, ok?204:404, ok?"{}":"{\"error\":\"not found\"}");
        } else respondJson(ex, 400, "{\"error\":\"bad request\"}");
    }

    // -------- JSON mapping (manual, flat)
    private static String toJson(Voluntario v) {
        return Json.obj(java.util.Map.of(
                "cpf", v.cpf, "nome", v.nome, "sobrenome", v.sobrenome, "idade", v.idade
        ));
    }
    private static String toJson(Adotante a) {
        return Json.obj(java.util.Map.of(
                "cpf", a.cpf, "nome", a.nome, "sobrenome", a.sobrenome, "idade", a.idade, "endereco", a.endereco
        ));
    }
    private static String toJson(Ong o) {
        return Json.obj(java.util.Map.of(
                "nome", o.nome, "endereco", o.endereco, "telefone", o.telefone
        ));
    }
    private static String toJson(Animal a) {
        return Json.obj(java.util.Map.of(
                "id", a.id, "nome", a.nome, "idade", a.idade, "especie", a.especie, "se_castrado", a.seCastrado
        ));
    }

    private static Voluntario fromJsonVoluntario(String json) { Voluntario v = new Voluntario();
        v.cpf = field(json, "cpf"); v.nome = field(json, "nome"); v.sobrenome = field(json, "sobrenome"); v.idade = intField(json, "idade"); String senha = field(json, "senha"); if (senha != null) v.senhaEnc = XorCipher.encrypt(senha, "mpet"); return v; }
    private static Adotante fromJsonAdotante(String json) { Adotante a = new Adotante();
        a.cpf = field(json, "cpf"); a.nome = field(json, "nome"); a.sobrenome = field(json, "sobrenome"); a.idade = intField(json, "idade"); a.endereco = field(json, "endereco"); String senha = field(json, "senha"); if (senha != null) a.senhaEnc = XorCipher.encrypt(senha, "mpet"); return a; }
    private static Ong fromJsonOng(String json) { Ong o = new Ong();
        o.nome = field(json, "nome"); o.endereco = field(json, "endereco"); o.telefone = field(json, "telefone"); String senha = field(json, "senha"); if (senha != null) o.senhaEnc = XorCipher.encrypt(senha, "mpet"); return o; }
    private static Animal fromJsonAnimal(String json) { Animal a = new Animal();
        a.id = field(json, "id"); a.nome = field(json, "nome"); a.idade = intField(json, "idade"); a.especie = field(json, "especie"); a.seCastrado = boolField(json, "se_castrado"); return a; }

    // naive JSON field extraction for simple inputs
    private static String field(String json, String key) {
        String pattern = '"' + key + '"' + ':';
        int i = json.indexOf(pattern);
        if (i < 0) return null;
        int start = json.indexOf('"', i + pattern.length());
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }
    private static int intField(String json, String key) {
        String pattern = '"' + key + '"' + ':';
        int i = json.indexOf(pattern);
        if (i < 0) return 0;
        int start = i + pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        return Integer.parseInt(json.substring(start, end).trim());
    }
    private static boolean boolField(String json, String key) {
        String pattern = '"' + key + '"' + ':';
        int i = json.indexOf(pattern);
        if (i < 0) return false;
        int start = i + pattern.length();
        String tail = json.substring(start).trim();
        return tail.startsWith("true");
    }

    // Simple login across entidades by cpf/nome and senha, prioritizing Voluntario, then Adotante, then Ong
    private static void handleLogin(HttpExchange ex, Repositories r) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { respondJson(ex, 405, "{\"error\":\"method not allowed\"}"); return; }
        String body = readBody(ex);
        String usuario = field(body, "usuario");
        String senha = field(body, "senha");
        if (usuario == null || senha == null) { respondJson(ex, 400, "{\"error\":\"usuario e senha requeridos\"}"); return; }
        String enc = XorCipher.encrypt(senha, "mpet");
        // try CPF voluntario
        var v = r.voluntario.read(usuario);
        if (v.isPresent() && enc.equals(v.get().senhaEnc)) { respondJson(ex, 200, Json.obj(java.util.Map.of("tipo","voluntario","id",v.get().cpf))); return; }
        // adotante
        var a = r.adotante.read(usuario);
        if (a.isPresent() && enc.equals(a.get().senhaEnc)) { respondJson(ex, 200, Json.obj(java.util.Map.of("tipo","adotante","id",a.get().cpf))); return; }
        // ong by nome
        var o = r.ong.read(usuario);
        if (o.isPresent() && enc.equals(o.get().senhaEnc)) { respondJson(ex, 200, Json.obj(java.util.Map.of("tipo","ong","id",o.get().nome))); return; }
        respondJson(ex, 401, "{\"error\":\"credenciais invalidas\"}");
    }

    // Maintenance endpoint: compact data files and rebuild indexes
    private static void handleVacuum(HttpExchange ex, Repositories r) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { respondJson(ex, 405, "{\"error\":\"method not allowed\"}"); return; }
        long start = System.currentTimeMillis();
        try {
            r.vacuumAll();
            long ms = System.currentTimeMillis() - start;
            respondJson(ex, 200, Json.obj(java.util.Map.of("ok", true, "ms", ms)));
        } catch (IOException e) {
            respondJson(ex, 500, Json.obj(java.util.Map.of("ok", false, "error", e.getMessage())));
        }
    }

    // Parse simple query param from key=value&...
    private static String param(String query, String key) {
        if (query == null || query.isEmpty()) return null;
        for (String p : query.split("&")) {
            int i = p.indexOf('=');
            if (i > 0) {
                String k = p.substring(0, i);
                String v = p.substring(i+1);
                if (k.equals(key)) return java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
