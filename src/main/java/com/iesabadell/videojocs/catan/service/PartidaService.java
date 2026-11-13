package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.dto.HexagonTaulerDTO;
import com.iesabadell.videojocs.catan.models.Jugador;
import com.iesabadell.videojocs.catan.models.Partida;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PartidaService {

    @Autowired
    private PartidaRepository partidaRepository;


    @Transactional
    public ResponseEntity<?> tauler(Integer idPartida) {
        Partida partida = partidaRepository.findById(idPartida)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La partida " + idPartida + " no existeix"));

        // lladre és EAGER a Partida: ja ve carregat
        Integer idLladre = partida.getLladre() != null ? partida.getLladre().getId() : null;
        List result = partidaRepository.findTauler(idPartida).stream()
                .map(h -> HexagonTaulerDTO.de(h, h.getId().equals(idLladre)))
                .toList();


        return ResponseEntity.ok()
                .header("Header-Customitzat", "Total videojocs: " + result.size())
                .body(result);
    }

    @Transactional
    public ResponseEntity<?> crearPartida(List<String> jugadors, Optional<Integer> puntsPerGuanyar) {

        Partida p = new Partida();

        List<Jugador> finalJugadors = new ArrayList<>();

        for (String j : jugadors) {
            Jugador jj = new Jugador();
            jj.setNom(j);
            finalJugadors.add(jj);
        }

        p.setJugadors(finalJugadors);

        if (puntsPerGuanyar.isPresent()) {
            p.setPuntsPerGuanyar(puntsPerGuanyar.get());
        }

        partidaRepository.save(p);


    }


}
