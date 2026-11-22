package com.iesabadell.videojocs.catan.repository;

import com.iesabadell.videojocs.catan.models.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JugadorRepository extends JpaRepository<Jugador, Integer> {
}
