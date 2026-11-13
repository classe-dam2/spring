package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.dto.HexagonTaulerDTO;
import com.iesabadell.videojocs.catan.dto.PartidaResponseDTO;
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
import java.util.Random;

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
        
        return ResponseEntity.status(HttpStatus.CREATED).body(PartidaResponseDTO.de(p));

    }


    public ResponseEntity<?> tirarDaus(Integer idPartida) {

        Random r = new Random();
        int dau1 = r.nextInt(6) + 1;
        int dau2 = r.nextInt(6) + 1;

        int tirada = dau1 + dau2;

        Optional<Partida> p = partidaRepository.findById(idPartida);

        if (p.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Partida partida = p.get();

        partida.setUltimaTirada(tirada);
        partida.setDausTirats(true);


    }

    private void descartarRecursos(Partida partida) {
        for (Jugador jugador : partida.getJugadors()) {
            int total = ReglesJoc.totalRecursos(jugador);
            if (total <= 7) continue;

            int perDescartar = total / 2;
            for (int i = 0; i < perDescartar; i++) {
                int index = RANDOM.nextInt(total);
                for (RecursJugador recurs : jugador.getRecursos()) {
                    if (index < recurs.getQuantitat()) {
                        ReglesJoc.descomptar(jugador, recurs.getTipus(), 1);
                        total--;
                        break;
                    }
                    index -= recurs.getQuantitat();
                }
            }
        }
    }


}
