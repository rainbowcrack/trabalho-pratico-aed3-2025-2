/**
 * index.js - Página de Match (Refatorado)
 * 
 * Arquitetura limpa com separação de responsabilidades:
 * - Dados: PetService (camada de dados mockada)
 * - Transformação: PetAdapter (converte DTO → View Model)
 * - Autenticação: SessionManager (controle de login)
 * - View: Funções de renderização neste arquivo
 */

// Estado da aplicação
let currentPets = []; // Pets carregados do serviço
let currentIndex = 0; // Índice do pet atual

/**
 * Cria corações voando pela tela com cor temática
 * @param {string} color - Cor dos corações
 */
function createHearts(color = '#ff3b3b') {
    let heartContainer = document.getElementById('heartContainer');
    if (!heartContainer) {
        heartContainer = document.createElement('div');
        heartContainer.id = 'heartContainer';
        document.body.appendChild(heartContainer);
    }
    for (let i = 0; i < 14; i++) {
        const heart = document.createElement('div');
        heart.className = 'flying-heart';
        heart.style.left = `${Math.random() * 100}%`;
        heart.style.bottom = `${Math.random() * 40 + 10}px`;
        heart.style.animationDelay = `${Math.random() * 0.3}s`;
        heart.dataset.color = color;
        heart.style.color = color;
        heartContainer.appendChild(heart);
        setTimeout(() => heart.remove(), 900);
    }
}

/**
 * Renderiza card do pet
 * @param {number} index - Índice do pet no array
 */
function renderCard(index) {
    const container = document.getElementById('appContainer');
    container.innerHTML = '';

    // Verifica se acabaram os pets
    if (index >= currentPets.length) {
        container.innerHTML = `
            <div class="pet-card" style="text-align: center; padding: 40px;">
                <div style="font-size: 4rem; margin-bottom: 20px;">🎉</div>
                <h2>Você viu todos os pets disponíveis!</h2>
                <p style="margin: 20px 0;">Continue voltando para conhecer novos amiguinhos.</p>
                <button onclick="window.location.reload()" class="action-btn like" style="margin: 20px auto; display: block;">
                    Recomeçar
                </button>
            </div>
        `;
        return;
    }

    const pet = currentPets[index];

    // Ajusta tema por tipo
    document.body.setAttribute('data-theme', pet.tema);

    // Cria card
    const card = document.createElement('div');
    card.className = 'pet-card';
    card.innerHTML = `
        <img class="pet-photo" src="${pet.imagem}" alt="Foto de ${pet.nome}">
        <div class="pet-info">
            <span class="pet-name">${pet.icone} ${pet.nome}</span>
            <span class="pet-details">${pet.detalhes}</span>
            <p class="pet-description">${pet.descricao}</p>
            <div class="pet-badges">
                <span class="badge">${pet.tag}</span>
                <span class="badge">${pet.porte}</span>
            </div>
        </div>
        <div class="actions">
            <button class="action-btn dislike" title="Não gostei">✖</button>
            <button class="action-btn like" title="Gostei">❤</button>
        </div>
    `;
    container.appendChild(card);

    // Handler: Curtir (registra interesse)
    card.querySelector('.like').onclick = async () => {
        const likeButton = card.querySelector('.like');
        
        // Mostra loading no botão
        const hideButtonLoading = LoadingSpinner.showButton(likeButton, 'sending');
        
        // Desabilita botões durante operação
        card.querySelectorAll('.action-btn').forEach(btn => btn.disabled = true);

        // Animação de saída
        card.style.transition = "transform 0.5s cubic-bezier(.4,0,.2,1), opacity 0.5s";
        card.style.transform = "translateX(400px) rotate(15deg)";
        card.style.opacity = "0";

        // Corações voando (cor por tema)
        const color = pet.tema === 'dog' ? '#22c55e' : '#ff3b3b';
        createHearts(color);

        // Registra interesse
        const cpf = SessionManager.getCurrentCpf();
        const result = await PetService.registerInterest(cpf, pet.id);

        // Esconde loading
        hideButtonLoading();

        // Feedback
        if (result.success) {
            showAlert(result.message, 'success');
        } else {
            showAlert(result.message, 'error');
        }

        // Avança para próximo
        setTimeout(() => {
            currentIndex++;
            renderCard(currentIndex);
        }, 500);
    };

    // Handler: Recusar
    card.querySelector('.dislike').onclick = () => {
        // Animação de saída
        card.style.transition = "transform 0.5s cubic-bezier(.4,0,.2,1), opacity 0.5s";
        card.style.transform = "translateX(-400px) rotate(-15deg)";
        card.style.opacity = "0";

        // Avança para próximo
        setTimeout(() => {
            currentIndex++;
            renderCard(currentIndex);
        }, 500);
    };
}

