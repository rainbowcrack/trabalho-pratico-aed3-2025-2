/**
 * petAdapter.js
 * 
 * Camada de adaptação entre dados do Backend (formato DAO/DTO) e
 * a camada de visualização (View Models).
 * 
 * O Backend não envia URLs de imagens nem strings formatadas.
 * Este adapter é responsável por:
 * 1. Gerar imagens placeholder baseadas no tipo
 * 2. Formatar strings de exibição
 * 3. Calcular ícones e temas
 * 4. Transformar dados técnicos em elementos visuais
 */

const PetAdapter = (function() {
    // Banco de imagens Unsplash por tipo e raça
    const IMAGE_BANKS = {
        CACHORRO: {
            SRD: [
                'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=800&q=60',
                'https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=800&q=60',
                'https://images.unsplash.com/photo-1587300003388-59208cc962cb?auto=format&fit=crop&w=800&q=60'
            ],
            LABRADOR: [
                'https://images.unsplash.com/photo-1558788353-f76d92427f16?auto=format&fit=crop&w=800&q=60',
                'https://images.unsplash.com/photo-1593134257782-e89567b7718a?auto=format&fit=crop&w=800&q=60'
            ],
            POODLE: [
                'https://images.unsplash.com/photo-1507146426996-ef05306b995a?auto=format&fit=crop&w=800&q=60'
            ],
            'BORDER COLLIE': [
                'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=800&q=60'
            ],
            DEFAULT: [
                'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=800&q=60',
                'https://images.unsplash.com/photo-1561037404-61cd46aa615b?auto=format&fit=crop&w=800&q=60'
            ]
        },
        GATO: {
            SRD: [
                'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=800&q=60',
                'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=800&q=60'
            ],
            SIAMES: [
                'https://images.unsplash.com/photo-1543852786-1cf6624b9987?auto=format&fit=crop&w=800&q=60'
            ],
            PERSA: [
                'https://images.unsplash.com/photo-1511045999812-7a32f6081c71?auto=format&fit=crop&w=800&q=60'
            ],
            DEFAULT: [
                'https://images.unsplash.com/photo-1513360371669-4adf3dd7dff8?auto=format&fit=crop&w=800&q=60',
                'https://images.unsplash.com/photo-1574158622682-e40e69881006?auto=format&fit=crop&w=800&q=60'
            ]
        }
    };

    /**
     * Gera URL de imagem baseada no tipo e raça
     * @param {string} tipo - CACHORRO ou GATO
     * @param {string} raca - Raça do animal
     * @param {number} id - ID para hash consistente
     * @returns {string}
     */
    function getImageUrl(tipo, raca, id) {
        const tipoKey = tipo.toUpperCase();
        const racaKey = raca.toUpperCase();
        
        // Busca imagens específicas da raça
        let images = IMAGE_BANKS[tipoKey]?.[racaKey] || IMAGE_BANKS[tipoKey]?.DEFAULT;
        
        if (!images || images.length === 0) {
            images = IMAGE_BANKS.CACHORRO.DEFAULT; // Fallback
        }

        // Usa ID como seed para escolha consistente
        const index = id ? (id % images.length) : 0;
        return images[index];
    }

    /**
     * Retorna ícone emoji baseado no tipo
     * @param {string} tipo - CACHORRO ou GATO
     * @returns {string}
     */
    function getIcono(tipo) {
        return tipo === 'CACHORRO' ? '🐶' : '🐱';
    }

    /**
     * Retorna tema CSS baseado no tipo
     * @param {string} tipo - CACHORRO ou GATO
     * @returns {string} 'dog' ou 'cat'
     */
    function getTema(tipo) {
        return tipo === 'CACHORRO' ? 'dog' : 'cat';
    }

    /**
     * Formata sexo para exibição
     * @param {string} sexo - 'M' ou 'F'
     * @returns {string}
     */
    function formatSexo(sexo) {
        return sexo === 'M' ? 'Macho' : 'Fêmea';
    }

    /**
     * Formata porte para exibição
     * @param {string} porte - PEQUENO, MEDIO, GRANDE
     * @returns {string}
     */
    function formatPorte(porte) {
        const map = {
            'PEQUENO': 'Pequeno',
            'MEDIO': 'Médio',
            'GRANDE': 'Grande'
        };
        return map[porte] || porte;
    }

    /**
     * Formata status de vacinação
     * @param {boolean|null} vacinado - TriBoolean
     * @returns {string}
     */
    function formatVacinacao(vacinado) {
        if (vacinado === true) return '✓ Vacinado';
        if (vacinado === false) return '✗ Não vacinado';
        return '? Vacinação indefinida';
    }

    /**
     * Calcula idade em anos a partir de data ISO
     * @param {string|null} dataIso - "YYYY-MM-DD"
     * @returns {number|null}
     */
    function calcularIdade(dataIso) {
        if (!dataIso) return null;
        const hoje = new Date();
        const nascimento = new Date(dataIso);
        const diff = hoje - nascimento;
        return Math.floor(diff / (1000 * 60 * 60 * 24 * 365));
    }

    /**
     * Formata string de detalhes (linha secundária do card)
     * Exemplo: "Fêmea • 2 anos • Labrador"
     * 
     * @param {object} pet - Animal DTO
     * @returns {string}
     */
    function formatDetalhes(pet) {
        const partes = [];

        // Sexo
        partes.push(formatSexo(pet.sexo));

        // Idade (se disponível)
        const idade = calcularIdade(pet.dataNascimentoAprox);
        if (idade !== null) {
            partes.push(idade === 1 ? '1 ano' : `${idade} anos`);
        }

        // Raça
        if (pet.raca) {
            partes.push(pet.raca);
        }

        return partes.join(' • ');
    }

    /**
     * Gera badge/tag principal do pet
     * Prioridade: Vacinado > Temperamento/Adestramento > Porte
     * 
     * @param {object} pet - Animal DTO
     * @returns {string}
     */
    function generateTag(pet) {
        // Prioridade 1: Vacinação
        if (pet.vacinado === true) {
            return 'Vacinado';
        }

        // Prioridade 2: Característica específica
        if (pet.tipo === 'GATO' && pet.temperamento) {
            const tempMap = {
                'DOCIL': 'Dócil',
                'BRINCALHAO': 'Brincalhão',
                'CALMO': 'Calmo',
                'ENERGETICO': 'Energético',
                'SOCIAVEL': 'Sociável',
                'INDEPENDENTE': 'Independente'
            };
            return tempMap[pet.temperamento] || pet.temperamento;
        }

        if (pet.tipo === 'CACHORRO' && pet.nivelAdestramento && pet.nivelAdestramento !== 'NENHUM') {
            const adestrMap = {
                'BASICO': 'Adestrado (Básico)',
                'INTERMEDIARIO': 'Adestrado (Intermediário)',
                'AVANCADO': 'Adestrado (Avançado)'
            };
            return adestrMap[pet.nivelAdestramento] || 'Adestrado';
        }

        // Prioridade 3: Porte
        return formatPorte(pet.porte);
    }

    /**
     * Adapta um Pet do formato Backend para o formato de Card da UI
     * 
     * @param {object} petBackendData - Animal DTO (Cachorro ou Gato)
     * @returns {object} View Model para renderização
     */
    function adaptToCard(petBackendData) {
        return {
            // Dados originais (para referência)
            id: petBackendData.id,
            idOng: petBackendData.idOng,
            tipo: petBackendData.tipo,
            
            // Dados formatados para UI
            nome: petBackendData.nome,
            detalhes: formatDetalhes(petBackendData),
            descricao: petBackendData.descricao || 'Sem descrição disponível.',
            imagem: getImageUrl(petBackendData.tipo, petBackendData.raca, petBackendData.id),
            icone: getIcono(petBackendData.tipo),
            tema: getTema(petBackendData.tipo),
            tag: generateTag(petBackendData),
            
            // Dados adicionais úteis
            sexo: formatSexo(petBackendData.sexo),
            porte: formatPorte(petBackendData.porte),
            raca: petBackendData.raca || 'SRD',
            idade: calcularIdade(petBackendData.dataNascimentoAprox),
            vacinacao: formatVacinacao(petBackendData.vacinado),
            
            // Metadados
            dataOriginal: petBackendData // Referência completa se necessário
        };
    }

    /**
     * Adapta um Pet para mini-card (usado na home)
     * Versão simplificada do adaptToCard
     * 
     * @param {object} petBackendData - Animal DTO
     * @returns {object} View Model mini
     */
    function adaptToMiniCard(petBackendData) {
        return {
            id: petBackendData.id,
            nome: petBackendData.nome,
            tag: generateTag(petBackendData),
            imagem: getImageUrl(petBackendData.tipo, petBackendData.raca, petBackendData.id),
            tipo: getTema(petBackendData.tipo)
        };
    }

    /**
     * Adapta lista de pets
     * @param {Array} pets - Array de Animal DTOs
     * @returns {Array} Array de View Models
     */
    function adaptList(pets) {
        return pets.map(adaptToCard);
    }

    /**
     * Adapta lista para mini-cards
     * @param {Array} pets - Array de Animal DTOs
     * @returns {Array} Array de View Models mini
     */
    function adaptMiniList(pets) {
        return pets.map(adaptToMiniCard);
    }

    // API pública
    return {
        adaptToCard,
        adaptToMiniCard,
        adaptList,
        adaptMiniList,
        getImageUrl,
        getIcono,
        getTema,
        formatSexo,
        formatPorte,
        formatVacinacao,
        formatDetalhes,
        generateTag,
        calcularIdade
    };
})();

// Exporta para uso global
window.PetAdapter = PetAdapter;
