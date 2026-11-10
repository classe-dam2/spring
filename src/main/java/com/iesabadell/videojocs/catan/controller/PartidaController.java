package com.iesabadell.videojocs.catan.controller;

import com.iesabadell.videojocs.catan.dto.HexagonTaulerDTO;
import com.iesabadell.videojocs.catan.models.Partida;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class PartidaController {

    private final PartidaRepository partidaRepository;

    public PartidaController(PartidaRepository partidaRepository) {
        this.partidaRepository = partidaRepository;
    }

    // Hexàgons de la partida amb terreny, número, lladre i construccions adjacents
    @GetMapping("/partida/{idPartida}/tauler")
    public List<HexagonTaulerDTO> tauler(@PathVariable Integer idPartida) {
        Partida partida = partidaRepository.findById(idPartida)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La partida " + idPartida + " no existeix"));

        // lladre és EAGER a Partida: ja ve carregat
        Integer idLladre = partida.getLladre() != null ? partida.getLladre().getId() : null;

        return partidaRepository.findTauler(idPartida).stream()
                .map(h -> HexagonTaulerDTO.de(h, h.getId().equals(idLladre)))
                .toList();
    }
}
