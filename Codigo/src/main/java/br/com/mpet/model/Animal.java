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
    boolean vacinado; // informações principais: se já foi vacinado
    String descricao; // Condição de saúde (descrição livre, opcional)

  



    boolean ativo; // espelha tombstone (tombstone=1 => ativo=false)

    // Getters e Setters mínimos para uso nos DAOs
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdOng() { return idOng; }
    public void setIdOng(int idOng) { this.idOng = idOng; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public java.time.LocalDate getDataNascimentoAprox() { return dataNascimentoAprox; }
    public void setDataNascimentoAprox(java.time.LocalDate data) { this.dataNascimentoAprox = data; }
    public char getSexo() { return sexo; }
    public void setSexo(char sexo) { this.sexo = sexo; }
    public Porte getPorte() { return porte; }
    public void setPorte(Porte porte) { this.porte = porte; }
    public boolean isVacinado() { return vacinado; }
    public void setVacinado(boolean vacinado) { this.vacinado = vacinado; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}

