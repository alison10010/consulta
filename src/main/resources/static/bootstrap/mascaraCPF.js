//Função para aplicar a máscara de CPF na class CPF
function formatCPF(cpf) {
    cpf = cpf.replace(/\D/g, ''); // Remove todos os caracteres não numéricos
    cpf = cpf.replace(/(\d{3})(\d)/, '$1.$2'); // Adiciona o primeiro ponto
    cpf = cpf.replace(/(\d{3})(\d)/, '$1.$2'); // Adiciona o segundo ponto
    cpf = cpf.replace(/(\d{3})(\d{1,2})$/, '$1-$2'); // Adiciona o traço
    return cpf;
}

var cpfElements = document.querySelectorAll(".cpf");
cpfElements.forEach(function (element) {
    element.innerText = formatCPF(element.innerText);
});



function formatarNumero(numero) {
    // Converte o número para uma string formatada com separador de milhares
    var numeroFormatado = numero.toLocaleString('pt-BR');
    
    return numeroFormatado;
}

// Chamada da função passando o número como parâmetro
var numeroElements = document.querySelectorAll(".numero");
numeroElements.forEach(function (element) {
    // Obtém o número do conteúdo do elemento
    var numero = parseInt(element.innerText);

    // Chama a função formatarNumero para obter o número formatado
    var numeroFormatado = formatarNumero(numero);

    // Atualiza o elemento com o número formatado
    element.innerText = numeroFormatado;
});
