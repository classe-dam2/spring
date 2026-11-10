package com.iesabadell.videojocs.catan.controller;

import com.iesabadell.videojocs.catan.dto.HexagonTaulerDTO;
import com.iesabadell.videojocs.catan.models.Partida;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import com.iesabadell.videojocs.catan.service.PartidaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class PartidaController {

    @Autowired
    private PartidaService service;

    // Hexàgons de la partida amb terreny, número, lladre i construccions adjacents
    @GetMapping("/partida/{idPartida}/tauler")
    public ResponseEntity<?> tauler(@PathVariable Integer idPartida) {

        return service.tauler(idPartida);
    }
}
