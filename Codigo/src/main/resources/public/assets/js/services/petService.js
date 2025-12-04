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
     * GET /api/animais
     * 
     * @returns {Promise<{success: boolean, data: Array, message?: string}>}
     */
    async function getAvailablePets() {
        try {
            const response = await fetch('/api/animais', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            
            return {
                success: true,
                data: data // Backend já retorna array de animais
            };
        } catch (error) {
            console.error('Erro ao buscar pets:', error);
            
            // FALLBACK: Se API não estiver disponível, usa mock
            if (error.message.includes('fetch')) {
                console.warn('⚠️  Backend não disponível, usando dados mockados');
                return {
                    success: true,
                    data: [...MOCK_PETS]
                };
            }
            
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
     * GET /api/animais (pega primeiros 6)
     * 
     * @param {number} limit - Quantidade de pets (padrão: 6)
     * @returns {Promise<{success: boolean, data: Array, message?: string}>}
     */
    async function getFeaturedPets(limit = 6) {
        try {
            const result = await getAvailablePets();
            
            if (result.success) {
                return {
                    success: true,
                    data: result.data.slice(0, limit)
                };
            }
            
            return result;
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
     * GET /api/animais/{id}
     * 
     * @param {number} id - ID do animal
     * @returns {Promise<{success: boolean, data?: object, message?: string}>}
     */
    async function getPetById(id) {
        try {
            const response = await fetch(`/api/animais/${id}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 404) {
                    return {
                        success: false,
                        message: `Pet com ID ${id} não encontrado.`
                    };
                }
                throw new Error(`HTTP ${response.status}`);
            }

            const data = await response.json();
            
            return {
                success: true,
                data: data
            };
        } catch (error) {
            console.error('Erro ao buscar pet:', error);
            
            // FALLBACK: tenta buscar no mock
            const pet = MOCK_PETS.find(p => p.id === id);
            if (pet) {
                console.warn('⚠️  Usando dados mockados');
                return { success: true, data: pet };
            }
            
            return {
                success: false,
                message: `Pet com ID ${id} não encontrado.`
            };
        }
    }

    /**
     * Registra interesse de adoção
     * 
     * POST /api/interesses
     * Body: { cpfAdotante: string, idAnimal: number }
     * 
     * @param {string} cpf - CPF do adotante
     * @param {number} animalId - ID do animal
     * @returns {Promise<{success: boolean, data?: object, message: string}>}
     */
    async function registerInterest(cpf, animalId) {
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

        try {
            const response = await fetch('/api/interesses', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    cpfAdotante: cpf,
                    idAnimal: animalId
                })
            });

            const data = await response.json();

            if (!response.ok) {
                return {
                    success: false,
                    message: data.error || 'Erro ao registrar interesse'
                };
            }

            return {
                success: true,
                data: data,
                message: data.message || 'Interesse registrado com sucesso! 🎉'
            };
        } catch (error) {
            console.error('Erro ao registrar interesse:', error);
            
            // FALLBACK: simula registro local
            console.warn('⚠️  Backend não disponível, simulando registro');
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
                message: 'Interesse registrado localmente! 🎉'
            };
        }
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

    /**
     * Busca matches (interesses + adoções) de um adotante
     * 
     * API REAL: GET /api/adotantes/{cpf}/interesses e /api/adotantes/{cpf}/adocoes
     * 
     * @param {string} cpfAdotante - CPF do adotante
     * @returns {Promise<{success: boolean, data: Array}>}
     */
    async function getMyMatches(cpfAdotante) {
        try {
            const API_BASE = 'http://localhost:8080';
            
            // Buscar interesses
            const interessesResponse = await fetch(`${API_BASE}/api/adotantes/${cpfAdotante}/interesses`);
            if (!interessesResponse.ok) {
                throw new Error('Erro ao buscar interesses');
            }
            const interesses = await interessesResponse.json();
            
            // Buscar adoções
            const adocoesResponse = await fetch(`${API_BASE}/api/adotantes/${cpfAdotante}/adocoes`);
            if (!adocoesResponse.ok) {
                throw new Error('Erro ao buscar adoções');
            }
            const adocoes = await adocoesResponse.json();
            
            // Buscar detalhes dos animais
            const animaisResponse = await fetch(`${API_BASE}/api/animais`);
            if (!animaisResponse.ok) {
                throw new Error('Erro ao buscar animais');
            }
            const animais = await animaisResponse.json();
            
            // Mapear interesses com dados dos animais
            const matches = interesses.map(interesse => {
                const animal = animais.find(a => a.id === interesse.idAnimal);
                if (!animal) return null;
                
                // Verificar se foi adotado
                const adocao = adocoes.find(ad => ad.idAnimal === interesse.idAnimal);
                
                return {
                    interesseId: interesse.id,
                    animal: animal,
                    status: adocao ? 'ADOTADO' : interesse.status,
                    dataInteresse: interesse.dataInteresse,
                    dataAdocao: adocao ? adocao.dataAdocao : null
                };
            }).filter(m => m !== null);
            
            return {
                success: true,
                data: matches
            };
            
        } catch (error) {
            console.error('Erro ao buscar matches:', error);
            return {
                success: false,
                error: error.message,
                data: []
            };
        }
    }

    // API pública
    return {
        getAvailablePets,
        getFeaturedPets,
        getPetById,
        registerInterest,
        getInteressesByCpf,
        getMyMatches,
        searchPets
    };
})();

// Exporta para uso global
window.PetService = PetService;
