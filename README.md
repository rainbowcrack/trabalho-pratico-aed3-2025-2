# MPet Backend

CRUD binário com índice B+ simplificado e CLI para animais (Cachorro/Gato).

## Escopo atual
- Persistência em arquivo `.dat` com cabeçalho fixo (128 bytes) e registros: `[tipo][tombstone][id][len][payload]`.
- Índice B+ simplificado em `.idx` (lista ordenada de pares `id -> offset`, com cabeçalho B+).
- CLI (`br.com.mpet.Interface`) para criar/ler/listar/editar/remover/vacuum, backup/restore em ZIP.
- Remoção da ficha médica detalhada; campos principais:
	- `vacinado` (booleano)
	- `descricao` (condição de saúde, texto)

## Modelo de dados
Campos comuns de `Animal`:
- `id` (int, sequencial)
- `idOng` (int)
- `nome` (String)
- `dataNascimentoAprox` (LocalDate, opcional)
- `sexo` (char: M/F/U)
- `porte` (enum: PEQUENO/MEDIO/GRANDE)
- `vacinado` (boolean)
- `descricao` (String, opcional)

Cachorro:
- `raca` (String)
- `nivelAdestramento` (enum: NENHUM/BASICO/AVANCADO)
- `seDaBemComCachorros`, `seDaBemComGatos`, `seDaBemComCriancas` (boolean)

Gato:
- `raca` (String)
- `seDaBemComCachorros`, `seDaBemComGatos`, `seDaBemComCriancas` (boolean)
- `acessoExterior`, `possuiTelamento` (boolean)

## Formato do payload (Codec)
Ordem nos registros de Animal:
1) idOng (int)
2) nome (StringU16)
3) dataNascimentoAprox (LocalDate)
4) sexo (char)
5) porte (Enum)
6) vacinado (TriBoolean 'V'/'F'/'U')
7) descricao (StringU16)

Depois, campos específicos da espécie conforme acima.

Semântica de StringU16:
- null -> 0xFFFF (tamanho -1)
- "" -> tamanho 0

Enum: ordinal+1 (0 = null)

## Como rodar (Windows PowerShell)
> Observação: se não tiver Maven instalado, peça para incluir Maven Wrapper.

- Compilar (com Maven):
```
mvn -f Codigo\pom.xml -q -DskipTests package
```

- Executar a CLI (usando a classe Interface via IDE ou com `java -cp` apontando para `target/classes`).

Arquivos são gravados em `dats/`:
- `animais.dat` e `animais.dat.idx`
- `animais.zip` (backup manual)

## Scripts para terminal
- Makefile (usa PowerShell internamente):
	- `make build` — compila
	- `make run` — executa a CLI (`br.com.mpet.Interface`)
	- `make clean` — limpa
- PowerShell puro:
	- `scripts/run.ps1` — compila e roda a Interface

## Organização do backend (guia por arquivo) 📁

Estrutura relevante (backend):
- `Codigo/pom.xml` — Configuração do Maven: Java 21, plugins de compile/jar, resources. Ajuste o `main.class` se quiser empacotar um jar executável.
- `Codigo/src/main/java/br/com/mpet/Interface.java` — CLI interativa para CRUD de Animais em arquivo binário. Menu com criar, ler, listar, editar, remover, vacuum, backup e restore. Todos os arquivos persistidos ficam em `dats/`.

### Modelos (pacote `br.com.mpet.model`)
- `Animal.java` — Classe abstrata base dos animais. Campos: `id`, `idOng`, `nome`, `dataNascimentoAprox`, `sexo`, `porte`, `vacinado`, `descricao`, `ativo` (espelha tombstone). Sem ficha médica detalhada (foi removida do modelo persistido).
- `Cachorro.java` — Extende `Animal`. Campos: `raca`, `nivelAdestramento`, booleans de convivência (`seDaBemComCachorros`, `...Gatos`, `...Criancas`).
- `Gato.java` — Extende `Animal`. Campos: `raca`, booleans de convivência e ambiente (`acessoExterior`, `possuiTelamento`).
- `Porte.java` — Enum de porte: `PEQUENO`, `MEDIO`, `GRANDE`.
- `NivelAdestramento.java` — Enum: `NENHUM`, `BASICO`, `AVANCADO`.
- `Usuario.java` — Base para `Adotante` e `Voluntario` (campos típicos de usuário: email/senha/cpf/telefone/ativo). Usado fora do escopo imediato de Animal.
- `Adotante.java` / `Voluntario.java` / `Ong.java` — Entidades de cadastro. Persistência via DAOs específicos (a evoluir). 
- `HistoricoMedico.java` / `Vacina.java` / `Exame.java` — Suporte a ficha médica detalhada (não está sendo persistida atualmente). Mantidos no repo para evolução futura.
- Outros: `ComposicaoFamiliar.java`, `ResultadoTeste.java`, `Role.java`, `Temperamento.java`, `TipoMoradia.java` — enums/POJOs auxiliares do domínio.

