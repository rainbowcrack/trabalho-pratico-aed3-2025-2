window.pets = [
    // Dogs 🐶
    { tipo: 'dog', nome: "Luna", detalhes: "Fêmea • 2 anos • SRD", descricao: "Carinhosa, adora brincar e está pronta para um lar!", imagem: "https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=800&q=60" },
    { tipo: 'dog', nome: "Thor", detalhes: "Macho • 3 anos • Labrador", descricao: "Brincalhão, ama correr e se dá bem com crianças.", imagem: "https://images.unsplash.com/photo-1558788353-f76d92427f16?auto=format&fit=crop&w=800&q=60" },
    { tipo: 'dog', nome: "Mel", detalhes: "Fêmea • 1 ano • Poodle", descricao: "Dócil, gosta de colo e está vacinada.", imagem: "https://images.unsplash.com/photo-1507146426996-ef05306b995a?auto=format&fit=crop&w=800&q=60" },
    { tipo: 'dog', nome: "Bob", detalhes: "Macho • 4 anos • SRD", descricao: "Companheiro e muito leal.", imagem: "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=800&q=60" },
    { tipo: 'dog', nome: "Nina", detalhes: "Fêmea • 2 anos • Border Collie", descricao: "Energia alta, ótima para atividades.", imagem: "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=800&q=60" },
    // Cats 🐱
    { tipo: 'cat', nome: "Mimi", detalhes: "Fêmea • 2 anos • SRD", descricao: "Carinhosa, adora janelas de sol.", imagem: "https://images.unsplash.com/photo-1511044568932-338cba0ad803?auto=format&fit=crop&w=800&q=60" },
    { tipo: 'cat', nome: "Zeca", detalhes: "Macho • 3 anos • Siamês", descricao: "Elegante e curioso.", imagem: "https://images.unsplash.com/photo-1543852786-1cf6624b9987?auto=format&fit=crop&w=800&q=60" },
    { tipo: 'cat', nome: "Lola", detalhes: "Fêmea • 1 ano • Persa", descricao: "Calma e dorminhoca.", imagem: "https://images.unsplash.com/photo-1511045999812-7a32f6081c71?auto=format&fit=crop&w=800&q=60" },
    { tipo: 'cat', nome: "Fred", detalhes: "Macho • 5 anos • SRD", descricao: "Independente e limpinho.", imagem: "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=800&q=60" }
];

window.currentIndex = 0;

// Cria corações voando pela tela toda com cor temática
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
        // Posição horizontal aleatória na tela
        heart.style.left = `${Math.random() * 100}%`;
        // Posição vertical inicial aleatória (de baixo para cima)
        heart.style.bottom = `${Math.random() * 40 + 10}px`;
        heart.style.animationDelay = `${Math.random() * 0.3}s`;
    heart.dataset.color = color;
    heart.style.color = color;
        heartContainer.appendChild(heart);
        setTimeout(() => heart.remove(), 900);
    }
}

window.renderCard = function renderCard(index) {
    const container = document.getElementById('appContainer');
    container.innerHTML = '';
    if (index >= pets.length) {
        container.innerHTML = `<div class="pet-card"><p style="text-align:center;">Não há mais pets para mostrar!</p></div>`;
        return;
    }
    const pet = pets[index];
    // Ajusta tema por tipo
    const theme = pet.tipo === 'dog' ? 'dog' : 'cat';
    document.body.setAttribute('data-theme', theme);
    const card = document.createElement('div');
    card.className = 'pet-card';
    card.innerHTML = `
        <img class="pet-photo" src="${pet.imagem}" alt="Foto do pet">
        <div class="pet-info">
            <span class="pet-name">${pet.nome}</span>
            <span class="pet-details">${pet.detalhes}</span>
            <p class="pet-description">${pet.descricao}</p>
        </div>
        <div class="actions">
            <button class="action-btn dislike" title="Não gostei">✖</button>
            <button class="action-btn like" title="Gostei">❤</button>
        </div>
    `;
    container.appendChild(card);

    // Curtir: anima para direita + corações voando
    card.querySelector('.like').onclick = () => {
        card.style.transition = "transform 0.5s cubic-bezier(.4,0,.2,1), opacity 0.5s";
        card.style.transform = "translateX(400px) rotate(15deg)";
        card.style.opacity = "0";
        // cor do coração segue o tema atual (cat usa brand1/2 padrão avermelhado, dog usa verde)
        const color = theme === 'dog' ? '#22c55e' : '#ff3b3b';
        createHearts(color);
        setTimeout(() => {
            window.currentIndex++;
            window.renderCard(window.currentIndex);
        }, 500);
    };

    // Recusar: anima para esquerda
    card.querySelector('.dislike').onclick = () => {
        card.style.transition = "transform 0.5s cubic-bezier(.4,0,.2,1), opacity 0.5s";
        card.style.transform = "translateX(-400px) rotate(-15deg)";
        card.style.opacity = "0";
        setTimeout(() => {
            window.currentIndex++;
            window.renderCard(window.currentIndex);
        }, 500);
    };
}

document.addEventListener('DOMContentLoaded', () => window.renderCard(window.currentIndex));

// CSS para animação dos corações e garantir footer fixo
const style = document.createElement('style');
style.innerHTML = `
body {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}
#heartContainer {
    position: fixed;
    left: 0; top: 0; width: 100vw; height: 100vh;
    pointer-events: none;
    z-index: 1000;
}
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
footer {
    margin-top: auto !important;
    position: relative;
    z-index: 2;
}
`;
document.head.appendChild(style);