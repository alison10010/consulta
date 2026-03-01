package com.consulta.rest;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.consulta.model.Endereco;
import com.consulta.model.Especialidade;
import com.consulta.model.Usuario;
import com.consulta.model.UsuarioEspecialidade;
import com.consulta.record.UsuarioDTO;
import com.consulta.repository.EspecialidadeRepository;
import com.consulta.repository.UsuarioEspecialidadeRepository;
import com.consulta.repository.UsuarioRepository;
import com.consulta.service.FileStorageService;
import com.consulta.util.EmailConfirmaCadastro;
import com.consulta.util.Ferramentas;


@RestController
@RequestMapping("/api/") 
@CrossOrigin(origins = "*")
public class ResponseController {

	@Autowired UsuarioRepository usuarioRepository;
	
	@Autowired private PasswordEncoder passwordEncoder;
	
	@Autowired private FileStorageService fileService;
	
	@Autowired private EmailConfirmaCadastro emailConfirmaCadastro;
	
	// #============================================================
	// ====== SPECIALIDADE/USUARIO
    @Autowired private UsuarioEspecialidadeRepository usuarioEspecialidadeRepository;

    // ADD UMA NOVA ESPECIALIDADE
    
    @Autowired private EspecialidadeRepository especialidadeRepository; 
    
    private Long especialidadeSelecionadaId;
    private UsuarioEspecialidade usuarioEspecialidade;
	
