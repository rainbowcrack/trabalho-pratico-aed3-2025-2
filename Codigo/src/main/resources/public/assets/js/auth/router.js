/**
 * router.js
 * 
 * Sistema de roteamento e controle de acesso baseado em papéis.
 * Gerencia navegação entre páginas públicas e protegidas.
 * 
 * ESTRUTURA DO SITE:
 * 
 * PÚBLICO:
 * - /index.html (Home/Apresentação)
 * - /pages/login.html
 * - /pages/sobre.html
 * 
 * PROTEGIDO - ADMIN:
 * - /pages/admin/dashboard.html (Painel principal)
 * - /pages/admin/animais.html (Gerenciar animais)
 * - /pages/admin/ongs.html (Gerenciar ONGs)
 * - /pages/admin/adotantes.html (Gerenciar adotantes)
 * - /pages/admin/voluntarios.html (Gerenciar voluntários)
 * - /pages/admin/adocoes.html (Gerenciar adoções)
 * - /pages/admin/sistema.html (Backup/Restore/Vacuum)
 * 
 * PROTEGIDO - ADOTANTE:
 * - /pages/adotante/dashboard.html (Painel principal)
 * - /pages/adotante/perfil.html (Ver/editar dados)
 * - /pages/adotante/match.html (Sistema de match - PRINCIPAL)
 * - /pages/adotante/interesses.html (Meus interesses)
 * - /pages/adotante/chats.html (Minhas conversas)
 * 
 * PROTEGIDO - VOLUNTÁRIO:
 * - /pages/voluntario/dashboard.html (Painel principal)
 * - /pages/voluntario/perfil.html (Ver/editar dados)
 * - /pages/voluntario/animais.html (Animais da minha ONG)
 * - /pages/voluntario/interesses.html (Interessados nos animais)
 * - /pages/voluntario/chats.html (Chats com adotantes)
 * - /pages/voluntario/adocoes.html (Confirmar adoções)
 */

