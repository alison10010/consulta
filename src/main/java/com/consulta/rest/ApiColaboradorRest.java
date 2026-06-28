package com.consulta.rest;

import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.consulta.controller.HomeController;
import com.consulta.model.Horario;
import com.consulta.model.Usuario;
import com.consulta.record.ApiColaboradoresDTO;
import com.consulta.repository.HorarioRepository;
import com.consulta.repository.UsuarioEspecialidadeRepository;
import com.consulta.repository.UsuarioRepository;
import com.consulta.service.FileStorageService;

import lombok.Getter;
import lombok.Setter;

@RestController
@RequestMapping("/api/")
@Getter @Setter
@CrossOrigin(origins = "*")
public class ApiColaboradorRest implements Serializable {
	
	@Autowired UsuarioRepository usuarioRepository;
	
	@Autowired UsuarioEspecialidadeRepository usuarioEspecialidadeRepository;	
	
	@Autowired private HorarioRepository horarioRepository;
	
	@Autowired private FileStorageService fileService;
	
	@Autowired HomeController homeController;
	
	private static final long serialVersionUID = 1L;
	

	@GetMapping("/colaboradores")
	public ResponseEntity<List<ApiColaboradoresDTO>> listColaboradores() {

	    var lista = usuarioRepository.findByAcessoComEndereco("COLABORADOR")
	        .stream()
	        .map(u -> {
	            var e = u.getEndereco();

	            var especialidades = usuarioEspecialidadeRepository
	                .findByUsuarioIdAndStatusIgnoreCase(u.getId(), "APROVADO")
	                .stream()
	                .map(ue -> Map.<String, Object>of(
	                    "nome", ue.getEspecialidade().getEspecialidade(),
	                    "valor", ue.getValor(),
	                    "valorComGuia", ue.getValorComGuia(),
	                    "registro", ue.getRegistro(),
	                    "conselho", ue.getConselho()
	                ))
	                .sorted(Comparator.comparing(m -> (String) m.get("nome")))
	                .toList();

	            String foto = "/consulta/api/usuario/" + u.getId() + "/perfil";
	            
	            return new ApiColaboradoresDTO(
	                u.getId(), u.getNome(), u.getUsername(), u.getEmail(), u.getEspecialidade(),
	                u.getTelefone(), u.getCelular(), u.isPossuiWpp(), u.getAcesso(),

	                foto,
	                
	                e != null ? e.getCep() : null,
	                e != null ? e.getEndereco() : null,
	                e != null ? e.getNumero() : null,
	                e != null ? e.getBairro() : null,
	                e != null ? e.getCidade() : null,
	                e != null ? e.getEstado() : null,
	                e != null ? e.getComplemento() : null,
	                e != null ? e.getCoordenadas() : null,

	                especialidades
	            );
	        })
	        .toList();

	    return ResponseEntity.ok(lista);
	}
	
	@GetMapping("/usuarios/especialidades")
	public ResponseEntity<List<String>> especialidadesColaboradores() {
	    return ResponseEntity.ok(
	        usuarioRepository.listarEspecialidadesUsadas()
	    );
	}
	
	@GetMapping("/colaboradores/{id}/horarios-disponiveis")
	public ResponseEntity<List<Map<String, Object>>> horariosDisponiveis(@PathVariable Long id) {

	    LocalDate hoje = LocalDate.now(ZoneId.of("America/Rio_Branco"));
	    LocalDate fim  = hoje.plusDays(7);

	    var lista = horarioRepository.listarDisponiveisPeriodo(id, hoje, fim)
	    		 .stream()
		    	    .collect(Collectors.groupingBy(Horario::getData))
		    	    .entrySet()
		    	    .stream()
		    	    .map(e -> {
		    	        LocalDate data = e.getKey();
		    	        List<Horario> horarios = e.getValue();

		    	        String vaga = horarios.get(0).getVaga(); // mesma janela no dia

		    	        return Map.<String, Object>of(
		    	            "data", data.toString(),
		    	            "vagas", horarios.size(),
		    	            "janela", vaga
		    	        );
		    	    })
		    	    .sorted(Comparator.comparing(m -> (String)m.get("data")))
		    	    .toList(); 

	    return ResponseEntity.ok(lista);
	}
	
	// ============ FOTO DE PERFIL    
    @GetMapping("/usuario/{id}/perfil")
    public ResponseEntity<byte[]> fotoUsuario(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            byte[] bytes = fileService.buscarBytesArquivo(
                    usuario.getId(),
                    usuario.getPerfil()
            );

            if (bytes == null || bytes.length == 0) {
                InputStream is = getClass().getResourceAsStream("/static/img/perfil.png");
                bytes = is.readAllBytes();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.IMAGE_PNG);
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(bytes);

        } catch (Exception e) {
            try {
                InputStream is = getClass().getResourceAsStream("/static/img/perfil.png");
                byte[] bytes = is.readAllBytes();

                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                        .body(bytes);
            } catch (Exception ex) {
                return ResponseEntity.notFound().build();
            }
        }
    }

	@GetMapping("/usuario/logado")
	public Map<String, Object> logado() {
	    Map<String, Object> resp = new HashMap<>();
	    resp.put("logado", homeController.logado());
	    return resp;
	}

}