### Persistência (pacote `br.com.mpet.persistence`)
- `BaseDataFile.java` — Base com utilitários de acesso ao `.dat`: cabeçalho, append, tombstone, etc. Fechamento idempotente (tolera múltiplos `close()` sem exceção).
- `CrudDao.java` — Interface genérica de CRUD com `create/read/update/delete/listAllActive/rebuildIfEmpty/vacuum/close`.

#### DAOs (pacote `br.com.mpet.persistence.dao`)
- `AnimalDao.java` — Abstração da família de DAOs de Animal.
- `AnimalDataFileDao.java` — Implementação concreta file-based (binária) para `Animal`/`Cachorro`/`Gato`:
	- Layout de registro: `[tipo(1)][tomb(1)][id(4)][len(4)][payload(len)]`.
	- Índice primário em B+ simplificado: `id -> offset` persistido em `.idx` (classe `BPlusTreeIndex`).
	- Operações: `create` (append + index), `read` (via índice), `update` (in-place se tamanho igual; senão tombstone + append + index), `delete` (tombstone + remove index), `listAllActive`, `rebuildIfEmpty`, `vacuum` (gera arquivo temporário, regrava apenas ativos, troca `.dat` e `.idx`).
	- Serialização usa `Codec` (ver abaixo). Campos comuns incluem `vacinado`.
- `AdotanteDao.java`, `VoluntarioDao.java`, `OngDao.java` — Stubs/DAOs para outras entidades (em evolução).

#### Índices (pacote `br.com.mpet.persistence.index`)
- `BPlusTreeIndex.java` — “B+ Tree” simplificada: mantém chaves em memória (`TreeMap`) e persiste a lista ordenada como `(int key, long offset)` com um cabeçalho B+. `put/get/remove/size/close`. Fechamento idempotente.

#### I/O helpers (pacote `br.com.mpet.persistence.io`)
- `Codec.java` — Codificadores/decodificadores binários:
	- Primitivos: `int/long/char` (big-endian), booleans com tri-estado `V/F/U`.
	- Strings U16: `null=0xFFFF`, `""=0`.
	- Enums: `ordinal+1` (`0=null`).
	- `LocalDate` com flag de presença.
	- Vários métodos utilitários e concatenação de payloads.
- `FileHeaderHelper.java` — Cabeçalhos de 128 bytes para `.dat` e `.idx` (B+ e Hash extensível):
	- Header principal (`versaoFormato`, `proximoId`, `countAtivos`).
	- Header B+ (`ponteiroParaNoRaiz`, `ordemDaArvore`, `alturaDaArvore`, `countTotalDeRegistros`, `ponteiroParaListaDeNosLivres`).
	- Header Hash (planejado para relacionamentos 1:N).

### Recursos (pacote `src/main/resources`)
- `public/` (HTML/CSS/JS) — Frontend estático do site. Fora do escopo deste README (backend).
- `script-db/db.sql` — Esquema SQL de referência (não usado pelo backend file-based atual). Mantido como documentação/apoio.

### Scripts e automação
- `Makefile` — Targets `build`, `run`, `clean` usando PowerShell.
- `scripts/run.ps1` — Script PowerShell para compilar e executar a CLI.
- Diretório de dados: `dats/` — Local onde são gravados `.dat`, `.idx` e backups `.zip`.

## Fluxos principais (CLI) 🖥️
- Criar (1): coleta dados básicos (inclui `vacinado`) e específicos (Gato/Cachorro), salva e mostra `id`.
- Ler (2): busca por `id` via índice B+.
- Listar (3): varre o `.dat` e carrega somente registros ativos.
- Editar (4): atualiza campos, com update in-place quando possível.
- Remover (5): tombstone lógico + remove do índice.
- Compactar (6): `vacuum` (regrava só ativos, substitui `.dat` e `.idx`).
- Restaurar (8) e Backup (9): ZIP contendo `animais.dat` e `animais.dat.idx` em `dats/`.

## Boas práticas e pitfalls
- Sempre reabrir o DAO após operações que substituem arquivos (vacuum/restore) — a `Interface` já faz isso.
- O índice `.idx` é sincronizado a cada mutação; não editar arquivos manualmente.
- A ordem dos campos no payload deve ser mantida para compatibilidade.

