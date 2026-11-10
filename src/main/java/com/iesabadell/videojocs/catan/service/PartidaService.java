package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.dto.HexagonTaulerDTO;
import com.iesabadell.videojocs.catan.models.Aresta;
import com.iesabadell.videojocs.catan.models.Hexagon;
import com.iesabadell.videojocs.catan.models.HexagonVertex;
import com.iesabadell.videojocs.catan.models.Partida;
import com.iesabadell.videojocs.catan.models.Vertex;
import com.iesabadell.videojocs.catan.models.enums.EstatPartida;
import com.iesabadell.videojocs.catan.models.enums.TipusTerreny;
import com.iesabadell.videojocs.catan.repository.ArestaRepository;
import com.iesabadell.videojocs.catan.repository.HexagonRepository;
import com.iesabadell.videojocs.catan.repository.HexagonVertexRepository;
import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import com.iesabadell.videojocs.catan.repository.VertexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class PartidaService {

    @Autowired
    private PartidaRepository partidaRepository;


    private static final String CARPETA_CSV = "dades/";

    // Els 19 hexàgons del Catan: 4 bosc, 4 pastures, 4 camps, 3 turons, 3 muntanyes i 1 desert
    private static final List<TipusTerreny> TERRENYS = Stream.of(
                    Collections.nCopies(4, TipusTerreny.BOSC),
                    Collections.nCopies(4, TipusTerreny.PASTURES),
                    Collections.nCopies(4, TipusTerreny.CAMPS),
                    Collections.nCopies(3, TipusTerreny.TURONS),
                    Collections.nCopies(3, TipusTerreny.MUNTANYES),
                    List.of(TipusTerreny.DESERT))
            .flatMap(List::stream)
            .toList();

    // Fitxes de número dels 18 hexàgons que no són desert (no hi ha 7)
    private static final List<Integer> NUMEROS = List.of(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12);

    @Autowired
    private JugadorRepository jugador;

    @Autowired
    private PartidaRepository partida;

    @Autowired
    private HexagonRepository hexagon;

    @Autowired
    private VertexRepository vertex;

    @Autowired
    private ArestaRepository aresta;

    @Autowired
    private HexagonVertexRepository hexagonVertex;

    /*
     * Crea una partida en PREPARACIO amb el tauler dels CSV de src/main/resources/dades:
     * hexagons.csv, vertexs.csv, arestes.csv i hexagon_vertex.csv.
     *
     * Els CSV només defineixen la geometria. El terreny i el número de cada hexàgon no hi són:
     * es reparteixen a l'atzar, i el lladre comença al desert.
     * Les coordenades x,y de vertexs.csv no es guarden: Vertex no té aquests camps.
     *
     * Tot dins d'una sola transacció: si un CSV és invàlid no queda mig tauler a la BD.
     * Retorna el nombre de registres del tauler creats (hexàgons + vèrtexs + arestes + hexagon_vertex).
     */

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
    public int carregarDades() {
        Partida nova = new Partida();
        nova.setEstat(EstatPartida.PREPARACIO);
        nova.setDataInici(LocalDateTime.now());
        partida.save(nova);

        Map<Integer, Hexagon> hexagons = carregarHexagons(nova);
        Map<Integer, Vertex> vertexs = carregarVertexs(nova);
        int arestes = carregarArestes(nova, vertexs);
        int hexagonVertexs = carregarHexagonVertexs(hexagons, vertexs);

        // nova és gestionada: el canvi es guarda en acabar la transacció
        hexagons.values().stream()
                .filter(h -> h.getTerreny() == TipusTerreny.DESERT)
                .findFirst()
                .ifPresent(nova::setLladre);

        return hexagons.size() + vertexs.size() + arestes + hexagonVertexs;
    }

    // codi (id_hexagon del CSV) -> hexàgon ja guardat
    private Map<Integer, Hexagon> carregarHexagons(Partida nova) {
        List<Map<String, String>> files = llegirCsv("hexagons.csv");
        if (files.size() != TERRENYS.size()) {
            throw new IllegalStateException("hexagons.csv ha de tenir " + TERRENYS.size()
                    + " hexàgons i en té " + files.size());
        }

        List<TipusTerreny> terrenys = new ArrayList<>(TERRENYS);
        List<Integer> numeros = new ArrayList<>(NUMEROS);
        Collections.shuffle(terrenys);
        Collections.shuffle(numeros);
        Iterator<Integer> seguentNumero = numeros.iterator();

        Map<Integer, Hexagon> perCodi = new LinkedHashMap<>();
        for (int i = 0; i < files.size(); i++) {
            Map<String, String> fila = files.get(i);
            Hexagon h = new Hexagon();
            h.setCodi(enter(fila, "id_hexagon"));
            h.setQ(enter(fila, "q"));
            h.setR(enter(fila, "r"));
            h.setTerreny(terrenys.get(i));
            h.setNumero(h.getTerreny() == TipusTerreny.DESERT ? null : seguentNumero.next());
            h.setPartida(nova);
            if (perCodi.put(h.getCodi(), h) != null) {
                throw new IllegalStateException("hexagons.csv: id_hexagon repetit: " + h.getCodi());
            }
        }
        hexagon.saveAll(perCodi.values());
        return perCodi;
    }

    // codi (id_vertex del CSV) -> vèrtex ja guardat
    private Map<Integer, Vertex> carregarVertexs(Partida nova) {
        Map<Integer, Vertex> perCodi = new LinkedHashMap<>();
        for (Map<String, String> fila : llegirCsv("vertexs.csv")) {
            Vertex v = new Vertex();
            v.setCodi(enter(fila, "id_vertex"));
            v.setPartida(nova);
            if (perCodi.put(v.getCodi(), v) != null) {
                throw new IllegalStateException("vertexs.csv: id_vertex repetit: " + v.getCodi());
            }
        }
        vertex.saveAll(perCodi.values());
        return perCodi;
    }

    private int carregarArestes(Partida nova, Map<Integer, Vertex> vertexs) {
        List<Aresta> arestes = new ArrayList<>();
        for (Map<String, String> fila : llegirCsv("arestes.csv")) {
            Aresta a = new Aresta();
            a.setCodi(enter(fila, "id_aresta"));
            a.setVertexA(buscar(vertexs, enter(fila, "id_vertex_a"), "arestes.csv", "vèrtex"));
            a.setVertexB(buscar(vertexs, enter(fila, "id_vertex_b"), "arestes.csv", "vèrtex"));
            a.setPartida(nova);
            arestes.add(a);
        }
        aresta.saveAll(arestes);
        return arestes.size();
    }

    private int carregarHexagonVertexs(Map<Integer, Hexagon> hexagons, Map<Integer, Vertex> vertexs) {
        List<HexagonVertex> relacions = new ArrayList<>();
        for (Map<String, String> fila : llegirCsv("hexagon_vertex.csv")) {
            HexagonVertex hv = new HexagonVertex();
            hv.setPosicio(enter(fila, "posicio"));
            hv.setHexagon(buscar(hexagons, enter(fila, "id_hexagon"), "hexagon_vertex.csv", "hexàgon"));
            hv.setVertex(buscar(vertexs, enter(fila, "id_vertex"), "hexagon_vertex.csv", "vèrtex"));
            relacions.add(hv);
        }
        hexagonVertex.saveAll(relacions);
        return relacions.size();
    }

    private static <T> T buscar(Map<Integer, T> perCodi, Integer codi, String csv, String que) {
        T trobat = perCodi.get(codi);
        if (trobat == null) {
            throw new IllegalStateException(csv + ": " + que + " " + codi + " no existeix");
        }
        return trobat;
    }

    private static Integer enter(Map<String, String> fila, String columna) {
        String valor = fila.get(columna);
        if (valor == null) {
            throw new IllegalStateException("Falta la columna " + columna + " al CSV");
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Valor no numèric a la columna " + columna + ": " + valor, e);
        }
    }

    // Cada fila és un mapa columna -> valor, segons la capçalera; així no depèn de l'ordre de les columnes
    private List<Map<String, String>> llegirCsv(String fitxer) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource(CARPETA_CSV + fitxer).getInputStream(), StandardCharsets.UTF_8))) {
            String linia = br.readLine();
            if (linia == null) {
                throw new IllegalStateException(fitxer + " és buit");
            }
            String[] capcalera = linia.trim().split(",");

            List<Map<String, String>> files = new ArrayList<>();
            while ((linia = br.readLine()) != null) {
                if (linia.isBlank()) {
                    continue;
                }
                String[] valors = linia.trim().split(",");
                if (valors.length != capcalera.length) {
                    throw new IllegalStateException(fitxer + ": la fila '" + linia + "' no té "
                            + capcalera.length + " columnes");
                }
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 0; i < capcalera.length; i++) {
                    fila.put(capcalera[i].trim(), valors[i].trim());
                }
                files.add(fila);
            }
            return files;
        } catch (IOException e) {
            throw new IllegalStateException("No es pot llegir " + fitxer, e);
        }
    }
}
