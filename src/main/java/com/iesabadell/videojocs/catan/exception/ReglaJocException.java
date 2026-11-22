package com.iesabadell.videojocs.catan.exception;

import org.springframework.http.HttpStatus;

/**
 * Una regla del joc (o una dada de l'acció) impedeix fer l'acció. Com que és RuntimeException,
 * el @Transactional del servei desfà tot el que s'havia canviat; GestorError la converteix en la resposta HTTP.
 */
public class ReglaJocException extends RuntimeException {
    private final HttpStatus status;

    public ReglaJocException(HttpStatus status, String missatge) {
        super(missatge);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
