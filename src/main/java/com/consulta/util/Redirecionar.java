package com.consulta.util;

import java.io.Serializable;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;

@SessionScoped
public class Redirecionar implements Serializable {

	private static final long serialVersionUID = 1L;

	
	// REDIRECIONA COM MSG
	public static String irParaURL(String url){
		try {
			FacesContext context = FacesContext.getCurrentInstance();	
			context.getExternalContext().getFlash().setKeepMessages(true);
			context.getExternalContext().redirect("/consulta/"+url);	 
		} catch (Exception e) {
			e.printStackTrace();
		}		      
        return "";
    }
}
