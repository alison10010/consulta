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

    public String salvarDocumento(Long usuarioId, String nomePadrao, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        // Define a pasta: .../colaboradores/{id}/
        Path pastaUsuario = resolvePastaCompartilhada()
                .resolve("colaboradores")
                .resolve(usuarioId.toString());

        // Cria a árvore de diretórios se não existir
        Files.createDirectories(pastaUsuario);

        // Extrai extensão original
        String originalName = arquivo.getOriginalFilename();
        String extensao = (originalName != null && originalName.contains(".")) 
                ? originalName.substring(originalName.lastIndexOf(".")) 
                : "";

        String nomeFinal = nomePadrao + extensao;
        Path destino = pastaUsuario.resolve(nomeFinal);

        // Salva o arquivo no disco (substitui se já existir)
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        
        return nomeFinal; // Retorna apenas o nome do arquivo para o banco
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
