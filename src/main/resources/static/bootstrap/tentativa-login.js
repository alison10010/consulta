function iniciarContadorBloqueio() {
        // --- Configurações ---
        const BLOQUEIO_SEGUNDOS = 5 * 60; // Duração do bloqueio: 5 minutos
        const STORAGE_KEY = 'lockCountdown';
        const contadorEl = document.getElementById('contador');
        
        // Verifica se o elemento contador existe na página (só existirá se param.error for 'locked')
        if (!contadorEl) {
            return;
        }

        // 1. Tenta recuperar o tempo de expiração do armazenamento local
        let expiraStr = localStorage.getItem(STORAGE_KEY);
        let expira = expiraStr ? parseInt(expiraStr, 10) : 0;
        const agora = Date.now();

        // 2. Se não houver tempo salvo ou se o tempo salvo já expirou
        if (!expira || agora > expira) {
            // Cria um novo tempo de bloqueio de 5 minutos a partir de agora
            expira = agora + BLOQUEIO_SEGUNDOS * 1000;
            localStorage.setItem(STORAGE_KEY, expira.toString());
        }

        // 3. Função principal de atualização do contador
        function atualizarContador() {
            const restanteMs = expira - Date.now();
            
            if (restanteMs <= 0) {
                // Fim do contador: Remove a chave e exibe mensagem final
                contadorEl.innerHTML = '<span style="color:green;">Você já pode tentar novamente.</span>';
                localStorage.removeItem(STORAGE_KEY);
                clearInterval(intervalo);
                
                // Os campos não são mais desabilitados/reabilitados, pois o formulário fica sempre ativo.
                
                return;
            }
            
            // Cálculo do tempo restante
            const s = Math.floor(restanteMs / 1000);
            const min = Math.floor(s / 60);
            const seg = s % 60;
            
            // CORREÇÃO: Usando concatenação de strings (+) em vez de Template Literals
            contadorEl.textContent =
                "Tente novamente em " + min + ":" + seg.toString().padStart(2, '0') + "";
        }
        
        // REMOVIDO: Seção 4 (Garantia de desativação do formulário).
        // O formulário permanece ativo para que outro usuário possa tentar o login.
        
        // 4. Inicia o loop
        atualizarContador();
        const intervalo = setInterval(atualizarContador, 1000);
    }

    // Garante que o script só inicie quando a página estiver totalmente carregada
    window.addEventListener('load', function() {
        // Verifica se o erro "locked" está presente
        if (new URLSearchParams(window.location.search).get('error') === 'locked') {
            iniciarContadorBloqueio();
        }
    });