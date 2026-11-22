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
import java.util.Collections;
import java.util.List;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
        p.setEstat(EstatPartida.PREPARACIO);
        p.setDataInici(LocalDateTime.now());
        puntsPerGuanyar.ifPresent(p::setPuntsPerGuanyar);

        List<String> jugadorsBarrejats = new ArrayList<>(jugadors);
        Collections.shuffle(jugadorsBarrejats, RANDOM);

        List<Jugador> finalJugadors = new ArrayList<>();
        for (int i = 0; i < jugadorsBarrejats.size(); i++) {
            Jugador jj = new Jugador();
            jj.setNom(jugadorsBarrejats.get(i));
            jj.setOrdreTorn(i + 1);
            jj.setColor(ColorJugador.values()[i]);
            jj.setPartida(p);

            List<RecursJugador> recursos = new ArrayList<>(TipusRecurs.values().length);
            for (TipusRecurs tipus : TipusRecurs.values()) {
                RecursJugador recurs = new RecursJugador();
                recurs.setTipus(tipus);
                recurs.setQuantitat(0);
                recurs.setJugador(jj);
                recursos.add(recurs);
            }
            jj.setRecursos(recursos);
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
        Jugador jugador = idJugador == null ? partida.getJugadorActiu() :
                jugadorRepository.findById(idJugador)
                        .orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
                                String.format(Missatges.JUGADOR_NO_EXISTEIX, idJugador)));
        ReglesJoc.validarTirada(partida, jugador);

        int dau1 = RANDOM.nextInt(6) + 1;
        int dau2 = RANDOM.nextInt(6) + 1;
        int tirada = dau1 + dau2;

        partida.setUltimaTirada(tirada);
        partida.setDausTirats(true);
        Map<Integer, Map<TipusRecurs, Integer>> recursos = new LinkedHashMap<>();
        if (tirada != TIRADA_SENSE_PRODUCCIO) {
            produir(partida, tirada, recursos);
        }
        return ResponseEntity.ok(Map.of("dau1", dau1, "dau2", dau2, "tirada", tirada, "recursos", recursos));
    }


    private void produir(Partida partida, int tirada, Map<Integer, Map<TipusRecurs, Integer>> recursos) {
        for (Hexagon h : partida.getHexagons()) {
            if (h.getNumero() == null || h.getNumero() != tirada || esElLladre(partida, h)) continue;
            TipusRecurs recurs = ReglesJoc.recursDe(h.getTerreny());
            for (HexagonVertex hv : h.getHexagonVertexs()) {
                Assentament a = hv.getVertex().getAssentament();
                if (a == null) continue;
                int quantitat = a instanceof Ciutat ? 2 : 1;
                ReglesJoc.afegir(a.getJugador(), recurs, quantitat);
                recursos.computeIfAbsent(a.getJugador().getId(), id -> new EnumMap<>(TipusRecurs.class))
                        .merge(recurs, quantitat, Integer::sum);
            }
        }
    }

    private boolean esElLladre(Partida partida, Hexagon h) {
        return partida.getLladre() != null && partida.getLladre().getId().equals(h.getId());
    }


}
