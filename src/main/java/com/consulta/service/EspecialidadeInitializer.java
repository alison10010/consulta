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
		    "Acupuntura",
		    "Alergia e Imunologia",
		    "Anestesiologia",
		    "Angiologia",
		    "Cardiologia",
		    "Cirurgia Cardiovascular",
		    "Cirurgia da Mão",
		    "Cirurgia de Cabeça e Pescoço",
		    "Cirurgia do Aparelho Digestivo",
		    "Cirurgia Geral",
		    "Cirurgia Oncológica",
		    "Cirurgia Pediátrica",
		    "Cirurgia Plástica",
		    "Cirurgia Torácica",
		    "Cirurgia Vascular",
		    "Clínica Médica",
		    "Coloproctologia",
		    "Dermatologia",
		    "Endocrinologia e Metabologia",
		    "Endoscopia",
		    "Gastroenterologia",
		    "Genética Médica",
		    "Geriatria",
		    "Ginecologia e Obstetrícia",
		    "Hematologia e Hemoterapia",
		    "Homeopatia",
		    "Infectologia",
		    "Mastologia",
		    "Medicina de Emergência",
		    "Medicina de Família e Comunidade",
		    "Medicina do Trabalho",
		    "Medicina de Tráfego",
		    "Medicina Esportiva",
		    "Medicina Física e Reabilitação",
		    "Medicina Intensiva",
		    "Medicina Legal e Perícia Médica",
		    "Medicina Nuclear",
		    "Medicina Preventiva e Social",
		    "Nefrologia",
		    "Neurocirurgia",
		    "Neurologia",
		    "Nutrologia",
		    "Oftalmologia",
		    "Oncologia Clínica",
		    "Ortopedia e Traumatologia",
		    "Otorrinolaringologia",
		    "Patologia",
		    "Patologia Clínica/Medicina Laboratorial",
		    "Pediatria",
		    "Pneumologia",
		    "Psiquiatria",
		    "Radiologia e Diagnóstico por Imagem",
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

