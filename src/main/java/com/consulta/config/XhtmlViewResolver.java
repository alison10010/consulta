package com.consulta.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

public class XhtmlViewResolver {
	
	@Bean
	public ViewResolver xhtmlViewResolver() {
	    InternalResourceViewResolver resolver = new InternalResourceViewResolver();
	    resolver.setPrefix("/view/");
	    resolver.setSuffix(".xhtml");
	    resolver.setViewNames("*"); // aceita todas as views
	    resolver.setOrder(1);       // alta prioridade
	    return resolver;
	}
}
