/**
 * petService.js
 * 
 * Camada de serviço para operações de Pet.
 * Simula comportamento assíncrono de uma API REST.
 * 
 * IMPORTANTE: Este é um serviço MOCKADO para desenvolvimento.
 * Para integração com o Backend Java:
 * 1. Substituir MOCK_PETS por chamadas fetch('/api/animais')
 * 2. Implementar tratamento de erros HTTP
 * 3. Adicionar headers de autenticação (JWT/Bearer)
 * 4. Mapear respostas do backend para DTOs
 */

const PetService = (function() {
    // ========================================
    // DADOS MOCKADOS (Formato Backend/DTO)
    // ========================================
    
    /**
     * Array mockado de animais no formato do Backend.
     * Estrutura EXATA das classes Animal/Cachorro/Gato do Java.
     * 
     * Quando integrar: REMOVER este array e buscar de GET /api/animais
     */
    const MOCK_PETS = [
        // Cachorros 🐶
        {
            id: 1,
            idOng: 1,
            nome: 'Luna',
            dataNascimentoAprox: '2022-03-15', // ISO String
            sexo: 'F',
            porte: 'MEDIO',
            vacinado: true,
            descricao: 'Carinhosa, adora brincar e está pronta para um lar amoroso!',
            tipo: 'CACHORRO',
            raca: 'SRD',
            nivelAdestramento: 'BASICO'
        },
        {
            id: 2,
            idOng: 1,
            nome: 'Thor',
            dataNascimentoAprox: '2021-06-20',
            sexo: 'M',
            porte: 'GRANDE',
            vacinado: true,
            descricao: 'Brincalhão, ama correr e se dá muito bem com crianças.',
            tipo: 'CACHORRO',
            raca: 'Labrador',
            nivelAdestramento: 'INTERMEDIARIO'
        },
        {
            id: 3,
            idOng: 1,
            nome: 'Mel',
            dataNascimentoAprox: '2023-01-10',
            sexo: 'F',
            porte: 'PEQUENO',
            vacinado: true,
            descricao: 'Dócil, gosta de colo e está totalmente vacinada.',
            tipo: 'CACHORRO',
            raca: 'Poodle',
            nivelAdestramento: 'BASICO'
        },
        {
            id: 4,
            idOng: 2,
            nome: 'Bob',
            dataNascimentoAprox: '2020-08-05',
            sexo: 'M',
            porte: 'MEDIO',
            vacinado: false,
            descricao: 'Companheiro fiel e muito leal ao tutor.',
            tipo: 'CACHORRO',
            raca: 'SRD',
            nivelAdestramento: 'NENHUM'
        },
        {
            id: 5,
            idOng: 2,
            nome: 'Nina',
            dataNascimentoAprox: '2022-11-30',
            sexo: 'F',
            porte: 'MEDIO',
            vacinado: true,
            descricao: 'Energia alta, perfeita para quem gosta de atividades ao ar livre.',
            tipo: 'CACHORRO',
            raca: 'Border Collie',
            nivelAdestramento: 'AVANCADO'
        },
        
        // Gatos 🐱
        {
            id: 6,
            idOng: 1,
            nome: 'Mimi',
            dataNascimentoAprox: '2022-05-12',
            sexo: 'F',
            porte: 'PEQUENO',
            vacinado: true,
            descricao: 'Carinhosa, adora janelas de sol e lugares quentinhos.',
            tipo: 'GATO',
            raca: 'SRD',
            temperamento: 'DOCIL'
        },
        {
            id: 7,
            idOng: 2,
            nome: 'Zeca',
            dataNascimentoAprox: '2021-09-18',
            sexo: 'M',
            porte: 'MEDIO',
            vacinado: true,
            descricao: 'Elegante e muito curioso. Adora explorar.',
            tipo: 'GATO',
            raca: 'Siamês',
            temperamento: 'SOCIAVEL'
        },
        {
            id: 8,
            idOng: 1,
            nome: 'Lola',
            dataNascimentoAprox: '2023-02-28',
            sexo: 'F',
            porte: 'PEQUENO',
            vacinado: null, // TriBoolean: indefinido
            descricao: 'Calma e dorminhoca. Perfeita para apartamento.',
            tipo: 'GATO',
            raca: 'Persa',
            temperamento: 'CALMO'
        },
        {
            id: 9,
            idOng: 2,
            nome: 'Fred',
            dataNascimentoAprox: '2019-04-22',
            sexo: 'M',
            porte: 'MEDIO',
            vacinado: false,
            descricao: 'Independente e muito limpinho. Ideal para quem tem rotina agitada.',
            tipo: 'GATO',
            raca: 'SRD',
            temperamento: 'INDEPENDENTE'
        },
        {
            id: 10,
            idOng: 1,
            nome: 'Pérola',
            dataNascimentoAprox: '2022-12-05',
            sexo: 'F',
            porte: 'PEQUENO',
            vacinado: true,
            descricao: 'Brincalhona e cheia de energia. Adora brinquedos.',
            tipo: 'GATO',
            raca: 'SRD',
            temperamento: 'BRINCALHAO'
        }
    ];

    /**
     * Array mockado de interesses registrados
     * Simula a tabela interesses.dat
     */
    const MOCK_INTERESSES = [];

    // ========================================
    // FUNÇÕES AUXILIARES
    // ========================================

    /**
     * Simula delay de rede (100-500ms)
     * @returns {Promise<void>}
     */
    function simulateNetworkDelay() {
        const delay = 100 + Math.random() * 400;
        return new Promise(resolve => setTimeout(resolve, delay));
    }

    /**
     * Valida CPF (formato básico)
     * @param {string} cpf 
     * @returns {boolean}
     */
    function isValidCpf(cpf) {
        if (!cpf) return false;
        const cleaned = cpf.replace(/\D/g, '');
        return cleaned.length === 11;
    }

    /**
     * Gera ID sequencial para novos registros
     * @returns {number}
     */
    function generateId() {
        return MOCK_INTERESSES.length > 0 
            ? Math.max(...MOCK_INTERESSES.map(i => i.id)) + 1 
            : 1;
    }

    // ========================================
    // API PÚBLICA
    // ========================================

    /**
     * Busca todos os pets disponíveis para adoção
     * 
     * MOCK: Retorna MOCK_PETS
     * FUTURO: GET /api/animais
     * 
     * @returns {Promise<{success: boolean, data: Array, message?: string}>}
     */
    async function getAvailablePets() {
        await simulateNetworkDelay();

        try {
            // Simula busca bem-sucedida
            return {
                success: true,
                data: [...MOCK_PETS] // Clone para evitar mutação
            };
        } catch (error) {
            console.error('Erro ao buscar pets:', error);
            return {
                success: false,
                data: [],
                message: 'Erro ao carregar pets. Tente novamente.'
            };
        }
    }

    /**
     * Busca os 6 primeiros pets em destaque
     * 
     * MOCK: Retorna primeiros 6 do MOCK_PETS
     * FUTURO: GET /api/animais/featured?limit=6
     * 
     * @param {number} limit - Quantidade de pets (padrão: 6)
     * @returns {Promise<{success: boolean, data: Array, message?: string}>}
     */
    async function getFeaturedPets(limit = 6) {
        await simulateNetworkDelay();

        try {
            const featured = MOCK_PETS.slice(0, limit);
            return {
                success: true,
                data: featured
            };
        } catch (error) {
            console.error('Erro ao buscar pets em destaque:', error);
            return {
                success: false,
                data: [],
                message: 'Erro ao carregar pets em destaque.'
            };
        }
    }

    /**
     * Busca um pet específico por ID
     * 
     * MOCK: Busca no MOCK_PETS
     * FUTURO: GET /api/animais/{id}
     * 
     * @param {number} id - ID do animal
     * @returns {Promise<{success: boolean, data?: object, message?: string}>}
     */
    async function getPetById(id) {
        await simulateNetworkDelay();

        const pet = MOCK_PETS.find(p => p.id === id);
        
        if (pet) {
            return {
                success: true,
                data: pet
            };
        } else {
            return {
                success: false,
                message: `Pet com ID ${id} não encontrado.`
            };
        }
    }

    /**
     * Registra interesse de adoção
     * 
     * MOCK: Adiciona em MOCK_INTERESSES
     * FUTURO: POST /api/interesses
     * Body: { cpfAdotante: string, idAnimal: number }
     * 
     * @param {string} cpf - CPF do adotante
     * @param {number} animalId - ID do animal
     * @returns {Promise<{success: boolean, data?: object, message: string}>}
     */
    async function registerInterest(cpf, animalId) {
        await simulateNetworkDelay();

        // Validações
        if (!isValidCpf(cpf)) {
            return {
                success: false,
                message: 'CPF inválido.'
            };
        }

        if (!animalId || typeof animalId !== 'number') {
            return {
                success: false,
                message: 'ID do animal inválido.'
            };
        }

        // Verifica se animal existe
        const pet = MOCK_PETS.find(p => p.id === animalId);
        if (!pet) {
            return {
                success: false,
                message: `Animal com ID ${animalId} não existe.`
            };
        }

        // Verifica duplicata
        const existente = MOCK_INTERESSES.find(
            i => i.cpfAdotante === cpf && i.idAnimal === animalId
        );
        
        if (existente) {
            return {
                success: false,
                message: `Você já manifestou interesse por ${pet.nome}.`
            };
        }

        // Cria novo interesse
        const novoInteresse = {
            id: generateId(),
            cpfAdotante: cpf,
            idAnimal: animalId,
            status: 'PENDENTE',
            dataRegistro: new Date().toISOString()
        };

        MOCK_INTERESSES.push(novoInteresse);

        return {
            success: true,
            data: novoInteresse,
            message: `Interesse por ${pet.nome} registrado com sucesso! 🎉`
        };
    }

    /**
     * Busca interesses de um adotante
     * 
     * MOCK: Filtra MOCK_INTERESSES
     * FUTURO: GET /api/interesses?cpf={cpf}
     * 
     * @param {string} cpf - CPF do adotante
     * @returns {Promise<{success: boolean, data: Array, message?: string}>}
     */
    async function getInteressesByCpf(cpf) {
        await simulateNetworkDelay();

        if (!isValidCpf(cpf)) {
            return {
                success: false,
                data: [],
                message: 'CPF inválido.'
            };
        }

        const interesses = MOCK_INTERESSES.filter(i => i.cpfAdotante === cpf);

        return {
            success: true,
            data: interesses
        };
    }

    /**
     * Busca pets por filtro
     * 
     * MOCK: Filtra MOCK_PETS
     * FUTURO: GET /api/animais?tipo={tipo}&porte={porte}...
     * 
     * @param {object} filtros - { tipo?, porte?, sexo?, vacinado? }
     * @returns {Promise<{success: boolean, data: Array}>}
     */
    async function searchPets(filtros = {}) {
        await simulateNetworkDelay();

        let resultado = [...MOCK_PETS];

        // Filtro por tipo
        if (filtros.tipo) {
            resultado = resultado.filter(p => p.tipo === filtros.tipo);
        }

        // Filtro por porte
        if (filtros.porte) {
            resultado = resultado.filter(p => p.porte === filtros.porte);
        }

        // Filtro por sexo
        if (filtros.sexo) {
            resultado = resultado.filter(p => p.sexo === filtros.sexo);
        }

        // Filtro por vacinação
        if (filtros.vacinado !== undefined) {
            resultado = resultado.filter(p => p.vacinado === filtros.vacinado);
        }

        return {
            success: true,
            data: resultado
        };
    }

    // API pública
    return {
        getAvailablePets,
        getFeaturedPets,
        getPetById,
        registerInterest,
        getInteressesByCpf,
        searchPets
    };
})();

// Exporta para uso global
window.PetService = PetService;