/**
 * Inicializa a aplicação
 */
async function init() {
    // 1. PROTEÇÃO DE ROTA - Verifica autenticação
    if (!SessionManager.isAuthenticated()) {
        // Salva URL para retornar depois do login
        sessionStorage.setItem('mpet_return_url', 'match.html');
        window.location.href = 'login.html';
        return;
    }

    // 2. Busca pets do serviço com loading
    const container = document.getElementById('appContainer');
    const hideLoading = LoadingSpinner.show(container, 'pets');
    
    try {
        const result = await PetService.getAvailablePets();

        if (result.success && result.data.length > 0) {
            // 3. Adapta dados para View Models
            currentPets = PetAdapter.adaptList(result.data);
            
            // 4. Esconde loading e renderiza primeiro card
            hideLoading();
            currentIndex = 0;
            renderCard(currentIndex);
        } else {
            // Esconde loading e mostra empty state
            hideLoading();
            EmptyState.render(container, 'animals', {
                message: 'No momento não há pets disponíveis para adoção. Volte em breve para conhecer novos amiguinhos!',
                ctaText: 'Atualizar página',
                ctaAction: () => window.location.reload()
            });
        }
    } catch (error) {
        console.error('Erro ao carregar pets:', error);
        // Esconde loading e mostra erro
        hideLoading();
        EmptyState.render(container, 'error', {
            message: 'Não foi possível carregar os pets. Verifique sua conexão e tente novamente.',
            ctaText: 'Tentar novamente',
            ctaAction: () => window.location.reload()
        });
    }
}

// Inicializa quando DOM estiver pronto
document.addEventListener('DOMContentLoaded', init);


// ========================================
// ESTILOS INJETADOS
// ========================================

const style = document.createElement('style');
style.innerHTML = `
/* Layout responsivo */
body {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

/* Container de corações */
#heartContainer {
    position: fixed;
    left: 0; top: 0; 
    width: 100vw; 
    height: 100vh;
    pointer-events: none;
    z-index: 1000;
}

/* Animação de corações voadores */
.flying-heart {
    position: absolute;
    font-size: 2.2rem;
    color: currentColor;
    animation: flyHeart 0.9s forwards;
    pointer-events: none;
    z-index: 1001;
}

.flying-heart::before {
    content: "❤";
    display: block;
    filter: drop-shadow(0 0 4px rgba(255,255,255,0.25));
}

@keyframes flyHeart {
    0% {
        opacity: 1;
        transform: translateY(0) scale(1) rotate(-10deg);
    }
    60% {
        opacity: 1;
        transform: translateY(-80px) scale(1.2) rotate(10deg);
    }
    100% {
        opacity: 0;
        transform: translateY(-180px) scale(0.8) rotate(-10deg);
    }
}

/* Badges de informação */
.pet-badges {
    display: flex;
    gap: 8px;
    margin-top: 12px;
    flex-wrap: wrap;
}

.badge {
    padding: 4px 12px;
    background: rgba(255,255,255,0.2);
    border-radius: 12px;
    font-size: 0.85rem;
    font-weight: 500;
}

/* Footer fixo */
footer {
    margin-top: auto !important;
    position: relative;
    z-index: 2;
}
`;
document.head.appendChild(style);
