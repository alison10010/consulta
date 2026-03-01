package com.consulta.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MunicipioService {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // cache UF -> lista
    private final Map<String, List<String>> cache = new HashMap<>();

    public List<String> listarMunicipiosPorUF(String uf) {
        String u = normalizarUF(uf);
        if (u == null) return Collections.emptyList();

        List<String> cached = cache.get(u);
        if (cached != null) return cached;

        try {
            String url = "https://servicodados.ibge.gov.br/api/v1/localidades/estados/" + u + "/municipios";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return Collections.emptyList();

            List<String> lista = extrairSomenteNomesMunicipios(resp.body());

            // ordena e remove duplicados (só por segurança)
            lista = lista.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();

            List<String> imutavel = Collections.unmodifiableList(lista);
            cache.put(u, imutavel);
            return imutavel;

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<String> extrairSomenteNomesMunicipios(String json) throws Exception {
        List<String> out = new ArrayList<>();
        JsonNode root = mapper.readTree(json);

        // root é um ARRAY de municípios
        if (root != null && root.isArray()) {
            for (JsonNode item : root) {
                JsonNode nome = item.get("nome"); // <-- só o nome do município (top-level)
                if (nome != null && !nome.isNull()) {
                    out.add(nome.asText());
                }
            }
        }
        return out;
    }

    private String normalizarUF(String uf) {
        if (uf == null) return null;
        String u = uf.trim().toUpperCase();
        return u.length() == 2 ? u : null;
    }

    public void limparCache() {
        cache.clear();
    }
}
