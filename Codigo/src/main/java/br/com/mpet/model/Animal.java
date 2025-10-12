package br.com.mpet.model;

/**
 * Classe base para hierarquia de animais. Tornada abstract para evitar
 * instanciação direta de um "Animal genérico". O campo "ativo" reflete o
 * estado lógico; no arquivo binário haverá também o byte tombstone. Na leitura
 * definiremos ativo = (tombstone == 0).
 */
public abstract class Animal {
    int id; // PK sequencial (gerado pelo DAO / cabeçalho do arquivo)
    int idOng; // FK para ONG responsável
    String nome; // obrigatório (pode ser apelido)
    java.time.LocalDate dataNascimentoAprox; // opcional
    char sexo; // 'M', 'F', 'U'
    Porte porte; // obrigatório
    HistoricoMedico historicoMedico; // nunca null; inicializado no create se vier null
    String descricao;
    // inclui temperamento, historico de maus tratos e historia de vida
  



    boolean ativo; // espelha tombstone (tombstone=1 => ativo=false)
}