## Tasks e prazos (atualizados em 12/10/2025)

Até Domingo 12/10
- [ ] Terminar os CRUDs (ajustes finais em DAO/CLI)
- [ ] Fazer scripts Bash e Makefile para testar no terminal
- [ ] Fazer Deploy no terminal
- [ ] Reunião de Alinhamento

Até Terça 14/10
- [ ] Relacionamentos 1:N com Hash Extensível (offset)

Até Quarta 15/10
- [ ] Último teste
- [ ] Completar a documentação

Até Quinta 16/10
- [ ] Enviar

## Scripts (teste rápido)
Criaremos `scripts/run.sh` e `Makefile` para facilitar build e execução.

## Notas
- O vacuum compacta o `.dat` e sincroniza o `.idx` correspondente.
- Backup/restore funcionam via ZIP sob `dats/`.
O sistema deve permitir o cadastro e o gerenciamento de Voluntários, Adotantes, ONGs e Animais, possibilitando a integração entre ONGs e adotantes. Além disso, deve fornecer uma métrica de compatibilidade entre os animais disponíveis para doação e os perfis dos adotantes.


# Back End 

Esta seção documenta toda a implementação do backend: arquitetura, formato de dados, algoritmos (KMP, LZW, XOR) e manutenção (vacuum), além dos endpoints disponíveis e como executar.

## Visão geral
- Linguagem/Build: Java 17 + Maven, app standalone usando HttpServer do JDK (sem frameworks).
- Escopo: CRUD de Voluntário, Adotante, ONG e Animal com persistência em arquivos binários, índices (Árvore B+ e Hash Extensível), compressão de payloads (LZW), senha com cifra XOR, buscas parciais (KMP) e rotina de compactação (vacuum).
- Código relevante: `Codigo/src/main/java/br/com/mpet/**`.

## Como executar
1) Build do jar:
	 - Dentro de `Codigo/`: `mvn -DskipTests package`
	 - Saída: `Codigo/target/backend-0.1.0.jar`
2) Rodar servidor:
	 - `java -jar target/backend-0.1.0.jar` (variável `PORT` opcional, padrão 8080)
3) Front-end estático: servido em `/` a partir de `Codigo/src/main/resources/public/`.

## Estrutura de dados e persistência
Cada entidade é serializada de/para bytes em um arquivo binário próprio, gerenciado por `BinaryFileStore`:
- Formato de registro: `[tombstone(1 byte)] [length(4 bytes, int)] [payload(length bytes)]`.
	- `tombstone = 0` ativo, `1` removido logicamente.
	- `payload` é o objeto serializado (campos) e é comprimido com LZW ao gravar; ao ler, é descomprimido.
- Operações:
	- Create: escreve no fim do arquivo e retorna o offset (posição) físico.
	- Read: busca o offset pelo índice e lê/decodifica o registro.
	- Update: tenta sobrescrever in-place; se crescer, marca antigo como removido e regrava no fim (offset muda) e o índice é atualizado.
	- Delete: marca o registro como removido e remove a chave do índice.

Arquivos gerados (por padrão em `Codigo/data/`):
- Voluntários: `voluntarios.dat` + índice `voluntarios.bpt` (Árvore B+)
- Adotantes: `adotantes.dat` + índice `adotantes.ehash` (Hash Extensível)
- ONGs: `ongs.dat` + índice `ongs.bpt` (Árvore B+)
- Animais: `animais.dat` + índice `animais.ehash` (Hash Extensível)

## Índices: Árvore B+ e Hash Extensível
- Árvore B+ (Voluntário/ONG): estrutura ordenada em memória com ordem fixa, permitindo iteração em ordem e busca por chave. É persistida em arquivo texto simples (pares `chave<TAB>offset`) ao fechar a aplicação; no bootstrap, é recarregada reinserindo as chaves. Usada porque preserva ordenação (ex.: listar ONGs em ordem de nome).
- Hash Extensível (Adotante/Animal): diretório com profundidade global e buckets com profundidade local. Buckets dividem ao encher, ajustando o diretório. Persistência também em formato texto (profundidade + diretório + conteúdo de buckets). Usado pela velocidade de acesso direto por chave.
- Recuperação de índice: em cada repositório há um `rebuildIfEmpty()` que, se necessário, varre o arquivo `.dat` e repopula o índice.

