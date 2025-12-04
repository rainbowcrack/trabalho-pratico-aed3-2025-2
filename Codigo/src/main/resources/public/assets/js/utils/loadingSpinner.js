/**
 * loadingSpinner.js - Componente de Loading Reutilizável
 * 
 * Cria e gerencia indicadores de carregamento padronizados.
 */

/**
 * Configurações de loading predefinidas
 */
const LOADING_CONFIGS = {
  default: {
    text: 'Carregando...',
    spinner: '⏳',
    size: 'medium'
  },
  
  pets: {
    text: 'Buscando pets...',
    spinner: '🐾',
    size: 'medium'
  },
  
  matches: {
    text: 'Carregando seus matches...',
    spinner: '💕',
    size: 'medium'
  },
  
  chats: {
    text: 'Carregando conversas...',
    spinner: '💬',
    size: 'medium'
  },
  
  login: {
    text: 'Entrando...',
    spinner: '🔐',
    size: 'small'
  },
  
  save: {
    text: 'Salvando...',
    spinner: '💾',
    size: 'small'
  },
  
  sending: {
    text: 'Enviando...',
    spinner: '📤',
    size: 'small'
  }
};

/**
 * Cria HTML de loading spinner
 * @param {string} type - Tipo do loading (default, pets, matches, etc.)
 * @param {Object} customConfig - Configuração personalizada opcional
 * @returns {string} HTML do loading spinner
 */
function createLoadingHTML(type = 'default', customConfig = {}) {
  const config = { ...LOADING_CONFIGS[type], ...customConfig };
  
  const sizeClass = `loading-spinner-${config.size}`;
  
  return `
    <div class="loading-spinner ${sizeClass}" data-type="${type}">
      <div class="loading-content">
        <div class="loading-icon spinning">${config.spinner}</div>
        <div class="loading-text">${config.text}</div>
      </div>
    </div>
  `;
}

/**
 * Mostra loading spinner em um container
 * @param {HTMLElement} container - Container onde exibir
 * @param {string} type - Tipo do loading
 * @param {Object} customConfig - Configuração personalizada opcional
 * @returns {Function} Função para esconder o loading
 */
function showLoading(container, type = 'default', customConfig = {}) {
  if (!container) {
    console.warn('Container não encontrado para loading spinner');
    return () => {};
  }

  // Salva conteúdo original
  const originalContent = container.innerHTML;
  
  // Renderiza loading
  container.innerHTML = createLoadingHTML(type, customConfig);
  
  // Retorna função para esconder loading
  return function hideLoading() {
    container.innerHTML = originalContent;
  };
}

/**
 * Mostra loading em botão
 * @param {HTMLButtonElement} button - Botão para mostrar loading
 * @param {string} type - Tipo do loading
 * @returns {Function} Função para restaurar botão
 */
function showButtonLoading(button, type = 'save') {
  if (!button) {
    console.warn('Botão não encontrado para loading');
    return () => {};
  }

  const config = LOADING_CONFIGS[type] || LOADING_CONFIGS.default;
  
  // Salva estado original
  const originalText = button.textContent;
  const originalDisabled = button.disabled;
  
  // Aplica loading
  button.innerHTML = `<span class="spinning" style="margin-right: 8px;">${config.spinner}</span> ${config.text}`;
  button.disabled = true;
  button.style.opacity = '0.8';
  
  // Retorna função para restaurar
  return function hideButtonLoading() {
    button.textContent = originalText;
    button.disabled = originalDisabled;
    button.style.opacity = '';
  };
}

/**
 * Loading overlay para página inteira
 * @param {string} type - Tipo do loading
 * @param {Object} customConfig - Configuração personalizada opcional
 * @returns {Function} Função para esconder overlay
 */
function showPageLoading(type = 'default', customConfig = {}) {
  // Remove overlay anterior se existir
  const existingOverlay = document.getElementById('pageLoadingOverlay');
  if (existingOverlay) {
    existingOverlay.remove();
  }

  // Cria overlay
  const overlay = document.createElement('div');
  overlay.id = 'pageLoadingOverlay';
  overlay.className = 'loading-overlay';
  overlay.innerHTML = createLoadingHTML(type, customConfig);
  
  document.body.appendChild(overlay);
  
  // Anima entrada
  setTimeout(() => overlay.classList.add('show'), 10);
  
  // Retorna função para esconder
  return function hidePageLoading() {
    overlay.classList.remove('show');
    setTimeout(() => {
      if (overlay.parentNode) {
        overlay.remove();
      }
    }, 300);
  };
}

// CSS para loading spinners (será injetado dinamicamente)
const LOADING_CSS = `
  .loading-spinner {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem;
    text-align: center;
  }

  .loading-spinner-small {
    padding: 1rem;
  }

  .loading-spinner-medium {
    padding: 2rem;
  }

  .loading-spinner-large {
    padding: 3rem;
  }

  .loading-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
  }

  .loading-icon {
    font-size: 2rem;
    opacity: 0.8;
  }

  .loading-spinner-small .loading-icon {
    font-size: 1.5rem;
  }

  .loading-spinner-large .loading-icon {
    font-size: 3rem;
  }

  .spinning {
    animation: spin 2s linear infinite;
  }

  .loading-text {
    color: var(--text-secondary, #64748b);
    font-size: 0.9rem;
    font-weight: 500;
  }

  .loading-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(4px);
    z-index: 9999;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .loading-overlay.show {
    opacity: 1;
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  /* Loading em botões */
  button .spinning {
    display: inline-block;
    animation: spin 1s linear infinite;
  }

  /* Responsivo */
  @media (max-width: 768px) {
    .loading-spinner {
      padding: 1.5rem;
    }
    
    .loading-icon {
      font-size: 1.8rem;
    }
    
    .loading-text {
      font-size: 0.85rem;
    }
  }
`;

/**
 * Injeta CSS de loading spinners no documento
 */
function injectLoadingCSS() {
  if (document.getElementById('loading-spinner-styles')) return; // Já injetado

  const style = document.createElement('style');
  style.id = 'loading-spinner-styles';
  style.textContent = LOADING_CSS;
  document.head.appendChild(style);
}

// Auto-inicialização
document.addEventListener('DOMContentLoaded', () => {
  injectLoadingCSS();
});

// Exporta para uso global
window.LoadingSpinner = {
  show: showLoading,
  showButton: showButtonLoading,
  showPage: showPageLoading,
  createHTML: createLoadingHTML,
  configs: LOADING_CONFIGS
};