const Router = (function() {
    
    // Mapeamento de rotas por papel
    const ROUTES = {
        PUBLIC: [
            '/index.html',
            '/pages/login.html',
            '/pages/sobre.html'
        ],
        ADMIN: [
            '/pages/admin/dashboard.html',
            '/pages/admin/animais.html',
            '/pages/admin/ongs.html',
            '/pages/admin/adotantes.html',
            '/pages/admin/voluntarios.html',
            '/pages/admin/adocoes.html',
            '/pages/admin/sistema.html'
        ],
        ADOTANTE: [
            '/pages/adotante/dashboard.html',
            '/pages/adotante/perfil.html',
            '/pages/adotante/match.html',
            '/pages/adotante/interesses.html',
            '/pages/adotante/chats.html'
        ],
        VOLUNTARIO: [
            '/pages/voluntario/dashboard.html',
            '/pages/voluntario/perfil.html',
            '/pages/voluntario/animais.html',
            '/pages/voluntario/interesses.html',
            '/pages/voluntario/chats.html',
            '/pages/voluntario/adocoes.html'
        ]
    };

    // Página padrão após login por papel
    const DEFAULT_PAGES = {
        ADMIN: '/pages/admin/dashboard.html',
        ADOTANTE: '/pages/adotante/match.html', // Match é o principal!
        VOLUNTARIO: '/pages/voluntario/dashboard.html'
    };

    /**
     * Verifica se usuário tem permissão para acessar a rota
     * @param {string} path - Caminho da página
     * @returns {boolean}
     */
    function canAccess(path) {
        // Rotas públicas são sempre acessíveis
        if (ROUTES.PUBLIC.includes(path)) {
            return true;
        }

        // Rotas protegidas exigem autenticação
        const user = SessionManager.getCurrentUser();
        if (!user || !user.role) {
            return false;
        }

        // Verifica se a rota pertence ao papel do usuário
        const userRoutes = ROUTES[user.role];
        return userRoutes && userRoutes.includes(path);
    }

    /**
     * Redireciona para página adequada baseada no papel
     * @param {string} role - ADMIN | ADOTANTE | VOLUNTARIO
     */
    function navigateToDefault(role) {
        const defaultPage = DEFAULT_PAGES[role] || '/index.html';
        window.location.href = defaultPage;
    }

    /**
     * Protege a página atual (chamar no DOMContentLoaded)
     * Redireciona se usuário não tiver permissão
     */
    function protectPage() {
        const currentPath = window.location.pathname;
        
        // Normaliza o path (remove início se tiver /pages)
        let normalizedPath = currentPath;
        if (!normalizedPath.startsWith('/pages/') && !normalizedPath.endsWith('.html')) {
            // Caso esteja em /public/pages/login.html, extrai /pages/login.html
            const match = currentPath.match(/(\/pages\/.*\.html)$/);
            if (match) {
                normalizedPath = match[1];
            }
        }

        // Se é rota pública, libera
        if (ROUTES.PUBLIC.some(route => normalizedPath.endsWith(route))) {
            return true;
        }

        // Se não está autenticado, redireciona para login
        const user = SessionManager.getCurrentUser();
        if (!user) {
            sessionStorage.setItem('mpet_return_url', normalizedPath);
            window.location.href = '/pages/login.html';
            return false;
        }

        // Verifica permissão
        const hasAccess = canAccess(normalizedPath);
        if (!hasAccess) {
            alert('Acesso negado! Você não tem permissão para esta página.');
            navigateToDefault(user.role);
            return false;
        }

        return true;
    }

    /**
     * Navega para uma página (com validação de acesso)
     * @param {string} path - Caminho da página
     */
    function navigate(path) {
        if (canAccess(path)) {
            window.location.href = path;
        } else {
            alert('Você não tem permissão para acessar esta página.');
        }
    }

    /**
     * Retorna menu de navegação baseado no papel
     * @param {string} role - ADMIN | ADOTANTE | VOLUNTARIO
     * @returns {Array<{label: string, href: string, icon: string}>}
     */
    function getMenuForRole(role) {
        const menus = {
            ADMIN: [
                { label: 'Dashboard', href: '/pages/admin/dashboard.html', icon: '📊' },
                { label: 'Animais', href: '/pages/admin/animais.html', icon: '🐾' },
                { label: 'ONGs', href: '/pages/admin/ongs.html', icon: '🏢' },
                { label: 'Adotantes', href: '/pages/admin/adotantes.html', icon: '👥' },
                { label: 'Voluntários', href: '/pages/admin/voluntarios.html', icon: '🙋' },
                { label: 'Adoções', href: '/pages/admin/adocoes.html', icon: '❤️' },
                { label: 'Sistema', href: '/pages/admin/sistema.html', icon: '⚙️' }
            ],
            ADOTANTE: [
                { label: 'Match', href: '/pages/adotante/match.html', icon: '💖' },
                { label: 'Meus Interesses', href: '/pages/adotante/interesses.html', icon: '⭐' },
                { label: 'Conversas', href: '/pages/adotante/chats.html', icon: '💬' },
                { label: 'Meu Perfil', href: '/pages/adotante/perfil.html', icon: '👤' }
            ],
            VOLUNTARIO: [
                { label: 'Dashboard', href: '/pages/voluntario/dashboard.html', icon: '📊' },
                { label: 'Animais da ONG', href: '/pages/voluntario/animais.html', icon: '🐾' },
                { label: 'Interessados', href: '/pages/voluntario/interesses.html', icon: '👀' },
                { label: 'Conversas', href: '/pages/voluntario/chats.html', icon: '💬' },
                { label: 'Adoções', href: '/pages/voluntario/adocoes.html', icon: '✅' },
                { label: 'Meu Perfil', href: '/pages/voluntario/perfil.html', icon: '👤' }
            ]
        };

        return menus[role] || [];
    }

    /**
     * Renderiza menu de navegação no header
     * Chama automaticamente ao carregar página protegida
     */
    function renderNavMenu() {
        const user = SessionManager.getCurrentUser();
        if (!user) return;

        const menuItems = getMenuForRole(user.role);
        const nav = document.querySelector('nav ul');
        
        if (!nav) return;

        // Limpa menu existente
        nav.innerHTML = '';

        // Adiciona itens do menu
        menuItems.forEach(item => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = item.href;
            a.innerHTML = `${item.icon} ${item.label}`;
            
            // Marca como ativo se for a página atual
            if (window.location.pathname.endsWith(item.href)) {
                a.style.fontWeight = 'bold';
                a.style.textDecoration = 'underline';
            }
            
            li.appendChild(a);
            nav.appendChild(li);
        });

        // Adiciona botão de logout
        const logoutLi = document.createElement('li');
        const logoutBtn = document.createElement('a');
        logoutBtn.href = '#';
        logoutBtn.innerHTML = '🚪 Sair';
        logoutBtn.style.color = 'var(--danger, #ef4444)';
        logoutBtn.onclick = (e) => {
            e.preventDefault();
            if (confirm('Deseja realmente sair?')) {
                SessionManager.logout();
                window.location.href = '/index.html';
            }
        };
        logoutLi.appendChild(logoutBtn);
        nav.appendChild(logoutLi);
    }

    /**
     * Inicialização automática do router
     * Chame no DOMContentLoaded de páginas protegidas
     */
    function init() {
        const isProtected = protectPage();
        if (isProtected && SessionManager.isAuthenticated()) {
            renderNavMenu();
        }
    }

    // API pública
    return {
        canAccess,
        navigate,
        navigateToDefault,
        protectPage,
        getMenuForRole,
        renderNavMenu,
        init,
        ROUTES,
        DEFAULT_PAGES
    };
})();

// Exporta para uso global
window.Router = Router;