## Compactação (LZW)
- O que é: algoritmo clássico de compressão sem perdas baseado em dicionário (Lempel–Ziv–Welch). Reduz tamanho de payloads armazenados.
- Como usamos: `BinaryFileStore` aplica `LZWCodec` ao escrever (encode) e desfaz ao ler (decode). Transparente para os repositórios.
- Benefícios: economiza espaço e I/O; custo computacional moderado adequado ao cenário.

## Criptografia XOR (senhas)
- O que é: cifra de fluxo simples aplicando XOR entre bytes da senha e de uma chave repetida.
- Implementação: `XorCipher.encrypt(senha, "mpet")` gera `senhaEnc` em hexadecimal. Os modelos `Voluntario`, `Adotante` e `Ong` armazenam o campo `senhaEnc` serializado no payload.
- Login: endpoint compara `senhaEnc` calculado com o persistido (sem guardar senhas em texto puro).

## Casamento de padrões (KMP)
- O que é: algoritmo Knuth–Morris–Pratt para busca de substring em tempo linear.
- Uso: buscas parciais por fragmento de texto (ex.: nome/sobrenome de voluntários/adotantes, nome/espécie de animais). Implementado em `br.com.mpet.algorithms.KMP` e usado nos repositórios ao filtrar resultados.

## Vacuum (compactação de arquivo)
- Problema: updates que crescem e deletes deixam “buracos” (registros tombados) no `.dat`.
- Solução: `vacuum()` reescreve o arquivo, mantendo apenas registros ativos, retornando novos offsets; em seguida os índices são reconstruídos para apontar para as novas posições compactadas.
- Como chamar: endpoint `POST /api/admin/vacuum` executa vacuum para todas as entidades. Resposta inclui `ok` e o tempo em ms. Observação: o endpoint não possui autenticação de propósito didático; pode-se proteger com um header secreto simples.

## Endpoints
- Saúde: `GET /health` → `{ "status": "ok" }`
- Login: `POST /api/login` body `{ "usuario": "<cpf|nome_ong>", "senha": "..." }`

- Voluntários (`cpf` é a chave):
	- `GET /api/voluntarios?nome=<frag>` lista filtrando por fragmento (KMP) em nome/sobrenome.
	- `GET /api/voluntarios/{cpf}` retorna um voluntário.
	- `POST /api/voluntarios` cria (body: cpf, nome, sobrenome, idade, senha). A senha vira `senhaEnc` com XOR.
	- `PUT /api/voluntarios/{cpf}` atualiza o registro do cpf.
	- `DELETE /api/voluntarios/{cpf}` remove.

- Adotantes (`cpf`):
	- `GET /api/adotantes?nome=<frag>` busca por fragmento de nome/sobrenome.
	- `GET /api/adotantes/{cpf}`
	- `POST /api/adotantes` (cpf, nome, sobrenome, idade, endereco, senha)
	- `PUT /api/adotantes/{cpf}`
	- `DELETE /api/adotantes/{cpf}`

- ONGs (`nome` é a chave):
	- `GET /api/ongs` lista em ordem alfabética (graças à Árvore B+).
	- `GET /api/ongs/{nome}`
	- `POST /api/ongs` (nome, endereco, telefone, senha)
	- `PUT /api/ongs/{nome}`
	- `DELETE /api/ongs/{nome}`

- Animais (`id`):
	- `GET /api/animais?nome=<frag>&especie=<frag>&castrado=<true|false>`
	- `GET /api/animais/{id}`
	- `POST /api/animais` (id, nome, idade, especie, se_castrado)
	- `PUT /api/animais/{id}`
	- `DELETE /api/animais/{id}`

- Manutenção:
	- `POST /api/admin/vacuum` → compacta todos os `.dat` e reconstrói índices.

### Exemplos (JSON simplificado)
- Criar voluntário:
	Request: `{ "cpf":"123", "nome":"Ana", "sobrenome":"Silva", "idade":25, "senha":"segredo" }`
	Response 201: `{ "cpf":"123", "nome":"Ana", "sobrenome":"Silva", "idade":25 }`
- Buscar voluntários: `GET /api/voluntarios?nome=si` → retorna array JSON com correspondências por KMP.
- Login: `POST /api/login` `{ "usuario":"123", "senha":"segredo" }` → `{ "tipo":"voluntario", "id":"123" }`.

## Limitações e próximos passos
- Validação de entrada e erros podem ser enriquecidos (mensagens, 400/404/405 mais detalhados).
- Concorrência: repositórios não são sincronizados; para alta concorrência, adicionar locks.
- Segurança: adicionar controle de acesso ao `/api/admin/vacuum`.
- Testes automatizados: unitários (armazenamento/índices) e integração dos endpoints.