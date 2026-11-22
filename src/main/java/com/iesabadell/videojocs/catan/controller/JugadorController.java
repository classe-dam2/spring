package com.iesabadell.videojocs.catan.controller;

import com.iesabadell.videojocs.catan.service.JugadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
// Prefix de totes les URL d'aquesta classe: localhost:8080/api/plataformes
@RequestMapping("/jugador")
public class JugadorController {

    @Autowired
    private JugadorService service;
    
    @GetMapping("/{idJugador}/recursos")
    public ResponseEntity<?> recursos(@PathVariable Integer idJugador) {
    	return service.recursos(idJugador);
    }

    @PostMapping("/{idJugador}/passar")
    public ResponseEntity<?> passarTorn(@PathVariable Integer idJugador) {
        return service.passarTorn(idJugador);
    }
    
    @PostMapping("/{idJugador}/construir")
    public ResponseEntity<?> construir(@PathVariable Integer idJugador,
            @RequestParam String tipus, @RequestParam Integer posicio) {

        return service.construir(idJugador, tipus, posicio);
    }
}
