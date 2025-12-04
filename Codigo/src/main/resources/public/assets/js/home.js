/**
 * home.js - Landing Page (Refatorado)
 * 
 * Carrega pets em destaque do PetService e renderiza dinamicamente.
 * Usa PetAdapter para transformar dados do Backend em View Models.
 */

/**
 * Preenche carrossel de pets em destaque
 */
async function fillFeatured() {
  const scroller = document.getElementById('featuredPets');
  if (!scroller) return;

  // Mostra loading
  const hideLoading = LoadingSpinner.show(scroller, 'pets');

  try {
    // Busca pets em destaque do serviço
    const result = await PetService.getFeaturedPets(6);
    
    if (result.success && result.data.length > 0) {
      // Adapta para mini-cards
      const featuredPets = PetAdapter.adaptMiniList(result.data);
      
      // Esconde loading e renderiza cards
      hideLoading();
      
      // Renderiza cada card
      featuredPets.forEach(pet => {
        const el = document.createElement('div');
        el.className = 'card pet-mini reveal';
        el.innerHTML = `
          <img src="${pet.imagem}" alt="${pet.nome}">
          <div class="name">${pet.nome}</div>
          <span class="badge">${pet.tag}</span>
        `;
        scroller.appendChild(el);
      });
    } else {
      // Esconde loading e mostra empty state
      hideLoading();
      EmptyState.render(scroller, 'featuredPets');
    }
  } catch (error) {
    console.error('Erro ao carregar pets em destaque:', error);
    // Esconde loading e mostra erro
    hideLoading();
    EmptyState.render(scroller, 'error', {
      message: 'Não foi possível carregar os pets em destaque. Verifique sua conexão.'
    });
  }
}

// Scroll reveal simples
function handleReveal() {
  const els = document.querySelectorAll('.reveal');
  const vh = window.innerHeight * 0.9;
  els.forEach(el => {
    const rect = el.getBoundingClientRect();
    if (rect.top < vh) el.classList.add('in-view');
  });
}

window.addEventListener('scroll', handleReveal);
window.addEventListener('resize', handleReveal);

document.addEventListener('DOMContentLoaded', () => {
  fillFeatured();
  handleReveal();
  // Inicia checagem de saúde da API na home
  initHealthWidget();
});

/**
 * Health widget: checa /api/health periodicamente e atualiza indicador na home
 */
async function initHealthWidget() {
  const el = document.getElementById('healthStatus');
  if (!el) return;

  async function check() {
    const start = performance.now();
    try {
      const res = await fetch('/api/health', { cache: 'no-store' });
      const ms = Math.round(performance.now() - start);
      if (res.ok) {
        el.textContent = `API: online (${ms}ms)`;
        el.style.color = '#22c55e'; // verde
      } else {
        el.textContent = `API: erro ${res.status}`;
        el.style.color = '#ef4444'; // vermelho
      }
    } catch (e) {
      el.textContent = 'API: offline';
      el.style.color = '#ef4444';
    }
  }

  // Primeira checagem imediata e agenda a cada 15s
  check();
  setInterval(check, 15000);
}
