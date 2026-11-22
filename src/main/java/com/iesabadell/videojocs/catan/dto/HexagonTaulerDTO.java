package com.iesabadell.videojocs.catan.dto;

import com.iesabadell.videojocs.catan.models.Hexagon;
import com.iesabadell.videojocs.catan.models.Jugador;
import com.iesabadell.videojocs.catan.models.Vertex;
import com.iesabadell.videojocs.catan.models.abstracts.Assentament;
import com.iesabadell.videojocs.catan.models.abstracts.Ciutat;
import com.iesabadell.videojocs.catan.models.enums.ColorJugador;
import com.iesabadell.videojocs.catan.models.enums.TipusTerreny;

import java.util.List;

// No es retornen les entitats: Vertex <-> Assentament són bidireccionals i Jackson entraria en bucle.
public record HexagonTaulerDTO(
        Integer id,
        Integer codi,
        TipusTerreny terreny,
        Integer numero,          // null al desert
        Integer q,
        Integer r,
        boolean lladre,
        List<ConstruccioDTO> construccions) {

    // Poblat o ciutat d'un dels 6 vèrtexs de l'hexàgon
    public record ConstruccioDTO(
            String tipus,        // POBLAT | CIUTAT
            Integer posicio,     // 0..5 dins de l'hexàgon
            Integer vertexCodi,
            Integer jugadorId,
            String jugadorNom,
            ColorJugador color) {
    }

    // Requereix hexagonVertexs, vertex i assentament ja carregats (vegeu PartidaRepository.findTauler)
    public static HexagonTaulerDTO de(Hexagon h, boolean lladre) {
        List<ConstruccioDTO> construccions = h.getHexagonVertexs().stream()
                .filter(hv -> hv.getVertex().getAssentament() != null)
                .map(hv -> {
                    Vertex v = hv.getVertex();
                    Assentament a = v.getAssentament();
                    Jugador j = a.getJugador();
                    return new ConstruccioDTO(
                            a instanceof Ciutat ? "CIUTAT" : "POBLAT",
                            hv.getPosicio(),
                            v.getCodi(),
                            j.getId(),
                            j.getNom(),
                            j.getColor());
                })
                .toList();

        return new HexagonTaulerDTO(h.getId(), h.getCodi(), h.getTerreny(), h.getNumero(),
                h.getQ(), h.getR(), lladre, construccions);
    }
}
