package com.consulta.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.consulta.model.Especialidade;
import com.consulta.repository.EspecialidadeRepository;

import jakarta.annotation.PostConstruct;

@Component
public class EspecialidadeInitializer {

    private final EspecialidadeRepository especialidadeRepository;

    public EspecialidadeInitializer(EspecialidadeRepository especialidadeRepository) {
        this.especialidadeRepository = especialidadeRepository;
    }

    @PostConstruct
    @Transactional
    public void init() {

    	List<String> especialidades = List.of(
	        "Cardiologia",
	        "Dermatologia",
	        "Psiquiatria",
	        "Alergia e Imunologia",
	        "Anestesiologia",
	        "Angiologia",
	        "Coloproctologia",
	        "Endocrinologia",
	        "Endoscopia",
	        "Gastroenterologia",
	        "Geriatria",
	        "Ginecologia e Obstetrícia",
	        "Hematologia e Hemoterapia",
	        "Infectologia",
	        "Mastologia",
	        "Nefrologia",
	        "Neurologia",
	        "Nutrologia",
	        "Oftalmologia",
	        "Oncologia Clínica",
	        "Ortopedia e Traumatologia",
	        "Otorrinolaringologia",
	        "Patologia",
	        "Pediatria",
	        "Pneumologia",
	        "Psicológico",
	        "Psiquiatra",
	        "Radiologia",
	        "Radioterapia",
	        "Reumatologia",
	        "Urologia"
	    );        
    	
    	especialidades.forEach(this::inserir);
    }

    private void inserir(String nome) {
        if (!especialidadeRepository.existsByEspecialidadeIgnoreCase(nome)) {
            Especialidade e = new Especialidade();
            e.setEspecialidade(nome);
            especialidadeRepository.save(e);
        }
    }
}

