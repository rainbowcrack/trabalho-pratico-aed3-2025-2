package br.com.mpet;

import br.com.mpet.model.*;
import br.com.mpet.persistence.dao.AnimalDataFileDao;

import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Interface de linha de comando (CLI) para testar e operar o CRUD de Animais (Cachorro/Gato)
 * diretamente no arquivo binário .dat, com suporte a:
 *   - criar, ler por ID, listar, editar, remover (tombstone), compactar (vacuum)
 *   - backup em ZIP (animais.dat + animais.dat.idx) e restauração do backup
 *
 * Persistência entre execuções: os dados ficam em "animais.dat" no diretório atual.
 * Ao fechar o programa e abrir novamente, a opção "Listar" (3) mostrará os registros já salvos.
 */
public class Interface {

    // Diretório onde serão salvos os arquivos .dat/.idx/.zip
    private static final File DATA_DIR = new File("dats");
    // Nomes base de arquivos
    private static final String DATA_FILENAME = "animais.dat";
    private static final String IDX_FILENAME = "animais.dat.idx";
    private static final String ZIP_FILENAME = "animais.zip";
    // Arquivos com caminho dentro de DATA_DIR
    private static final File DATA_FILE = new File(DATA_DIR, DATA_FILENAME);
    private static final File IDX_FILE = new File(DATA_DIR, IDX_FILENAME);
    private static final File ZIP_FILE = new File(DATA_DIR, ZIP_FILENAME);
    private static final byte VERSAO = 1;

    /**
     * Ponto de entrada. Mantemos o controle do DAO aqui para podermos fechá-lo quando
     * o usuário optar por restaurar um ZIP (pois restaurar sobrescreve os arquivos).
     */
    public static void main(String[] args) throws Exception {
        // Garante que a pasta 'dats' exista
        if (!DATA_DIR.exists()) {
            //noinspection ResultOfMethodCallIgnored
            DATA_DIR.mkdirs();
        }
        executarMenu(DATA_FILE);
    }

