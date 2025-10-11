const pets = [
    {
        nome: "Luna",
        detalhes: "Fêmea • 2 anos • SRD",
        descricao: "Luna é uma cachorrinha carinhosa, adora brincar e está pronta para encontrar um novo lar cheio de amor!",
        imagem: "https://images.unsplash.com/photo-1518717758536-85ae29035b6d?auto=format&fit=crop&w=400&q=80"
    },
    {
        nome: "Thor",
        detalhes: "Macho • 3 anos • Labrador",
        descricao: "Thor é brincalhão, adora correr e se dá bem com crianças.",
        imagem: "https://images.unsplash.com/photo-1558788353-f76d92427f16?auto=format&fit=crop&w=400&q=80"
    },
    {
        nome: "Mel",
        detalhes: "Fêmea • 1 ano • Poodle",
        descricao: "Mel é dócil, gosta de colo e está vacinada.",
        imagem: "https://images.unsplash.com/photo-1518715308788-3005759c41c8?auto=format&fit=crop&w=400&q=80"
    }
];

let currentIndex = 0;

// Cria corações voando pela tela toda
function createHearts() {
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
        heartContainer.appendChild(heart);
        setTimeout(() => heart.remove(), 900);
    }
}

function renderCard(index) {
    const container = document.getElementById('appContainer');
    container.innerHTML = '';
    if (index >= pets.length) {
        container.innerHTML = `<div class="pet-card"><p style="text-align:center;">Não há mais pets para mostrar!</p></div>`;
        return;
    }
    const pet = pets[index];
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
        createHearts();
        setTimeout(() => {
            currentIndex++;
            renderCard(currentIndex);
        }, 500);
    };

    // Recusar: anima para esquerda
    card.querySelector('.dislike').onclick = () => {
        card.style.transition = "transform 0.5s cubic-bezier(.4,0,.2,1), opacity 0.5s";
        card.style.transform = "translateX(-400px) rotate(-15deg)";
        card.style.opacity = "0";
        setTimeout(() => {
            currentIndex++;
            renderCard(currentIndex);
        }, 500);
    };
}

document.addEventListener('DOMContentLoaded', () => renderCard(currentIndex));

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
    color: #ff3b3b;
    animation: flyHeart 0.9s forwards;
    pointer-events: none;
    z-index: 1001;
}
.flying-heart::before {
    content: "❤";
    display: block;
    filter: drop-shadow(0 0 4px #ff8c42);
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