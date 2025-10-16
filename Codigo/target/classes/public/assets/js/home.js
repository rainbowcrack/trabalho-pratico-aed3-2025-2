// Preenche carrossel "Pets em destaque" e ativa animações de scroll
const featured = [
  { nome: 'Luna', tag: 'Dócil', img: 'https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=400&q=80' },
  { nome: 'Thor', tag: 'Brincalhão', img: 'https://images.unsplash.com/photo-1558788353-f76d92427f16?auto=format&fit=crop&w=400&q=80' },
  { nome: 'Mel', tag: 'Vacinada', img: 'https://images.unsplash.com/photo-1518715308788-3005759c41c8?auto=format&fit=crop&w=400&q=80' },
  { nome: 'Nina', tag: 'Calma', img: 'https://images.unsplash.com/photo-1587300003388-59208cc962cb?auto=format&fit=crop&w=400&q=80' },
  { nome: 'Bob', tag: 'Companheiro', img: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=400&q=80' },
  { nome: 'Mimi', tag: 'Carinhosa', img: 'https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=400&q=80' }
];

function fillFeatured() {
  const scroller = document.getElementById('featuredPets');
  if (!scroller) return;
  featured.forEach(p => {
    const el = document.createElement('div');
    el.className = 'card pet-mini reveal';
    el.innerHTML = `
      <img src="${p.img}" alt="${p.nome}">
      <div class="name">${p.nome}</div>
      <span class="badge">${p.tag}</span>
    `;
    scroller.appendChild(el);
  });
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
