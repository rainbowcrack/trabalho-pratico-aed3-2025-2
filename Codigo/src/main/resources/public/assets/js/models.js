/**
 * models.js
 * 
 * Definições de tipos (DTOs) que espelham as classes Java do Backend.
 * Estrutura projetada para compatibilidade total com a serialização binária
 * do sistema de persistência (BaseDataFile + Codec).
 */

/**
 * Enumeração de Porte
 * Espelha: br.com.mpet.model.Porte
 */
const Porte = {
    PEQUENO: 'PEQUENO',
    MEDIO: 'MEDIO',
    GRANDE: 'GRANDE'
};

/**
 * Enumeração de Tipo de Animal
 * Espelha o byte de tipo no AnimalDataFileDao:
 * - CACHORRO = tipo byte 1
 * - GATO = tipo byte 2
 */
const TipoAnimal = {
    CACHORRO: 'CACHORRO',
    GATO: 'GATO'
};

/**
 * Enumeração de Sexo
 * Char: 'M' ou 'F'
 */
const Sexo = {
    MACHO: 'M',
    FEMEA: 'F'
};

/**
 * Enumeração de Status de Interesse
 * Espelha: br.com.mpet.model.InteresseStatus
 */
const InteresseStatus = {
    PENDENTE: 'PENDENTE',
    APROVADO: 'APROVADO',
    RECUSADO: 'RECUSADO'
};

/**
 * Enumeração de Temperamento
 * Espelha: br.com.mpet.model.Temperamento
 */
const Temperamento = {
    DOCIL: 'DOCIL',
    BRINCALHAO: 'BRINCALHAO',
    CALMO: 'CALMO',
    ENERGETICO: 'ENERGETICO',
    TIMIDO: 'TIMIDO',
    AGRESSIVO: 'AGRESSIVO',
    SOCIAVEL: 'SOCIAVEL',
    INDEPENDENTE: 'INDEPENDENTE'
};

/**
 * Enumeração de Nível de Adestramento
 * Espelha: br.com.mpet.model.NivelAdestramento
 */
const NivelAdestramento = {
    NENHUM: 'NENHUM',
    BASICO: 'BASICO',
    INTERMEDIARIO: 'INTERMEDIARIO',
    AVANCADO: 'AVANCADO'
};

/**
 * Classe Animal (DTO Base)
 * Espelha: br.com.mpet.model.Animal
 * 
 * Payload no .dat (ordem CRÍTICA para Codec.java):
 * 1. idOng (int)
 * 2. nome (StringU16)
 * 3. dataNascimentoAprox (LocalDate - nullable)
 * 4. sexo (char)
 * 5. porte (Enum)
 * 6. vacinado (TriBoolean - 'V', 'F', 'U')
 * 7. descricao (StringU16)
 * 8. [campos específicos da subclasse]
 */
class Animal {
    constructor({
        id = null,
        idOng = null,
        nome = '',
        dataNascimentoAprox = null, // ISO String "YYYY-MM-DD" ou null
        sexo = Sexo.MACHO,
        porte = Porte.MEDIO,
        vacinado = null, // Boolean ou null (TriBoolean: true='V', false='F', null='U')
        descricao = '',
        tipo = TipoAnimal.CACHORRO // CACHORRO ou GATO
    }) {
        this.id = id;
        this.idOng = idOng;
        this.nome = nome;
        this.dataNascimentoAprox = dataNascimentoAprox;
        this.sexo = sexo;
        this.porte = porte;
        this.vacinado = vacinado;
        this.descricao = descricao;
        this.tipo = tipo;
    }

    /**
     * Calcula idade aproximada em anos
     * @returns {number|null}
     */
    getIdadeAproximada() {
        if (!this.dataNascimentoAprox) return null;
        const hoje = new Date();
        const nascimento = new Date(this.dataNascimentoAprox);
        const diff = hoje - nascimento;
        return Math.floor(diff / (1000 * 60 * 60 * 24 * 365));
    }

