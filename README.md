# MPet Backend (CLI + Persistência Binária)

Sistema de adoção de pets com backend em Java (CLI) e camada de persistência binária própria. Implementa CRUDs, relacionamentos, interesse/match/chat e adoções, com índices B+ em arquivo para acesso rápido.

Principais características:
- Persistência file-based (RandomAccessFile) com cabeçalho fixo e registros de tamanho variável
- Serialização binária consistente via `Codec` (Strings U16, enums, tri-boolean, datas)
- Índice B+ por entidade com arquivo `.idx` dedicado (id → offset)
- CLI interativa com login por papel (Admin, Adotante, Voluntário)
- Backup/Restore em ZIP e compactação (vacuum)

Repositório: código fonte em `Codigo/`, dados em `dats/`.

## Como compilar e executar

Linux/macOS (bash):

1) Compilar
```
mvn -f Codigo/pom.xml -q -DskipTests package
```

2) Executar a CLI
```
java -cp "Codigo/target/classes" br.com.mpet.Interface
```

Observações:
- O `Makefile` usa PowerShell (Windows). No Linux, use o Maven direto como acima.
- Os arquivos `.dat`/`.idx` e `backup.zip` ficam em `dats/`.

## Arquitetura e formato de arquivos

Cada entidade persiste em um `.dat` com cabeçalho de 128 bytes (FileHeaderHelper) e registros do tipo:
- Animal: `[tipo(1)][tombstone(1)][id(4)][len(4)][payload(len)]`
- Usuários (Adotante/Voluntário): `[tipo(1)][tombstone(1)][idKey(4)][len(4)][payload(len)]`
- Outras entidades (ONG, Adoção, Interesse, ChatThread, ChatMessage): `[tombstone(1)][id(4)][len(4)][payload(len)]`

Convenções do `Codec`:
- StringU16: 0xFFFF = null, 0x0000 = "" (vazia)
- Enum: ordinal+1 (0 = null)
- Tri-Boolean: 'V' true, 'F' false, 'U' indefinido
- LocalDate: 1 byte flag (0=null) + year(int) + month(byte) + day(byte)
- LocalDateTime (threads/mensagens de chat): epoch seconds (long), 0 = null

Índices B+ (`.idx`):
- AnimalDataFileDao, UsuarioDataFileDao (Adotante/Voluntário) e OngDataFileDao sempre usaram B+.
- AdocaoDataFileDao, InteresseDataFileDao, ChatThreadDataFileDao e ChatMessageDataFileDao também usam agora B+ (adicionado).
- Cada DAO mantém cache em memória (Map<K, Long>) e persiste no `.idx` com BTree.

Vacuum:
1. Cria arquivo temporário e regrava apenas registros ativos
2. Substitui o `.dat`
3. Substitui o `.idx` correspondente
4. Reabra o DAO/CLI para refletir os novos offsets

Backup/Restore (ZIP):
- Backup inclui todos `.dat` e `.idx` (animais, ongs, adotantes, voluntários, adoções, interesses, chats)
- Restore sobrescreve os arquivos em `dats/` (a CLI fecha os DAOs antes)

## Entidades e campos (resumo)

- Animal (abstrato) → Cachorro, Gato
    - Comuns: id, idOng, nome, dataNascimentoAprox, sexo, porte, vacinado, descricao, ativo
    - Cachorro: raca, nivelAdestramento, convive(cães/gatos/crianças)
    - Gato: raca, convive(cães/gatos/crianças), acessoExterior, possuiTelamento

- Ong
    - id (int), nome, cnpj, endereco, telefone, cpfResponsavel (String), ativo

- Usuario (base) → Adotante, Voluntario
    - Comuns: cpf, senha, telefone, ativo
    - Adotante: nomeCompleto, dataNascimento, tipoMoradia, tela protetora, outros animais (desc), horas fora de casa, composicao familiar, viagens (desc), já teve pets, experiência, motivo, ciência de responsável/custos
    - Voluntario: nome, endereco, idOng, cargo (Role)

- Adocao
    - id (int), cpfAdotante (String), idAnimal (int), dataAdocao (LocalDate), ativo

- Interesse
    - id (int), cpfAdotante (String), idAnimal (int), data (LocalDate), status (PENDENTE/APROVADO/RECUSADO), ativo

- ChatThread
    - id (int), idAnimal (int), cpfAdotante (String), aberto (boolean), criadoEm (LocalDateTime)

- ChatMessage
    - id (int), threadId (int), sender (ADOTANTE/VOLUNTARIO), conteudo (String), enviadoEm (LocalDateTime), ativo

## DAOs principais

