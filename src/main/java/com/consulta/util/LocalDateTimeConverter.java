package com.consulta.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter(value = "localDateTimeConverter")
public class LocalDateTimeConverter implements Converter<LocalDateTime> {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public String getAsString(FacesContext ctx, UIComponent c, LocalDateTime value) {
        return value == null ? "" : value.format(FMT);
    }

    @Override
    public LocalDateTime getAsObject(FacesContext ctx, UIComponent c, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(value, FMT);
    }
}
