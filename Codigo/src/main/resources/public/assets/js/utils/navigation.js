/**
 * navigation.js
 * 
 * Utilitários para navegação dinâmica baseada em autenticação.
 * Renderiza navbar responsiva com menus diferentes por tipo de usuário.
 */

/**
 * Renderiza navbar dinamicamente baseado em autenticação
 * @param {string} currentPageName - Nome da página atual (ex: 'index', 'match', 'meus-matches')
 */
function renderNavbar(currentPageName = null) {
    const headerNav = document.querySelector('header nav ul');
    if (!headerNav) return; // Se não houver nav na página, skip

    const isAuthenticated = SessionManager.isAuthenticated();
    const user = isAuthenticated ? SessionManager.getCurrentUser() : null;

    // Limpa navegação atual
    headerNav.innerHTML = '';

    // Links sempre visíveis
    const links = [
        { text: 'início', href: 'index.html', show: true },
        { text: 'sobre', href: 'sobre.html', show: true },
        { text: 'matches', href: 'match.html', show: isAuthenticated },
        { text: 'meus-matches', href: 'meus-matches.html', show: isAuthenticated },
        { text: 'meus-chats', href: 'meus-chats.html', show: isAuthenticated },
    ];

    links.forEach(link => {
        if (!link.show) return;

        const li = document.createElement('li');
        const a = document.createElement('a');
        a.href = link.href;
        a.textContent = link.text;

        // Marca link ativo
        if (currentPageName && link.href.includes(currentPageName)) {
            a.style.color = 'var(--accent)';
            a.style.fontWeight = 'bold';
        }

        li.appendChild(a);
        headerNav.appendChild(li);
    });

    // Botão de logout (se autenticado)
    if (isAuthenticated) {
        const li = document.createElement('li');
        const btn = document.createElement('button');
        btn.style.cssText = `
            background: none;
            border: none;
            color: #fff;
            font-weight: bold;
            cursor: pointer;
            padding: 0;
            font-size: 1rem;
            transition: color 0.2s;
        `;
        btn.textContent = 'logout';
        btn.onmouseover = () => btn.style.color = 'var(--accent)';
        btn.onmouseout = () => btn.style.color = '#fff';
        btn.onclick = (e) => {
            e.preventDefault();
            SessionManager.logout();
            window.location.href = 'index.html';
        };
        li.appendChild(btn);
        headerNav.appendChild(li);

        // Exibe nome do usuário
        const userSpan = document.createElement('li');
        userSpan.style.cssText = `
            margin-left: auto;
            color: var(--muted);
            font-size: 0.9rem;
        `;
        userSpan.textContent = `👤 ${user.nome || user.cpf}`;
        headerNav.appendChild(userSpan);
    }
}

/**
 * Protege rota: redireciona para login se não autenticado
 * @param {string} requiredRole - Role requerida (null = qualquer autenticado, 'ADOTANTE' = só adotantes, etc)
 * @returns {boolean} - true se passou na proteção
 */
function protectRoute(requiredRole = null) {
    if (!SessionManager.isAuthenticated()) {
        // Salva URL para retornar após login
        const currentUrl = window.location.pathname.split('/').pop() || 'index.html';
        SessionManager.setSavedUrl(currentUrl);
        window.location.href = 'login.html';
        return false;
    }

    if (requiredRole) {
        const user = SessionManager.getCurrentUser();
        if (user.role !== requiredRole) {
            showAlert(`Acesso restrito. Você precisa ser ${requiredRole}.`, 'error');
            window.location.href = 'index.html';
            return false;
        }
    }

    return true;
}

/**
 * Exibe alerta genérico (compatível com diferentes estilos de alerta)
 */
function showAlert(message, type = 'info') {
    // Tenta encontrar elemento alertBox (como em login.html)
    let alertBox = document.getElementById('alertBox');
    if (alertBox) {
        alertBox.textContent = message;
        alertBox.className = `alert alert-${type} show`;
        setTimeout(() => alertBox.classList.remove('show'), 5000);
        return;
    }

    // Alternativa: usa toast
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

/**
 * Carrega ONGs disponíveis (mock para agora)
 */
async function loadONGs() {
    // FUTURO: Buscar via API /api/ongs
    // MOCK: Retorna ONGs de teste
    return [
        { id: 1, nome: 'Patinhas Felizes', cnpj: '12.345.678/0001-00', endereco: 'São Paulo, SP' },
        { id: 2, nome: 'Resgate Animal RJ', cnpj: '23.456.789/0001-11', endereco: 'Rio de Janeiro, RJ' },
        { id: 3, nome: 'Patas e Amor', cnpj: '34.567.890/0001-22', endereco: 'Belo Horizonte, MG' },
    ];
}

/**
 * Formata CPF com máscara
 */
function formatCPF(cpf) {
    if (!cpf) return '';
    const clean = cpf.replace(/\D/g, '');
    return clean.replace(/(\d{3})(\d)/, '$1.$2')
                .replace(/(\d{3})(\d)/, '$1.$2')
                .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

/**
 * Limpa formatação de CPF
 */
function cleanCPF(cpf) {
    return cpf.replace(/\D/g, '');
}

/**
 * Formata data de DD/MM/YYYY para YYYY-MM-DD
 */
function formatDateInput(date) {
    if (!date) return '';
    const [day, month, year] = date.split('/');
    return `${year}-${month}-${day}`;
}

/**
 * Formata data de YYYY-MM-DD para DD/MM/YYYY
 */
function formatDateDisplay(date) {
    if (!date) return '';
    const [year, month, day] = date.split('-');
    return `${day}/${month}/${year}`;
}

/**
 * Validar CNPJ
 */
function validateCNPJ(cnpj) {
    const clean = cnpj.replace(/\D/g, '');
    if (clean.length !== 14) return false;
    // Implementar lógica de validação se necessário
    return true;
}

/**
 * Inicializa navbar ao carregar página
 * Chama esta função no DOMContentLoaded de cada página
 */
document.addEventListener('DOMContentLoaded', function() {
    // Se há header nav, renderiza navbar
    if (document.querySelector('header nav')) {
        const pageName = window.location.pathname.split('/').pop()?.replace('.html', '');
        renderNavbar(pageName);
    }
});