- AnimalDataFileDao: CRUD polimórfico de Animal (Cachorro/Gato) com B+ id→offset
- UsuarioDataFileDao<T extends Usuario>: CRUD (CPF como chave lógica) com idKey int derivado do CPF e B+ idKey→offset; verificação de CPF no payload
- OngDataFileDao: CRUD com B+ id→offset
- AdocaoDataFileDao, InteresseDataFileDao, ChatThreadDataFileDao, ChatMessageDataFileDao: CRUD com B+ id→offset (adicionados), rebuild, vacuum com troca do `.idx`

## CLI e fluxos

Login:
- Admin: usuário=admin, senha=admin
- Adotante/Voluntário: via CPF + senha
- Tela de login exibe logins/CPFs de exemplo (se houver dados)

Painel do Admin:
- Gerenciar Animais (CRUD, vincula/exige ONG existente)
- Gerenciar ONGs (CRUD, responsável por CPF de voluntário)
- Gerenciar Adotantes (CRUD)
- Gerenciar Voluntários (CRUD, seleção de ONG)
- Gerenciar Adoções (registrar/listar/remover/listar por CPF)
- Sistema: Backup/Restore/Vacuum (inclui todos os `.dat`/`.idx`)

Painel do Adotante:
- Ver/editar meus dados
- Listar animais disponíveis (exclui animais já adotados)
- Demonstrar interesse (sem duplicar interesse para o mesmo animal)
- Ver minhas conversas (lê mensagens; se thread aberta, envia mensagens)

Painel do Voluntário (limitado à sua ONG):
- CRUD de animais da própria ONG (não permite trocar ONG na edição)
- Listar interessados por animal (status)
- Aprovar match (abre ou usa thread de chat para o par adotante–animal)
- Chats: listar threads e enviar mensagens
- Confirmar adoção (cria Adocao, fecha threads do animal e notifica candidatos não escolhidos)

Regras/validações relevantes:
- Ao criar/editar Animal, a ONG é sempre escolhida de uma lista válida
- `Ong.cpfResponsavel` (String) substitui id de voluntário: escolha por lista de voluntários ativos
- Senhas são mostradas nos prints para Admin (uso didático). Não use este projeto como referência de segurança.

## Layout de payload (ordem e Codec)

Animal (comuns): idOng, nome, dataNascimentoAprox, sexo, porte, vacinado, descricao
- Cachorro: raca, nivelAdestramento, seDaBemComCachorros, seDaBemComGatos, seDaBemComCriancas
- Gato: raca, seDaBemComCachorros, seDaBemComGatos, seDaBemComCriancas, acessoExterior, possuiTelamento

Ong: nome, cnpj, endereco, telefone, cpfResponsavel

Usuario (prefixado com CPF, senha, telefone, ativo):
- Adotante: + nomeCompleto, dataNascimento, tipoMoradia, tela, outrosAnimais, descOutrosAnimais, horasForaDeCasa, composicaoFamiliar, viagens, descViagens, jaTevePets, experiencia, motivoAdocao, cienteResponsavel, cienteCustos
- Voluntario: + nome, endereco, idOng, cargo

Adocao: cpfAdotante, idAnimal, dataAdocao

Interesse: cpfAdotante, idAnimal, data, status

ChatThread: idAnimal, cpfAdotante, aberto, criadoEm (epoch long)

ChatMessage: threadId, sender, conteudo, enviadoEm (epoch long)

Manter esta ordem é obrigatório para compatibilidade e leitura correta dos dados anteriores.

## Datasets e arquivos

Diretório `dats/` (criado no primeiro run):
- animais.dat | animais.dat.idx
- ongs.dat | ongs.dat.idx
- adotantes.dat | adotantes.dat.idx
- voluntarios.dat | voluntarios.dat.idx
- adocoes.dat | adocoes.dat.idx
- interesses.dat | interesses.dat.idx
- chat_threads.dat | chat_threads.dat.idx
- chat_msgs.dat | chat_msgs.dat.idx
- backup.zip

## Limitações e avisos

- Sem criptografia de senhas (campo em texto para fins didáticos e inspeção por Admin)
- Sem concorrência multi-processo/threads garantida (RandomAccessFile + sincronização simples)
- Sem testes automatizados; verifique pela CLI
- Mudanças na ordem de campos quebram compatibilidade com dados antigos

## Roadmap / Pendências

- Índices secundários (opcionais):
    - Por `idAnimal` em `ChatThread/ChatMessage` e `Interesse` para filtrar sem varrer
    - Por `cpfAdotante` em `Interesse` e `Adocao`
- UX da CLI: mostrar nome do animal nas listas de threads/mensagens
- Impedir criar interesse em animal já adotado já no input (além de filtrar na listagem)
- Migrações de versão de payload (header.versaoFormato) para evolução de campos
- Testes automatizados (unidade/integração) e scripts de seeds
- Segurança: ocultar senhas por padrão e exigir permissões explícitas para exibição

## Licença

MIT