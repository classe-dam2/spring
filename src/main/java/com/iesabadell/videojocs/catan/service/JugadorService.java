package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.exception.ReglaJocException;
import com.iesabadell.videojocs.catan.models.*;

import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.util.Missatges;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class JugadorService {
    @Autowired
    private JugadorRepository jugadorRepository;
    
    @Transactional
    public ResponseEntity<?> recursos(Integer idJugador) {

        Jugador jugador = jugadorRepository.findById(idJugador)
                .orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
                        String.format(Missatges.JUGADOR_NO_EXISTEIX, idJugador)));

        List<Map<String, Object>> recursos = jugador.getRecursos().stream()
                .map(r -> Map.<String, Object>of(
                        "tipus", r.getTipus(),
                        "quantitat", r.getQuantitat()
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "id", jugador.getId(),
                "nom", jugador.getNom(),
                "recursos", recursos
        ));
    }
}
