# MPet Backend - Copilot Instructions

## Project Overview
MPet is a pet adoption matching system with a **custom binary persistence layer** using file-based storage instead of traditional databases. The backend implements CRUD operations for Animals (polymorphic: Cachorro/Gato), ONGs, Adotantes, and Voluntários with B+ Tree and Extensible Hash indexing.

## Architecture & Core Concepts

### Binary File Structure
All entities persist to `.dat` files with this layout:
```
[tipo(1)][tombstone(1)][id(4)][len(4)][payload(len)]
```
- **Header**: 128 bytes fixed (managed by `FileHeaderHelper`) with `versaoFormato`, `proximoId`, `countAtivos`
- **Records**: Start at offset 128+. Tombstone `0`=active, `1`=deleted
- **Files location**: `dats/` directory (relative to working dir)

### Serialization Protocol (Codec.java)
**Critical**: Field order in payload MUST match exactly as documented. See `Codec.java` for encoding rules:
- **Strings**: `0xFFFF` = null, `0x0000` = empty string, length prefix U16 + UTF-8 bytes
- **Enums**: `ordinal+1` (0 = null)
- **Tri-Boolean**: `'V'` (true), `'F'` (false), `'U'` (undefined/null)
- **LocalDate**: 1 byte flag (0=null) + year(int) + month(byte) + day(byte)

**Example Animal payload order**:
1. idOng (int)
2. nome (StringU16)
3. dataNascimentoAprox (LocalDate)
4. sexo (char)
5. porte (Enum)
6. vacinado (TriBoolean)
7. descricao (StringU16)
8. *Then species-specific fields (Cachorro/Gato)*

### Index Structures
- **B+ Tree** (`BTree.java`): Used for Animal primary index (id → offset). Order=4, persists to `.idx` file
- **HashMap in-memory cache**: Each DAO maintains `Map<Key, Long>` for fast offset lookup
- **Index rebuilding**: `rebuildIfEmpty()` scans `.dat` and reconstructs index from scratch

### DAO Pattern (AnimalDataFileDao.java)
All DAOs extend `BaseDataFile<T>`:
1. **Create**: Assigns sequential ID from header, encodes payload, appends record, updates index
2. **Read**: Lookup offset in index/BTree, read record at offset, decode payload
3. **Update**: If same size → overwrite in-place; if different → tombstone old + append new + update index
4. **Delete**: Set tombstone byte=1, remove from index (physical space not reclaimed until vacuum)
5. **Vacuum**: Creates temp file with only active records, swaps `.dat` and rebuilds `.idx`

### Entity Polymorphism
`Animal` is abstract with concrete types `Cachorro` and `Gato`:
- Tipo byte: `1`=CACHORRO, `2`=GATO
- Each subclass has specific fields after common Animal fields
- `tipoPara()` and `decodeAnimal()` handle type detection and instantiation

## Development Workflows

### Build & Run
```bash
# Compile
mvn -f Codigo/pom.xml -q -DskipTests package

# Run CLI
java -cp "Codigo/target/classes" br.com.mpet.Interface

# Or use Makefile (uses PowerShell internally)
make build
make run
```

**Note**: Makefile is configured for Windows PowerShell (`pwsh.exe`). On Linux, adapt commands or use Maven directly.

### Data Files Management
- Data files stored in `dats/`: `animais.dat`, `animais.dat.idx`, `ongs.dat`, etc.
- **Backup/Restore**: CLI options create/extract `backup.zip` with all `.dat` and `.idx` files
- **Vacuum**: Compacts files by removing tombstoned records. Always close/reopen DAOs after vacuum

### CLI Authentication System
`Interface.java` implements role-based login:
- **Admin**: username=`admin`, password=`admin` (hardcoded)
- **Adotante/Voluntário**: Authenticate via CPF + senha from their respective DAOs
- After login, different menu hierarchies based on role

## Project-Specific Patterns

### Idempotent Close Pattern
`BaseDataFile.close()` and index classes tolerate multiple `close()` calls without exceptions. Always persist header before closing RAF.

### Error Handling Convention
- DAOs throw `IOException` for file operations
- CLI catches exceptions and displays with ANSI color codes (`ANSI_RED` for errors, `ANSI_GREEN` for success)
- No custom exception hierarchy - use standard Java exceptions

### Null Handling in Serialization
When encoding/decoding:
- Check for null BEFORE calling `Codec` methods
- Use sentinels: `0xFFFF` for strings, `0x00` for enums, `'U'` for booleans
- Optional fields (like `dataNascimentoAprox`) use flag bytes

### Vacuum Workflow
Critical sequence when vacuum is called:
1. Create new temporary DAO with `_tmp.dat` file
2. Iterate active records from old DAO using `listAllActive()`
3. Clone each entity and create in temp DAO (assigns NEW offsets)
4. Close both DAOs
5. Delete old `.dat` and `.idx` files
6. Rename temp files to production names
7. **Must reopen DAO** to use new files

## Common Pitfalls

1. **Index desync**: If you manually modify `.dat`, index becomes invalid. Always use DAO methods or call `rebuildIfEmpty()`
2. **Payload order**: Adding/reordering fields breaks existing data. Version migrations not yet implemented
3. **File locking**: RandomAccessFile keeps file open. Close DAO before file operations (rename/delete)
4. **ID reuse after vacuum**: Vacuum resets physical layout but IDs remain unique (tracked in header)
5. **String encoding**: Avoid trimming strings during codec operations - preserve exact bytes as written

## Key Files Reference
- `Interface.java`: CLI entry point, menus, all user interactions
- `AnimalDataFileDao.java`: Complete CRUD implementation reference (430+ lines)
- `Codec.java`: All serialization logic with extensive examples in comments
- `BaseDataFile.java`: Common DAO infrastructure (header, append, tombstone)
- `BTree.java`: B+ tree index implementation (order-4, file-backed)
- `FileHeaderHelper.java`: 128-byte header format for `.dat` files

## Testing Approach
No automated test suite exists. Manual testing via CLI:
1. Create entities across all types
2. Test read/update/delete operations
3. Run vacuum and verify data integrity
4. Backup → Delete files → Restore → Verify
5. Test large datasets (100+ records) for index performance

## Dependencies
- **Java 17** (configured in `pom.xml`)
- **Maven 3.x** for build
- **apache commons-compress 1.26.2**: Used for ZIP backup/restore
- No web framework - pure CLI application

## Future Evolution Notes
Files marked for future use but not currently persisted:
- `HistoricoMedico.java`, `Vacina.java`, `Exame.java` - medical records (models exist, persistence TODO)
- Hash Extensível (`HashIndex`) - mentioned in README for 1:N relationships, implementation incomplete
- Compression (LZW) and encryption (XOR) - mentioned in comments but not implemented in current codebase
