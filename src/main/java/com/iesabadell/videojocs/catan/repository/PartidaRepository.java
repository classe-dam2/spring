package com.iesabadell.videojocs.catan.repository;

import com.iesabadell.videojocs.catan.models.Hexagon;
import com.iesabadell.videojocs.catan.models.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, Integer> {

    // Una sola SELECT amb tot el tauler: hexàgons -> els seus 6 vèrtexs -> assentament de cada vèrtex.
    // El fetch join és obligatori: amb open-in-view=false les col·leccions LAZY no es poden llegir després.
    // LEFT JOIN: hexàgons sense vèrtexs o vèrtexs lliures (sense assentament) també han de sortir.
    @Query("""
            select h from Hexagon h
            left join fetch h.hexagonVertexs hv
            left join fetch hv.vertex v
            left join fetch v.assentament
            where h.partida.id = :idPartida
            order by h.codi, hv.posicio
            """)
    List<Hexagon> findTauler(@Param("idPartida") Integer idPartida);
}
