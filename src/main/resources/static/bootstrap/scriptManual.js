function somenteLetras(input) {
  input.value = input.value.replace(/[^a-zA-ZÀ-ÿçÇ\s/]/g, '');
}

function somenteNumeros(input) {
  // Remove todos os caracteres que não são números
  input.value = input.value.replace(/\D/g, '');
}