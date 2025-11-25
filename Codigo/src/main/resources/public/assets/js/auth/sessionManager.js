/**
 * sessionManager.js
 * 
 * Gerenciador de sessão e autenticação do usuário.
 * Controla login/logout e persistência no localStorage.
 * 
 * IMPORTANTE: Este é um stub para preparação da integração futura.
 * Quando o backend estiver conectado, as validações reais serão feitas
 * via API REST contra adotantes.dat com senha RSA-2048.
 */

const SessionManager = (function() {
    // Chaves do localStorage
    const STORAGE_KEY_USER = 'mpet_current_user';
    const STORAGE_KEY_SESSION = 'mpet_session_token';

    /**
     * Valida formato de CPF (apenas numérico, 11 dígitos)
     * @param {string} cpf 
     * @returns {boolean}
     */
    function validateCpfFormat(cpf) {
        if (!cpf || typeof cpf !== 'string') return false;
        const cleanCpf = cpf.replace(/\D/g, '');
        return cleanCpf.length === 11;
    }

    /**
     * Valida senha (mínimo 3 caracteres para mock)
     * @param {string} senha 
     * @returns {boolean}
     */
    function validatePassword(senha) {
        return senha && senha.length >= 3;
    }

    /**
     * Realiza login do usuário
     * 
     * STUB: Por enquanto apenas valida formato e salva no localStorage.
     * FUTURO: Fazer POST /api/auth/login com CPF + senha criptografada.
     * 
     * @param {string} cpf - CPF do adotante (apenas números)
     * @param {string} senha - Senha (será criptografada com RSA no backend)
     * @returns {Promise<{success: boolean, message: string, user?: object}>}
     */
    async function login(cpf, senha) {
        // Simula delay de rede (200-500ms)
        await new Promise(resolve => setTimeout(resolve, 200 + Math.random() * 300));

        // Validações de formato
        if (!validateCpfFormat(cpf)) {
            return {
                success: false,
                message: 'CPF inválido. Digite apenas os 11 números.'
            };
        }

        if (!validatePassword(senha)) {
            return {
                success: false,
                message: 'Senha deve ter no mínimo 3 caracteres.'
            };
        }

        const cleanCpf = cpf.replace(/\D/g, '');

        // MOCK: Base de usuários de teste
        // Quando integrar com backend, remover e fazer POST /api/auth/login
        const mockUsers = {
            // ADMIN (credenciais especiais)
            'admin': {
                cpf: 'admin',
                nome: 'Administrador',
                email: 'admin@mpet.com',
                role: 'ADMIN',
                senha: 'admin'
            },
            // ADOTANTES (CPF como chave)
            '12345678901': {
                cpf: '12345678901',
                idKey: 1,
                nome: 'João Silva',
                email: 'joao@teste.com',
                telefone: '(11) 98765-4321',
                role: 'ADOTANTE',
                senha: '123'
            },
            '98765432100': {
                cpf: '98765432100',
                idKey: 2,
                nome: 'Maria Santos',
                email: 'maria@teste.com',
                telefone: '(21) 99876-5432',
                role: 'ADOTANTE',
                senha: '123'
            },
            // VOLUNTÁRIOS (CPF como chave)
            '11111111111': {
                cpf: '11111111111',
                idKey: 1,
                nome: 'Pedro Voluntário',
                email: 'pedro@ong.com',
                telefone: '(11) 91234-5678',
                role: 'VOLUNTARIO',
                idOng: 1,
                cargo: 'COORDENADOR',
                senha: '123'
            },
            '22222222222': {
                cpf: '22222222222',
                idKey: 2,
                nome: 'Ana Voluntária',
                email: 'ana@ong.com',
                telefone: '(21) 91234-5678',
                role: 'VOLUNTARIO',
                idOng: 1,
                cargo: 'CUIDADOR',
                senha: '123'
            }
        };

        // Busca usuário (admin usa "admin" como chave, outros usam CPF)
        const userKey = cleanCpf === 'admin' ? 'admin' : cleanCpf;
        const mockUser = mockUsers[userKey];

        if (!mockUser) {
            return {
                success: false,
                message: 'CPF não cadastrado. Teste: 12345678901 (Adotante) ou 11111111111 (Voluntário) ou admin/admin'
            };
        }

        // Valida senha
        if (mockUser.senha !== senha) {
            return {
                success: false,
                message: 'Senha incorreta.'
            };
        }

        // Cria objeto de sessão (remove senha antes de salvar)
        const { senha: _, ...userWithoutPassword } = mockUser;
        const user = {
            ...userWithoutPassword,
            loginAt: new Date().toISOString()
        };

        // Gera token mock (futuro: JWT do backend)
        const sessionToken = `mock_token_${cleanCpf}_${Date.now()}`;

        // Persiste no localStorage
        localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
        localStorage.setItem(STORAGE_KEY_SESSION, sessionToken);

        return {
            success: true,
            message: 'Login realizado com sucesso!',
            user: user
        };
    }

    /**
     * Realiza logout do usuário
     */
    function logout() {
        localStorage.removeItem(STORAGE_KEY_USER);
        localStorage.removeItem(STORAGE_KEY_SESSION);
    }

    /**
     * Retorna o usuário logado ou null
     * @returns {object|null}
     */
    function getCurrentUser() {
        try {
            const userJson = localStorage.getItem(STORAGE_KEY_USER);
            return userJson ? JSON.parse(userJson) : null;
        } catch (e) {
            console.error('Erro ao recuperar usuário:', e);
            return null;
        }
    }

    /**
     * Verifica se há usuário autenticado
     * @returns {boolean}
     */
    function isAuthenticated() {
        return getCurrentUser() !== null;
    }

    /**
     * Retorna CPF do usuário logado
     * @returns {string|null}
     */
    function getCurrentCpf() {
        const user = getCurrentUser();
        return user ? user.cpf : null;
    }

    /**
     * Redireciona para login se não autenticado
     * 
     * @param {string} returnUrl - URL de retorno após login (opcional)
     * @returns {boolean} true se autenticado, false se redirecionou
     */
    function requireAuth(returnUrl = null) {
        if (isAuthenticated()) {
            return true;
        }

        // Salva URL de retorno
        if (returnUrl) {
            sessionStorage.setItem('mpet_return_url', returnUrl);
        } else {
            sessionStorage.setItem('mpet_return_url', window.location.pathname);
        }

        // Redireciona para login (path relativo)
        const currentPath = window.location.pathname;
        if (currentPath.includes('/pages/')) {
            // Já está em /pages/, usa path relativo
            window.location.href = 'login.html';
        } else {
            // Está na raiz, vai para pages/
            window.location.href = 'pages/login.html';
        }
        return false;
    }

    /**
     * Mostra modal de login (alternativa ao redirect)
     * Útil para páginas que preferem modal ao invés de redirect
     */
    function showLoginModal() {
        // Dispara evento customizado que o modal pode escutar
        const event = new CustomEvent('mpet:requireLogin', {
            detail: {
                returnUrl: window.location.pathname
            }
        });
        document.dispatchEvent(event);
    }

    /**
     * Retorna para URL salva após login (com lógica de papel)
     */
    function returnToSavedUrl() {
        const returnUrl = sessionStorage.getItem('mpet_return_url');
        sessionStorage.removeItem('mpet_return_url');
        
        const user = getCurrentUser();
        if (!user) {
            window.location.href = 'login.html';
            return;
        }

        // Se tinha URL salva e é válida, vai pra ela
        if (returnUrl && returnUrl !== 'login.html' && !returnUrl.includes('login.html')) {
            window.location.href = returnUrl;
            return;
        }

        // Senão, redireciona para página padrão do papel
        const defaultPages = {
            'ADMIN': 'admin/dashboard.html',
            'ADOTANTE': 'match.html', // MATCH é o principal! (já está em /pages/)
            'VOLUNTARIO': 'voluntario/dashboard.html'
        };

        const defaultPage = defaultPages[user.role] || '../index.html';
        window.location.href = defaultPage;
    }

    /**
     * Retorna o papel (role) do usuário
     * @returns {string|null} 'ADMIN' | 'ADOTANTE' | 'VOLUNTARIO' | null
     */
    function getUserRole() {
        const user = getCurrentUser();
        return user ? user.role : null;
    }

    /**
     * Atualiza dados do usuário no localStorage
     * @param {object} userData 
     */
    function updateUser(userData) {
        const currentUser = getCurrentUser();
        if (currentUser) {
            const updatedUser = { ...currentUser, ...userData };
            localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(updatedUser));
        }
    }

    // API pública
    return {
        login,
        logout,
        getCurrentUser,
        getCurrentCpf,
        getUserRole,
        isAuthenticated,
        requireAuth,
        showLoginModal,
        returnToSavedUrl,
        updateUser,
        validateCpfFormat
    };
})();

// Exporta para uso global
window.SessionManager = SessionManager;
