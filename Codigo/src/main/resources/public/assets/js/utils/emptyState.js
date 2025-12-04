/**
 * emptyState.js - Componente de Empty States Reutilizável
 * 
 * Cria componentes de estado vazio padronizados com CTA para melhor UX.
 */

/**
 * Configurações de empty states predefinidas
 */
const EMPTY_STATE_CONFIGS = {
  featuredPets: {
    icon: '🐾',
    title: 'Nenhum pet em destaque',
    message: 'No momento não há pets disponíveis para destaque.',
    ctaText: 'Explore todos os pets',
    ctaAction: () => Router.navigate('/match.html'),
    ctaClass: 'btn-primary'
  },
  
  matches: {
    icon: '💕',
    title: 'Nenhum match ainda',
    message: 'Você ainda não tem matches. Que tal conhecer alguns pets disponíveis para adoção?',
    ctaText: 'Encontrar pets',
    ctaAction: () => Router.navigate('/match.html'),
    ctaClass: 'btn-primary'
  },
  
  chats: {
    icon: '💬', 
    title: 'Nenhuma conversa ativa',
    message: 'Você ainda não tem conversas. Demonstre interesse em um pet para iniciar um chat!',
    ctaText: 'Explorar pets',
    ctaAction: () => Router.navigate('/match.html'),
    ctaClass: 'btn-primary'
  },
  
  animals: {
    icon: '🔍',
    title: 'Nenhum pet encontrado',
    message: 'No momento não há pets disponíveis com os filtros selecionados. Tente ajustar os filtros ou volte mais tarde.',
    ctaText: 'Limpar filtros',
    ctaAction: () => window.location.reload(),
    ctaClass: 'btn-secondary'
  },

  error: {
    icon: '⚠️',
    title: 'Algo deu errado',
    message: 'Houve um problema ao carregar os dados. Verifique sua conexão e tente novamente.',
    ctaText: 'Tentar novamente',
    ctaAction: () => window.location.reload(),
    ctaClass: 'btn-primary'
  }
};

/**
 * Cria um empty state HTML baseado na configuração
 * @param {string} type - Tipo do empty state (featuredPets, matches, chats, animals, error)
 * @param {Object} customConfig - Configuração personalizada opcional
 * @returns {string} HTML do empty state
 */
function createEmptyStateHTML(type, customConfig = {}) {
  const config = { ...EMPTY_STATE_CONFIGS[type], ...customConfig };
  
  if (!config) {
    console.warn(`Empty state type "${type}" não encontrado`);
    return '';
  }

  return `
    <div class="empty-state" data-type="${type}">
      <div class="empty-state-content">
        <div class="empty-state-icon">${config.icon}</div>
        <h3 class="empty-state-title">${config.title}</h3>
        <p class="empty-state-message">${config.message}</p>
        ${config.ctaText ? `
          <button class="btn ${config.ctaClass} empty-state-cta" data-type="${type}">
            ${config.ctaText}
          </button>
        ` : ''}
      </div>
    </div>
  `;
}

/**
 * Renderiza um empty state em um container
 * @param {HTMLElement} container - Container onde renderizar
 * @param {string} type - Tipo do empty state
 * @param {Object} customConfig - Configuração personalizada opcional
 */
function renderEmptyState(container, type, customConfig = {}) {
  if (!container) {
    console.warn('Container não encontrado para empty state');
    return;
  }

  container.innerHTML = createEmptyStateHTML(type, customConfig);
  
  // Adiciona listener para o CTA
  const ctaButton = container.querySelector('.empty-state-cta');
  if (ctaButton) {
    ctaButton.addEventListener('click', () => {
      const config = { ...EMPTY_STATE_CONFIGS[type], ...customConfig };
      if (config.ctaAction) {
        config.ctaAction();
      }
    });
  }
}

/**
 * Inicializa listeners globais para empty states
 */
function initEmptyStateListeners() {
  document.addEventListener('click', (event) => {
    if (event.target.classList.contains('empty-state-cta')) {
      const type = event.target.getAttribute('data-type');
      const config = EMPTY_STATE_CONFIGS[type];
      
      if (config && config.ctaAction) {
        event.preventDefault();
        config.ctaAction();
      }
    }
  });
}

// CSS para empty states (será injetado dinamicamente)
const EMPTY_STATE_CSS = `
  .empty-state {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 200px;
    padding: 2rem;
    text-align: center;
    border: 2px dashed var(--border-color, #e2e8f0);
    border-radius: var(--border-radius, 12px);
    background: var(--bg-secondary, #f8fafc);
    margin: 1rem 0;
  }

  .empty-state-content {
    max-width: 300px;
  }

  .empty-state-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
    opacity: 0.7;
  }

  .empty-state-title {
    font-size: 1.25rem;
    font-weight: 600;
    color: var(--text-primary, #1e293b);
    margin: 0 0 0.5rem;
  }

  .empty-state-message {
    color: var(--text-secondary, #64748b);
    margin: 0 0 1.5rem;
    line-height: 1.5;
  }

  .empty-state-cta {
    margin-top: 0.5rem;
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: var(--border-radius, 8px);
    font-size: 0.9rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .btn-primary {
    background: var(--primary-solid, #ff6f61);
    color: white;
  }

  .btn-primary:hover {
    background: var(--primary-dark, #e55a4a);
    transform: translateY(-1px);
  }

  .btn-secondary {
    background: var(--bg-secondary, #f1f5f9);
    color: var(--text-primary, #1e293b);
    border: 1px solid var(--border-color, #e2e8f0);
  }

  .btn-secondary:hover {
    background: var(--bg-tertiary, #e2e8f0);
  }

  /* Responsive */
  @media (max-width: 768px) {
    .empty-state {
      min-height: 150px;
      padding: 1.5rem;
    }

    .empty-state-icon {
      font-size: 2.5rem;
    }

    .empty-state-title {
      font-size: 1.1rem;
    }

    .empty-state-message {
      font-size: 0.9rem;
    }
  }
`;

/**
 * Injeta CSS de empty states no documento
 */
function injectEmptyStateCSS() {
  if (document.getElementById('empty-state-styles')) return; // Já injetado

  const style = document.createElement('style');
  style.id = 'empty-state-styles';
  style.textContent = EMPTY_STATE_CSS;
  document.head.appendChild(style);
}

// Auto-inicialização
document.addEventListener('DOMContentLoaded', () => {
  injectEmptyStateCSS();
  initEmptyStateListeners();
});

// Exporta para uso global
window.EmptyState = {
  render: renderEmptyState,
  createHTML: createEmptyStateHTML,
  configs: EMPTY_STATE_CONFIGS
};