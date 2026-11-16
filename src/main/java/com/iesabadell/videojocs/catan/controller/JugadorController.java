package com.iesabadell.videojocs.catan.controller;

import com.iesabadell.videojocs.catan.service.JugadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
// Prefix de totes les URL d'aquesta classe: localhost:8080/api/plataformes
@RequestMapping(path="/jugador")
public class JugadorController {

    @Autowired
    private JugadorService service;


    @PostMapping("/{idJugador}/passar")
    public ResponseEntity<String> passarTorn(@PathVariable Integer idJugador) {
        return service.passarTorn(idJugador);
    }
}
