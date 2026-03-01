package com.consulta.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public class Mensagens {
	public static void informacao(String titulo, String mensagem) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, titulo, mensagem));
	}

	public static void erro(String titulo, String mensagem) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, titulo, mensagem));
	}

	public static void aviso(String titulo, String mensagem) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, titulo, mensagem));
	}

	public static void info(String titulo, String mensagem) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, titulo, mensagem));
	}
}
