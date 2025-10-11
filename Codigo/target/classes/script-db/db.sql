-- script do banco de dados PetMatch

-- aabela de Adotantes
CREATE TABLE IF NOT EXISTS Adotantes (
    idAdotante INT PRIMARY KEY,
    cpf CHAR(11),
    nome VARCHAR(100) NOT NULL, 
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(100) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- tabela de ONGs
CREATE TABLE IF NOT EXISTS Ongs (
    idOng INT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descricao TEXT DEFAULT '0',
    contato VARCHAR(100) NOT NULL,
    endereco TEXT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- tabela de Voluntarios
CREATE TABLE IF NOT EXISTS Voluntarios (
    idVoluntario INT PRIMARY KEY,
    cpf CHAR(11) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    contato VARCHAR(100),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- tabela de Animais
CREATE TABLE IF NOT EXISTS Animais (
    idAnimal INT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    especie VARCHAR(50),
    idade INT,
    sexo CHAR,
    descricao TEXT,
    idAdotante INT,  
    idOng INT,      
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT FK_ANIMAL_ADOTANTE FOREIGN KEY (idAdotante) REFERENCES Adotantes(idAdotante) 
        ON DELETE SET NULL ON UPDATE CASCADE,
    
    CONSTRAINT FK_ANIMAL_ONG FOREIGN KEY (idOng) REFERENCES Ongs(idOng)
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- tabela de Ficha Medica (1:1 com Animal)
CREATE TABLE IF NOT EXISTS FichaMedica (
    idFicha INT PRIMARY KEY,
    idAnimal INT UNIQUE, 
    descricao TEXT,
    
    CONSTRAINT FK_FICHA_ANIMAL FOREIGN KEY (idAnimal) REFERENCES Animais(idAnimal)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- relacao N:N entre Voluntarios e ONGs
CREATE TABLE IF NOT EXISTS Participa (
    idVoluntario INT,
    idOng INT,
    PRIMARY KEY(idVoluntario, idOng),
    
    CONSTRAINT FK_PARTICIPA_VOLUNTARIO FOREIGN KEY (idVoluntario) REFERENCES Voluntarios(idVoluntario)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT FK_PARTICIPA_ONG FOREIGN KEY (idOng) REFERENCES Ongs(idOng)
        ON DELETE CASCADE ON UPDATE CASCADE
);
