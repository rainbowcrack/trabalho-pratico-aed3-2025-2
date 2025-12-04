/**
 * petService.js
 * 
 * Camada de serviço para operações de Pet.
 * Integrado com a API REST do Backend Java.
 * 
 * Endpoints utilizados:
 * - GET /api/animais - Lista todos os animais
 * - GET /api/animais/:id - Busca animal por ID
 * - POST /api/interesses - Registra interesse de adoção
 * - GET /api/interesses?cpfAdotante= - Lista interesses por CPF
 */

const PetService = (function() {
    // ========================================
    // FUNÇÕES AUXILIARES
    // ========================================

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
            
            return {
                success: false,
                data: [],
                message: 'Erro ao carregar pets. Verifique se o servidor está ativo.'
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
            
            return {
                success: false,
                message: `Pet com ID ${id} não encontrado ou erro no servidor.`
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
            
            return {
                success: false,
                message: 'Erro ao registrar interesse. Verifique se o servidor está ativo.'
            };
        }
    }

    /**
     * Busca interesses de um adotante
     * 
     * GET /api/interesses?cpfAdotante={cpf}
     * 
     * @param {string} cpf - CPF do adotante
     * @returns {Promise<{success: boolean, data: Array, message?: string}>}
     */
    async function getInteressesByCpf(cpf) {
        if (!isValidCpf(cpf)) {
            return {
                success: false,
                data: [],
                message: 'CPF inválido.'
            };
        }

        try {
            const response = await fetch(`/api/interesses?cpfAdotante=${encodeURIComponent(cpf)}`, {
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
                data: data
            };
        } catch (error) {
            console.error('Erro ao buscar interesses:', error);
            
            return {
                success: false,
                data: [],
                message: 'Erro ao carregar interesses. Verifique se o servidor está ativo.'
            };
        }
    }

    /**
     * Busca pets por filtro
     * 
     * GET /api/animais com query params para filtros
     * 
     * @param {object} filtros - { tipo?, porte?, sexo?, vacinado? }
     * @returns {Promise<{success: boolean, data: Array}>}
     */
    async function searchPets(filtros = {}) {
        try {
            // Constrói query params
            const params = new URLSearchParams();
            if (filtros.tipo) params.append('tipo', filtros.tipo);
            if (filtros.porte) params.append('porte', filtros.porte);
            if (filtros.sexo) params.append('sexo', filtros.sexo);
            if (filtros.vacinado !== undefined) params.append('vacinado', filtros.vacinado);

            const url = `/api/animais${params.toString() ? '?' + params.toString() : ''}`;
            
            const response = await fetch(url, {
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
                data: data
            };
        } catch (error) {
            console.error('Erro ao buscar pets com filtros:', error);
            
            return {
                success: false,
                data: [],
                message: 'Erro ao buscar pets. Verifique se o servidor está ativo.'
            };
        }
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
