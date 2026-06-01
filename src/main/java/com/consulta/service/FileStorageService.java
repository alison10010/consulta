package com.consulta.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.primefaces.model.file.UploadedFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private Path resolvePastaCompartilhada() {
        String os = System.getProperty("os.name").toLowerCase();
        // BASE: C:\documentos (win) | /mnt/documentos (linux)
        return os.contains("win") ? Paths.get("C:\\documentos") : Paths.get("/mnt/documentos");
    }

    public String salvarDocumentoJSF(Long usuarioId, String nomePadrao, UploadedFile arquivo, String perfil) throws IOException {
        if (arquivo == null || arquivo.getFileName() == null || arquivo.getFileName().isEmpty()) {
            return null;
        }
        
        String contentType = arquivo.getContentType();
        if (!contentType.equals("application/pdf") && !contentType.startsWith("image/")) {
            throw new IOException("Tipo de arquivo não permitido.");
        }
        
        String diretorio;
        
        if(perfil.equals("COLABORADOR")) {
	    	diretorio = "colaboradores";
	    }else {
	    	diretorio = "pacientes";
	    }

        Path pastaUsuario = resolvePastaCompartilhada()
                .resolve(diretorio)
                .resolve(usuarioId.toString());

        Files.createDirectories(pastaUsuario);

        // --- NOVIDADE: Limpeza de arquivos antigos com o mesmo nome base ---
        // Isso remove 'diploma.jpg' se você estiver subindo um 'diploma.pdf'
        try (Stream<Path> arquivos = Files.list(pastaUsuario)) {
            arquivos.filter(path -> path.getFileName().toString().startsWith(nomePadrao))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Não foi possível deletar arquivo antigo: " + path);
                        }
                    });
        }

        // Extrai a nova extensão
        String originalName = arquivo.getFileName();
        String extensao = originalName.substring(originalName.lastIndexOf("."));
        
        String nomeFinal = nomePadrao + extensao;
        Path destino = pastaUsuario.resolve(nomeFinal);

        // Salva o novo arquivo
        try (InputStream input = arquivo.getInputStream()) {
            Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return nomeFinal;
    }
    
    public byte[] buscarBytesArquivo(Long usuarioId, String nomeArquivo) throws IOException {
        Path caminhoArquivo = resolvePastaCompartilhada()
                .resolve("colaboradores")
                .resolve(usuarioId.toString())
                .resolve(nomeArquivo);

        if (Files.exists(caminhoArquivo)) {
            return Files.readAllBytes(caminhoArquivo);
        }
        return null;
    }
    
    public byte[] buscarBytesArquivoPaciente(Long usuarioId, String nomeArquivo) throws IOException {
        Path caminhoArquivo = resolvePastaCompartilhada()
                .resolve("pacientes")
                .resolve(usuarioId.toString())
                .resolve(nomeArquivo);

        if (Files.exists(caminhoArquivo)) {
            return Files.readAllBytes(caminhoArquivo);
        }
        return null;
    }
    
    public String salvarDocumentoJSF(Long usuarioId, String nomePadrao, UploadedFile arquivo) throws IOException {
        if (arquivo == null || arquivo.getFileName() == null || arquivo.getFileName().isEmpty()) {
            return null;
        }
        
        String contentType = arquivo.getContentType();
        if (!contentType.equals("application/pdf") && !contentType.startsWith("image/")) {
            throw new IOException("Tipo de arquivo não permitido.");
        }

        Path pastaUsuario = resolvePastaCompartilhada()
                .resolve("colaboradores")
                .resolve(usuarioId.toString());

        Files.createDirectories(pastaUsuario);

        // --- NOVIDADE: Limpeza de arquivos antigos com o mesmo nome base ---
        // Isso remove 'diploma.jpg' se você estiver subindo um 'diploma.pdf'
        try (Stream<Path> arquivos = Files.list(pastaUsuario)) {
            arquivos.filter(path -> path.getFileName().toString().startsWith(nomePadrao))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Não foi possível deletar arquivo antigo: " + path);
                        }
                    });
        }

        // Extrai a nova extensão
        String originalName = arquivo.getFileName();
        String extensao = originalName.substring(originalName.lastIndexOf("."));
        
        String nomeFinal = nomePadrao + extensao;
        Path destino = pastaUsuario.resolve(nomeFinal);

        // Salva o novo arquivo
        try (InputStream input = arquivo.getInputStream()) {
            Files.copy(input, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return nomeFinal;
    }
}
