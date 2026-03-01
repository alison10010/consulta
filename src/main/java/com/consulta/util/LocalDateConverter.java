package com.consulta.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter(value = "localDateConverter", managed = true)
public class LocalDateConverter implements Converter<LocalDate> {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String getAsString(FacesContext ctx, UIComponent c, LocalDate value) {
        return value == null ? "" : value.format(FMT);
    }

    @Override
    public LocalDate getAsObject(FacesContext ctx, UIComponent c, String value) {
        if (value == null || value.isBlank()) return null;
        // Se você digitar no formato dd/MM/yyyy
        return LocalDate.parse(value, FMT);
    }
}

