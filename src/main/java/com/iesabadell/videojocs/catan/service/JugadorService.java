package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.exception.ReglaJocException;

import com.iesabadell.videojocs.catan.models.*;


import java.util.List;
import java.util.Map;
import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import com.iesabadell.videojocs.catan.util.Missatges;
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
    @Autowired
    private PartidaRepository partida;

    @Transactional
    public ResponseEntity<String> passarTorn(Integer idJugador) {

        Jugador j = jugador.findById(idJugador).orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
                String.format(Missatges.JUGADOR_NO_EXISTEIX, idJugador)));


        Partida p = j.getPartida();

        if (!ReglesJoc.esElSeuTorn(j, p)) {
            throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.NO_ES_EL_SEU_TORN);
        }

        List<Jugador> jugadors = p.getJugadors();
        int indexActual = jugadors.indexOf(j);
        if (indexActual < 0) {
            throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.NO_ES_EL_SEU_TORN);
        }

        int indexSeguent = (indexActual + 1) % jugadors.size();
        Jugador seguent = jugadors.get(indexSeguent);
        p.setJugadorActiu(seguent);
        p.setDausTirats(false);
        if (indexSeguent == 0) {
            p.setRonda(p.getRonda() + 1);
        }

        return ResponseEntity.ok(String.format(Missatges.INFO_TORN, seguent.getNom(), p.getRonda()));


    }

}