    /**
     * Retorna string de vacinação legível
     * @returns {string}
     */
    getVacinadoTexto() {
        if (this.vacinado === true) return 'Vacinado';
        if (this.vacinado === false) return 'Não vacinado';
        return 'Vacinação indefinida';
    }
}

/**
 * Classe Cachorro (DTO)
 * Espelha: br.com.mpet.model.Cachorro extends Animal
 * 
 * Campos específicos após Animal no payload:
 * - raca (StringU16)
 * - nivelAdestramento (Enum)
 */
class Cachorro extends Animal {
    constructor({
        id = null,
        idOng = null,
        nome = '',
        dataNascimentoAprox = null,
        sexo = Sexo.MACHO,
        porte = Porte.MEDIO,
        vacinado = null,
        descricao = '',
        raca = 'SRD',
        nivelAdestramento = NivelAdestramento.NENHUM
    }) {
        super({
            id,
            idOng,
            nome,
            dataNascimentoAprox,
            sexo,
            porte,
            vacinado,
            descricao,
            tipo: TipoAnimal.CACHORRO
        });
        this.raca = raca;
        this.nivelAdestramento = nivelAdestramento;
    }
}

/**
 * Classe Gato (DTO)
 * Espelha: br.com.mpet.model.Gato extends Animal
 * 
 * Campos específicos após Animal no payload:
 * - raca (StringU16)
 * - temperamento (Enum)
 */
class Gato extends Animal {
    constructor({
        id = null,
        idOng = null,
        nome = '',
        dataNascimentoAprox = null,
        sexo = Sexo.MACHO,
        porte = Porte.MEDIO,
        vacinado = null,
        descricao = '',
        raca = 'SRD',
        temperamento = Temperamento.DOCIL
    }) {
        super({
            id,
            idOng,
            nome,
            dataNascimentoAprox,
            sexo,
            porte,
            vacinado,
            descricao,
            tipo: TipoAnimal.GATO
        });
        this.raca = raca;
        this.temperamento = temperamento;
    }
}

/**
 * Classe Interesse (DTO)
 * Espelha: br.com.mpet.model.Interesse
 * 
 * Payload no .dat:
 * - id (int - auto)
 * - cpfAdotante (StringU16)
 * - idAnimal (int)
 * - status (Enum: PENDENTE/APROVADO/RECUSADO)
 * - dataRegistro (LocalDateTime)
 */
class Interesse {
    constructor({
        id = null,
        cpfAdotante = '',
        idAnimal = null,
        status = InteresseStatus.PENDENTE,
        dataRegistro = null // ISO String ou null
    }) {
        this.id = id;
        this.cpfAdotante = cpfAdotante;
        this.idAnimal = idAnimal;
        this.status = status;
        this.dataRegistro = dataRegistro;
    }
}

/**
 * Classe Adotante (DTO)
 * Espelha: br.com.mpet.model.Adotante
 * 
 * Chave lógica: CPF (String)
 * Chave física: idKey (int)
 * Senha: Criptografada com RSA-2048
 */
class Adotante {
    constructor({
        idKey = null,
        cpf = '',
        nome = '',
        email = '',
        telefone = '',
        senha = '', // Armazenada criptografada no backend
        composicaoFamiliar = null,
        tipoMoradia = null
    }) {
        this.idKey = idKey;
        this.cpf = cpf;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.senha = senha;
        this.composicaoFamiliar = composicaoFamiliar;
        this.tipoMoradia = tipoMoradia;
    }
}

// Exporta para uso global (navegador)
window.Models = {
    // Enums
    Porte,
    TipoAnimal,
    Sexo,
    InteresseStatus,
    Temperamento,
    NivelAdestramento,
    
    // Classes
    Animal,
    Cachorro,
    Gato,
    Interesse,
    Adotante
};
