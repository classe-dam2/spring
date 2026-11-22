package com.iesabadell.videojocs.catan.exception;


import java.util.LinkedHashMap;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/*
 * Errors de VALIDACIÓ (sessió 2).
 * Quan @Valid falla, Spring NO arriba a cridar el mètode del controlador (ni, per tant, el servei):
 * llança una excepció. Aquesta classe la captura per a TOTS els controladors i en fa la resposta.
 *
 * La resta d'errors (no existeix, regla de negoci) els llança el servei com a ReglaJocException
 * (o ResponseStatusException): fan rollback de la transacció i es converteixen aquí en la resposta.
 */
@RestControllerAdvice
public class GestorError {

    // @Valid @RequestBody ha fallat -> 400 amb un JSON {camp: missatge}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LinkedHashMap<String, String>> validacioCos(MethodArgumentNotValidException e) {
        LinkedHashMap<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .header("Header-Customitzat", "DADES NO VALIDES")
                .body(errors);
    }

    // Una validació d'un @PathVariable o @RequestParam ha fallat (per exemple, @Positive) -> 400
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<String> validacioParametres(HandlerMethodValidationException e) {
        String missatge = "";
        for (MessageSourceResolvable error : e.getAllErrors()) {
            missatge += error.getDefaultMessage() + "\n";
        }
        return ResponseEntity.badRequest()
                .header("Header-Customitzat", "PARAMETRES NO VALIDS")
                .body(missatge);
    }

    // Una regla del joc no es compleix (no existeix, no és el teu torn, recursos insuficients...) -> el codi de l'excepció
    @ExceptionHandler(ReglaJocException.class)
    public ResponseEntity<String> reglaJoc(ReglaJocException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }

}