	@PostMapping(value = "usuario/add", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
	public ResponseEntity<?> criarUsuario(
            @RequestPart("dto") UsuarioDTO dto,
            @RequestPart("diploma") MultipartFile diploma,
            @RequestPart("carteira") MultipartFile carteira) {
        try {
            // 1. Instanciar e mapear dados do Usuário
            Usuario usuario = new Usuario();
            usuario.setNome(dto.nome().toUpperCase());
            usuario.setUsername(dto.username());
            usuario.setEmail(dto.email());
            usuario.setCpf(dto.cpf());
            usuario.setNascimento(dto.nascimento());
            usuario.setSexo(dto.sexo());
            usuario.setTelefone(dto.telefone());
            usuario.setAcesso(dto.acesso());
            usuario.setTermoAceite(dto.termoAceite());
            usuario.setConselho(dto.conselho());
            usuario.setRegistro(dto.registro());
            
            usuario.setPossuiWpp(false);
            
            // Criptografar senha
            usuario.setPassword(passwordEncoder.encode(dto.password()));

            // 2. Mapear Endereço
            Endereco end = new Endereco();
            end.setCep(dto.cep());
            end.setEndereco(dto.endereco());
            end.setNumero(dto.numero());
            end.setBairro(dto.bairro());
            end.setCidade(dto.cidade());
            end.setEstado(dto.uf());
            usuario.setEndereco(end);

            // 3. Primeiro Save: Gera o ID necessário para a pasta de arquivos
            usuario = usuarioRepository.save(usuario);
                        
            Especialidade esp = especialidadeRepository.findById(dto.especialidade()).orElse(null);

            UsuarioEspecialidade ue = new UsuarioEspecialidade();
            ue.setUsuario(usuario);
            ue.setEspecialidade(esp);
            ue.setConselho(dto.conselho());
            ue.setRegistro(dto.registro());
            ue.setValorComGuia(BigDecimal.ZERO);
            ue.setValor(BigDecimal.ZERO);

            // status padrão
            ue.setStatus("PENDENTE_DOCS");

            // ====== SALVAR NO DISCO + GUARDAR PATH ======

        	String espNomeBase = normalizaParaArquivo(esp.getEspecialidade());
            ue.setNomeArquivo(espNomeBase); 

            String espNome = normalizaParaArquivo(esp.getEspecialidade());

            ue.setNomeArquivo(espNome);
            
            // DIPLOMA: Usando o Service
            String nomeDiploma= salvarDocumentoJSF(usuario.getId(), espNome + "_diploma", diploma);
            ue.setPathDiplomaEspecialidade(nomeDiploma); // Salvamos apenas o nome do arquivo no banco
            

            // CARTEIRA: Usando o Service
            String nomeCarteira = salvarDocumentoJSF(usuario.getId(), espNome + "_carteira", carteira);
            ue.setPathCarteiraEspecialidade(nomeCarteira); // Salvamos apenas o nome do arquivo no banco
            
            // Ajusta status conforme anexos
            boolean temDiploma = ue.getPathDiplomaEspecialidade() != null;
            boolean temCarteira = ue.getPathCarteiraEspecialidade() != null;
            ue.setStatus((temDiploma && temCarteira) ? "EM_ANALISE" : "PENDENTE_DOCS");
        
            
            usuarioEspecialidadeRepository.save(ue);
            
            // Gerar HASH único para o usuário
            String hash = Ferramentas.geraHash(
                usuario.getCpf() + LocalDateTime.now()
            );
            usuario.setHash(hash);	

            // 6. Segundo Save: Atualiza o registro com os caminhos dos documentos
            usuarioRepository.save(usuario);
            
            // envia Mensagem Assincrona sem esperar pela confirmação do envio do e-mail
	        new Thread(() -> {
	        	 emailConfirmaCadastro.enviaMensagem(dto.email(), hash);
	        }).start();	        
           
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao gravar arquivos no disco: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao processar cadastro: " + e.getMessage());
        }
    }

	// Método que você sugeriu para detectar o SO
	private Path resolvePastaCompartilhada() {
	    String os = System.getProperty("os.name").toLowerCase();
	    if (os.contains("win")) {
	        return Paths.get("C:\\documentos");
	    }
	    return Paths.get("/mnt/documentos");
	}
	
	public String salvarDocumentoJSF(Long usuarioId, String nomePadrao, MultipartFile arquivo) throws IOException {

	    String contentType = arquivo.getContentType();
	    if (contentType == null) contentType = "";

	    if (!contentType.equals("application/pdf") && !contentType.startsWith("image/")) {
	        throw new IOException("Tipo de arquivo não permitido: " + contentType);
	    }

	    Path pastaUsuario = resolvePastaCompartilhada()
	            .resolve("colaboradores")
	            .resolve(usuarioId.toString());

	    Files.createDirectories(pastaUsuario);

	    // Remove arquivos antigos com o mesmo "nomePadrao" (base), independente da extensão
	    try (Stream<Path> arquivos = Files.list(pastaUsuario)) {
	        arquivos
	            .filter(path -> path.getFileName().toString().startsWith(nomePadrao))
	            .forEach(path -> {
	                try {
	                    Files.deleteIfExists(path);
	                } catch (IOException e) {
	                    System.err.println("Não foi possível deletar arquivo antigo: " + path + " | " + e.getMessage());
	                }
	            });
	    }

	    // Extrai extensão do nome original
	    String originalName = arquivo.getOriginalFilename();
	    String extensao = "";

	    if (originalName != null && originalName.contains(".")) {
	        extensao = originalName.substring(originalName.lastIndexOf(".")); // inclui o ponto
	    } else {
	        // fallback se vier sem extensão (raro)
	        extensao = contentType.equals("application/pdf") ? ".pdf" : ".bin";
	    }

	    String nomeFinal = nomePadrao + extensao;
	    Path destino = pastaUsuario.resolve(nomeFinal);

	    try (InputStream input = arquivo.getInputStream()) {
	        Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
	    }

	    return nomeFinal;
	}
	

	@PostMapping(value = "usuario/paciente/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> criarPaciente(@RequestPart("dto") UsuarioDTO usuarioDTO) {
	    try {
	        Usuario usuario = new Usuario();
	        usuario.setNome(usuarioDTO.nome().toUpperCase());
	        usuario.setEmail(usuarioDTO.email());
	        usuario.setUsername(usuarioDTO.username());
	        usuario.setAcesso("NORMAL");
	   
	        usuario.setCpf(usuarioDTO.cpf());         
	        usuario.setSexo(usuarioDTO.sexo());        
	        usuario.setNascimento(usuarioDTO.nascimento()); 

	        usuario.setPassword(passwordEncoder.encode(usuarioDTO.password()));
	        
	        // Gerar HASH único para o usuário
            String hash = Ferramentas.geraHash(
                usuario.getCpf() + LocalDateTime.now()
            );
            usuario.setHash(hash);

	        usuarioRepository.save(usuario);
	        
	        // envia Mensagem Assincrona sem esperar pela confirmação do envio do e-mail
	        new Thread(() -> {
	        	 emailConfirmaCadastro.enviaMensagem(usuarioDTO.email(), hash);
	        }).start();

	        return ResponseEntity.status(HttpStatus.CREATED).build();

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao criar usuário: " + e.getMessage());
	    }
	}		
	
	// ## ============== VERIFICA SE JA EXISTE	
	@PostMapping("usuario/existente")
	public ResponseEntity<Boolean> getUsuarioExistente(@RequestParam String username) {
	    boolean existente = usuarioRepository.findByUsername(username.trim()) != null;
	    return ResponseEntity.ok(existente);
	}
	
	
	@GetMapping("especialidades")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(especialidadeRepository.listarDTO());
    }

	
    
    private String normalizaParaArquivo(String s) {
        if (s == null) return "sem_nome";
        return s.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_\\-]", ""); // tira acentos/símbolos
    }

}

