# trabalho-pratico-aed3-2025-2
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