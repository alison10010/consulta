// Usaremos a função ready do jQuery para garantir que o DOM e o jQuery estejam prontos
$(document).ready(function() {
    getGeolocation();
});


function getGeolocation() {
    // Para elementos sem tags JSF (como <span>), getElementById deve funcionar.
    const geoStatusEl = document.getElementById('geoStatus'); 
    
    // 🚨 SELETORES DE JQUERY PARA PRIMEACES: Encontra o elemento DOM nativo
    const acessarBtn = $('[id$="acessar"]').get(0);
    const latitudeInput = $('[id$="latitude"]').get(0);
    const longitudeInput = $('[id$="longitude"]').get(0);

    // Verificação de segurança
    if (!geoStatusEl || !acessarBtn || !latitudeInput || !longitudeInput) {
        if (geoStatusEl) {
             geoStatusEl.style.color = 'red';
             geoStatusEl.textContent = 'ERRO INTERNO: Falha ao localizar componentes PrimeFaces.';
        }
        console.error('ERRO: Falha ao encontrar elementos PrimeFaces (acessar, latitude, longitude).');
        return;
    }
    
    // =======================================================
    // 1. VERIFICAR SUPORTE À GEOLOCALIZAÇÃO
    // =======================================================
    if (!("geolocation" in navigator)) {
        // A) NAVEGADOR NÃO SUPORTA
        // Habilita e remove a classe PrimeFaces de desabilitado
        $(acessarBtn).prop('disabled', false).removeClass('ui-state-disabled'); 
        
        geoStatusEl.style.color = 'blue';
        geoStatusEl.textContent = '';
        latitudeInput.value = '';
        longitudeInput.value = '';
        return;
    }

    // =======================================================
    // B) NAVEGADOR SUPORTA -> TORNAR OBRIGATÓRIO
    // =======================================================
    
    geoStatusEl.style.color = 'orange';
    geoStatusEl.textContent = 'Permissão...';

    navigator.geolocation.getCurrentPosition(
        // Sucesso (Usuário Aceitou)
        function (position) {
			// 🎯 Arredonda para 4 casas decimais (~11 metros de precisão)
		    const lat = position.coords.latitude.toFixed(4); 
		    const lon = position.coords.longitude.toFixed(4);

            latitudeInput.value = lat;
            longitudeInput.value = lon;
            
            // 🎯 HABILITAÇÃO COMPLETA: Seta a prop e remove a classe
            $(acessarBtn).prop('disabled', false).removeClass('ui-state-disabled'); 
            
            geoStatusEl.textContent = ''; // Limpa a mensagem de status
        },
        // Erro (Usuário Negou ou Outro Erro)
        function (error) {
            geoStatusEl.style.color = 'red';
            let mensagemErro = 'A geolocalização é obrigatória para fazer o login.';
            
            if (error.code === error.PERMISSION_DENIED) {
                 mensagemErro = 'A geolocalização é obrigatória. Por favor, permita o acesso e tente novamente.';
            } else if (error.code === error.TIMEOUT) {
                 mensagemErro = 'Não foi possível obter sua localização. Tente novamente.';
            }
            
            geoStatusEl.textContent = mensagemErro;
            
            // 🎯 BLOQUEIO COMPLETO: Seta a prop e adiciona a classe
            $(acessarBtn).prop('disabled', true).addClass('ui-state-disabled'); 
        },
        // Opções de requisição
        {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 0
        }
    );
}