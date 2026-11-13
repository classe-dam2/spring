package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.dto.HexagonTaulerDTO;
import com.iesabadell.videojocs.catan.dto.PartidaResponseDTO;
import com.iesabadell.videojocs.catan.exception.ReglaJocException;
import com.iesabadell.videojocs.catan.models.*;
import com.iesabadell.videojocs.catan.models.abstracts.Assentament;
import com.iesabadell.videojocs.catan.models.abstracts.Ciutat;
import com.iesabadell.videojocs.catan.models.enums.ColorJugador;
import com.iesabadell.videojocs.catan.models.enums.EstatPartida;
import com.iesabadell.videojocs.catan.models.enums.TipusRecurs;
import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import com.iesabadell.videojocs.catan.util.Missatges;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class PartidaService {

    private static final Random RANDOM = new Random();

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private JugadorRepository jugadorRepository;

    @Autowired
    private CarregarService carregarService;

    private static final int TIRADA_SENSE_PRODUCCIO = 7;



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
        if (jugadors.size() < 2 || jugadors.size() > 4) {
            throw new ReglaJocException(HttpStatus.BAD_REQUEST, Missatges.JUGADORS_INVALIDS);
        }

        Partida p = new Partida();
        p.setEstat(EstatPartida.EN_CURS);
        p.setDataInici(LocalDateTime.now());
        puntsPerGuanyar.ifPresent(p::setPuntsPerGuanyar);

        List<Jugador> finalJugadors = new ArrayList<>();
        for (int i = 0; i < jugadors.size(); i++) {
            Jugador jj = new Jugador();
            jj.setNom(jugadors.get(i));
            jj.setOrdreTorn(i + 1);
            jj.setColor(ColorJugador.values()[i]);
            jj.setPartida(p);
            finalJugadors.add(jj);
        }
        p.setJugadors(finalJugadors);

        partidaRepository.save(p); // els jugadors es guarden amb la partida (cascade)
        p.setJugadorActiu(finalJugadors.getFirst());
        carregarService.crearTauler(p);
        // PartidaResponseDTO llegeix p.getHexagons(), que és el costat invers i crearTauler no l'omple
        p.setHexagons(partidaRepository.findTauler(p.getId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(PartidaResponseDTO.de(p));
    }


    @Transactional
    public ResponseEntity<?> tirarDaus(Integer idPartida, Integer idJugador) {
        Partida partida = partidaRepository.findById(idPartida)
                .orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
                        String.format(Missatges.PARTIDA_NO_EXISTEIX, idPartida)));
        Jugador jugador = jugadorRepository.findById(idJugador)
                .orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
                        String.format(Missatges.JUGADOR_NO_EXISTEIX, idJugador)));
        ReglesJoc.validarTirada(partida, jugador);

        int dau1 = RANDOM.nextInt(6) + 1;
        int dau2 = RANDOM.nextInt(6) + 1;
        int tirada = dau1 + dau2;

        partida.setUltimaTirada(tirada);
        partida.setDausTirats(true);
        if (tirada == TIRADA_SENSE_PRODUCCIO) {
            descartarRecursos(partida);
            partida.setLladrePendent(true);
        } else {
            produir(partida, tirada);
        }
        return ResponseEntity.ok(Map.of("dau1", dau1, "dau2", dau2, "tirada", tirada));
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

    private void produir(Partida partida, int tirada) {
        for (Hexagon h : partida.getHexagons()) {
            if (h.getNumero() == null || h.getNumero() != tirada || esElLladre(partida, h)) continue;
            TipusRecurs recurs = ReglesJoc.recursDe(h.getTerreny());
            for (HexagonVertex hv : h.getHexagonVertexs()) {
                Assentament a = hv.getVertex().getAssentament();
                if (a == null) continue;
                int quantitat = a instanceof Ciutat ? 2 : 1;
                ReglesJoc.afegir(a.getJugador(), recurs, quantitat);
            }
        }
    }

    private boolean esElLladre(Partida partida, Hexagon h) {
        return partida.getLladre() != null && partida.getLladre().getId().equals(h.getId());
    }


}
