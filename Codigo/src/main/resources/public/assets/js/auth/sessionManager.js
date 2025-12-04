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
     * Realiza login do usuário via API REST
     * 
     * POST /api/auth/login com CPF + senha
     * A resposta contém token e dados do usuário do backend
     * 
     * @param {string} cpf - CPF do adotante (pode ser "admin")
     * @param {string} senha - Senha em texto plano (será enviada via POST)
     * @returns {Promise<{success: boolean, message: string, user?: object}>}
     */
    async function login(cpf, senha) {
        // Validações de formato
        if (!validateCpfFormat(cpf) && cpf !== 'admin') {
            return {
                success: false,
                message: 'CPF inválido. Digite apenas os 11 números ou "admin".'
            };
        }

        if (!validatePassword(senha)) {
            return {
                success: false,
                message: 'Senha deve ter no mínimo 3 caracteres.'
            };
        }

        const cleanCpf = cpf === 'admin' ? 'admin' : cpf.replace(/\D/g, '');

        try {
            // ✨ CHAMADA REAL À API REST ✨
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    cpf: cleanCpf,
                    senha: senha
                })
            });

            const data = await response.json();

            if (!response.ok || !data.success) {
                return {
                    success: false,
                    message: data.error || 'CPF ou senha incorretos. Verifique suas credenciais.'
                };
            }

            // Sucesso: salva sessão com dados do backend
            const user = data.user;
            const sessionToken = data.token;
            
            // Salva usuário no localStorage
            localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(user));
            localStorage.setItem(STORAGE_KEY_SESSION, sessionToken);

            return {
                success: true,
                message: `✅ Bem-vindo, ${user.nome}!`,
                user: user
            };

        } catch (error) {
            console.error('Erro ao conectar com API:', error);
            return {
                success: false,
                message: 'Erro ao conectar ao servidor. Verifique se o backend está rodando em localhost:8080.'
            };
        }

        // ============ FALLBACK: Se API não responder, usar mock apenas para testes ============
        // (Comentado - remover se quiser desabilitar fallback)
        /*
        const mockUsers = {
            'admin': { cpf: 'admin', nome: 'Administrador', email: 'admin@mpet.com', role: 'ADMIN', senha: 'admin' },
            '12345678901': { cpf: '12345678901', nome: 'João Silva', role: 'ADOTANTE', senha: '123' },
            '11111111111': { cpf: '11111111111', nome: 'Pedro Voluntário', role: 'VOLUNTARIO', senha: '123' }
        };
        const mockUser = mockUsers[cleanCpf];
        if (mockUser && mockUser.senha === senha) {
            localStorage.setItem(STORAGE_KEY_USER, JSON.stringify(mockUser));
            return { success: true, message: `Bem-vindo, ${mockUser.nome}!`, user: mockUser };
        }
        return { success: false, message: 'CPF ou senha incorretos' };
        */
    }

    // ANTIGO CÓDIGO REMOVIDO
    /*
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
            'ADMIN': 'index.html', // Admin vai para home (TODO: criar dashboard)
            'ADOTANTE': 'match.html', // MATCH é o principal! (já está em /pages/)
            'VOLUNTARIO': 'index.html' // Voluntário vai para home (TODO: criar dashboard)
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

    /**
     * Salva URL para retornar após login
     * @param {string} url 
     */
    function setSavedUrl(url) {
        localStorage.setItem('mpet_saved_url', url);
    }

    /**
     * Obtém URL salva para retornar após login
     * @returns {string}
     */
    function getSavedUrl() {
        return localStorage.getItem('mpet_saved_url') || 'index.html';
    }

    /**
     * Limpa URL salva
     */
    function clearSavedUrl() {
        localStorage.removeItem('mpet_saved_url');
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
        setSavedUrl,
        getSavedUrl,
        clearSavedUrl,
        updateUser,
        validateCpfFormat
    };
})();

// Exporta para uso global
window.SessionManager = SessionManager;

