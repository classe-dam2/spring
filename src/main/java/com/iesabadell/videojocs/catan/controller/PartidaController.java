package com.iesabadell.videojocs.catan.controller;

import com.iesabadell.videojocs.catan.dto.HexagonTaulerDTO;
import com.iesabadell.videojocs.catan.models.Jugador;
import com.iesabadell.videojocs.catan.models.Partida;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import com.iesabadell.videojocs.catan.service.PartidaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RequestMapping("/partida")
@RestController
public class PartidaController {

    @Autowired
    private PartidaService service;

    // Hexàgons de la partida amb terreny, número, lladre i construccions adjacents
    @GetMapping("/{idPartida}/tauler")
    public ResponseEntity<?> tauler(@PathVariable Integer idPartida) {

        return service.tauler(idPartida);
    }

    @PostMapping("/")
    public ResponseEntity<?> crearPartida(@RequestParam List<String> jugadors, @RequestParam Optional<Integer> puntsPerGuanyar) {

        return service.crearPartida(jugadors, puntsPerGuanyar);

    }

    @PostMapping("/{idPartida}/daus")
    public ResponseEntity<?> tirarDaus(@PathVariable Integer idPartida, @RequestParam Integer idJugador) {
        return service.tirarDaus(idPartida, idJugador);
    }
}
