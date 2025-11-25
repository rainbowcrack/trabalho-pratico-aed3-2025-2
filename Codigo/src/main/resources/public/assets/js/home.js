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

  try {
    // Busca pets em destaque do serviço
    const result = await PetService.getFeaturedPets(6);
    
    if (result.success && result.data.length > 0) {
      // Adapta para mini-cards
      const featuredPets = PetAdapter.adaptMiniList(result.data);
      
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
      // Fallback: mensagem se não houver pets
      scroller.innerHTML = '<p style="padding: 20px; text-align: center;">Nenhum pet em destaque no momento.</p>';
    }
  } catch (error) {
    console.error('Erro ao carregar pets em destaque:', error);
    scroller.innerHTML = '<p style="padding: 20px; text-align: center;">Erro ao carregar pets.</p>';
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
});