    /**
     * Loop do menu principal. Abre o DAO, executa operações e permite backup/restore.
     * Quando o usuário opta por restaurar do ZIP, fechamos o DAO, restauramos os arquivos
     * e reabrimos o DAO na sequência, para refletir os dados restaurados imediatamente.
     */
    private static void executarMenu(File dataFile) throws IOException {
        Scanner sc = new Scanner(System.in);
        AnimalDataFileDao dao = new AnimalDataFileDao(dataFile, VERSAO);
        try {
            while (true) {
                System.out.println("\n=== PetMatch - CRUD de Animais ===");
                System.out.println("1) Criar animal (Cachorro/Gato)");
                System.out.println("2) Ler animal por ID");
                System.out.println("3) Listar todos os ativos");
                System.out.println("4) Editar animal existente");
                System.out.println("5) Remover animal (tombstone)");
                System.out.println("6) Compactar arquivo (vacuum)");
                System.out.println("7) descompactar arquivo (vacuum)");
                System.out.println("8) Restaurar de ZIP (sobrescreve arquivos)");
                System.out.println("9) Backup ZIP (animais.dat + .idx)");
                System.out.println("0) Sair");
                System.out.print("Escolha: ");
                String op = sc.nextLine().trim();

                try {
                    switch (op) {
                        case "1" -> criar(sc, dao);
                        case "2" -> ler(sc, dao);
                        case "3" -> listar(dao);
                        case "4" -> editar(sc, dao);
                        case "5" -> remover(sc, dao);
                        case "6" -> {
                            // Compacta e reabre o DAO para reler cabeçalho/índice após substituição
                            // Observação: dao.vacuum() já fecha este DAO internamente.
                            dao.vacuum();
                            dao = new AnimalDataFileDao(dataFile, VERSAO);
                            System.out.println("Arquivo compactado (vacuum) com sucesso.");
                        }
                        case "7" -> { // descompactar (interpretação: restaurar do ZIP)
                            try { dao.close(); } catch (Exception ignore) {}
                            restoreZip();
                            dao = new AnimalDataFileDao(dataFile, VERSAO);
                            System.out.println("Descompactação concluída e DAO reaberto.");
                        }
                        case "8" -> { // Restaurar de ZIP (sobrescreve arquivos)
                            // Fechamos o DAO para liberar handle no .dat/.idx antes de sobrescrever
                            try { dao.close(); } catch (Exception ignore) {}
                            restoreZip();
                            // Reabrimos para refletir os dados restaurados
                            dao = new AnimalDataFileDao(dataFile, VERSAO);
                            System.out.println("Restauração concluída e DAO reaberto.");
                        }
                        case "9" -> backupZip();
                        case "0" -> { return; }
                        default -> System.out.println("Opção inválida.");
                    }
                } catch (Exception ex) {
                    System.out.println("Erro: " + ex.getMessage());
                }
            }
        } finally {
            try { dao.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * Cria um novo animal. Pergunta tipo e campos; persiste e imprime o ID gerado.
     */
    private static void criar(Scanner sc, AnimalDataFileDao dao) throws IOException {
        System.out.print("Tipo (C=cachorro, G=gato): ");
        String t = sc.nextLine().trim().toUpperCase();
        Animal a;
        if (t.equals("C")) a = new Cachorro(); else if (t.equals("G")) a = new Gato(); else { System.out.println("Tipo inválido"); return; }

        preencherBasico(sc, a);

        if (a instanceof Cachorro c) {
            System.out.print("Raça: "); c.setRaca(sc.nextLine().trim());
            c.setNivelAdestramento(perguntarNivel(sc));
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
        System.out.println("Criado com ID: " + salvo.getId());
    }

    /** Lê e imprime um animal pelo ID. */
    private static void ler(Scanner sc, AnimalDataFileDao dao) throws IOException {
        System.out.print("ID: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        Optional<Animal> opt = dao.read(id);
        if (opt.isEmpty()) { System.out.println("Não encontrado."); return; }
        imprimirAnimal(opt.get());
    }

    /** Lista todos os animais ativos. */
    private static void listar(AnimalDataFileDao dao) throws IOException {
        List<Animal> todos = dao.listAllActive();
        if (todos.isEmpty()) { System.out.println("Nenhum ativo."); return; }
        todos.forEach(Interface::imprimirAnimal);
    }

    /** Edita campos básicos e específicos do animal selecionado por ID. */
    private static void editar(Scanner sc, AnimalDataFileDao dao) throws IOException {
        System.out.print("ID a editar: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        Optional<Animal> opt = dao.read(id);
        if (opt.isEmpty()) { System.out.println("Não encontrado."); return; }
        Animal a = opt.get();
        System.out.println("Editando: "); imprimirAnimal(a);

        // Campos básicos
        System.out.print("Novo nome (Enter mantém): ");
        String nome = sc.nextLine(); if (!nome.isBlank()) a.setNome(nome.trim());
        System.out.print("Nova descrição (Enter mantém): ");
        String desc = sc.nextLine(); if (!desc.isBlank()) a.setDescricao(desc.trim());

        if (a instanceof Cachorro c) {
            System.out.print("Nova raça (Enter mantém): ");
            String r = sc.nextLine(); if (!r.isBlank()) c.setRaca(r.trim());
        } else if (a instanceof Gato g) {
            System.out.print("Nova raça (Enter mantém): ");
            String r = sc.nextLine(); if (!r.isBlank()) g.setRaca(r.trim());
        }

        boolean ok = dao.update(a);
        System.out.println(ok ? "Atualizado." : "Falha ao atualizar.");
    }

    /** Remove logicamente (tombstone) um animal pelo ID. */
    private static void remover(Scanner sc, AnimalDataFileDao dao) throws IOException {
        System.out.print("ID a remover: ");
        int id = Integer.parseInt(sc.nextLine().trim());
        boolean ok = dao.delete(id);
        System.out.println(ok ? "Removido (tombstone)." : "Não encontrado.");
    }

    /* ======================= UTIL ======================= */
    /** Preenche campos comuns de Animal solicitando ao usuário. */
    private static void preencherBasico(Scanner sc, Animal a) {
        a.setAtivo(true);
        System.out.print("Nome: "); a.setNome(sc.nextLine().trim());
        System.out.print("Id da ONG (int): "); a.setIdOng(Integer.parseInt(sc.nextLine().trim()));
        System.out.print("Porte (PEQUENO/MEDIO/GRANDE): "); a.setPorte(Porte.valueOf(sc.nextLine().trim().toUpperCase()));
        System.out.print("Sexo (M/F/U): "); a.setSexo(sc.nextLine().trim().toUpperCase().charAt(0));

        System.out.print("Data de nascimento aprox (yyyy-mm-dd) ou enter: ");
        String dn = sc.nextLine().trim();
        if (!dn.isBlank()) a.setDataNascimentoAprox(LocalDate.parse(dn));

    a.setVacinado(perguntarBool(sc, "Já foi vacinado? (s/n): "));
    System.out.print("Condição de saúde (descrição, opcional): ");
    String d = sc.nextLine(); a.setDescricao(d.isBlank() ? null : d.trim());
    }

    /** Prompt booleano simples (aceita s/n, y/n, 1/0). */
    private static boolean perguntarBool(Scanner sc, String prompt) {
        System.out.print(prompt);
        String s = sc.nextLine().trim().toLowerCase();
        return s.startsWith("s") || s.equals("1") || s.equals("y");
    }

    /** Prompt para nível de adestramento (com fallback para NENHUM). */
    private static NivelAdestramento perguntarNivel(Scanner sc) {
        System.out.print("Nível de adestramento (NENHUM/BASICO/AVANCADO): ");
        String v = sc.nextLine().trim().toUpperCase();
        try { return NivelAdestramento.valueOf(v); } catch (Exception e) { return NivelAdestramento.NENHUM; }
    }

    /** Imprime representação amigável de um Animal. */
    private static void imprimirAnimal(Animal a) {
        // Observação: os dados persistem no arquivo .dat; ao reabrir o programa,
        // usar a opção 3 (Listar) para ver o que foi armazenado anteriormente.
    String base = String.format("[%s] id=%d nome=%s ong=%d porte=%s sexo=%s vacinado=%s ativo=%s",
        a.getClass().getSimpleName(), a.getId(), a.getNome(), a.getIdOng(), String.valueOf(a.getPorte()), a.getSexo(), a.isVacinado(), a.isAtivo());
        if (a instanceof Cachorro c) {
            System.out.println(base + String.format(" raca=%s nivel=%s dogs=%s cats=%s kids=%s",
                    c.getRaca(), c.getNivelAdestramento(), c.isSeDaBemComCachorros(), c.isSeDaBemComGatos(), c.isSeDaBemComCriancas()));
        } else if (a instanceof Gato g) {
            System.out.println(base + String.format(" raca=%s dogs=%s cats=%s kids=%s exterior=%s telamento=%s",
                    g.getRaca(), g.isSeDaBemComCachorros(), g.isSeDaBemComGatos(), g.isSeDaBemComCriancas(), g.isAcessoExterior(), g.isPossuiTelamento()));
        } else {
            System.out.println(base);
        }
    }

    /* ======================= BACKUP/RESTORE ZIP ======================= */

    /**
     * Cria um arquivo ZIP (animais.zip) contendo animais.dat e animais.dat.idx se existirem.
     * Exemplo: após criar registros, use esta opção para gerar um backup transportável.
     */
    private static void backupZip() throws IOException {
        File dat = DATA_FILE;
        File idx = IDX_FILE;
        if (!dat.exists()) { System.out.println("Não há '" + DATA_FILENAME + "' para compactar."); return; }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(ZIP_FILE))) {
            zipOne(zos, dat, DATA_FILENAME);
            if (idx.exists()) zipOne(zos, idx, IDX_FILENAME);
        }
        System.out.println("Backup gerado em: " + ZIP_FILE.getPath());
        // Lista conteúdo
        listZipContents(ZIP_FILE);
    }

    /**
     * Restaura os arquivos a partir de animais.zip para o diretório atual, sobrescrevendo
     * animais.dat e animais.dat.idx. Use após reinstalar/limpar a pasta para recuperar dados.
     */
    private static void restoreZip() throws IOException {
        File zip = ZIP_FILE;
        if (!zip.exists()) { System.out.println("Arquivo '" + ZIP_FILE.getPath() + "' não encontrado."); return; }
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                File out = switch (e.getName()) {
                    case DATA_FILENAME -> DATA_FILE;
                    case IDX_FILENAME -> IDX_FILE;
                    default -> null; // ignora entradas desconhecidas
                };
                if (out != null) {
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        zis.transferTo(fos);
                    }
                }
                zis.closeEntry();
            }
        }
        System.out.println("Arquivos restaurados a partir de: " + ZIP_FILE.getPath());
        listZipContents(zip);
    }

    /** Adiciona um arquivo ao ZIP com o nome lógico informado. */
    private static void zipOne(ZipOutputStream zos, File file, String entryName) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            fis.transferTo(zos);
            zos.closeEntry();
        }
    }

    /** Lista no console o conteúdo do ZIP (nome e tamanho de cada entrada). */
    private static void listZipContents(File zip) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            System.out.println("Conteúdo de " + zip.getName() + ":");
            ZipEntry e; int count=0;
            while ((e = zis.getNextEntry()) != null) {
                System.out.printf(" - %s (%d bytes)%n", e.getName(), e.getSize());
                count++;
                zis.closeEntry();
            }
            if (count==0) System.out.println("(vazio)");
        }
    }
}
