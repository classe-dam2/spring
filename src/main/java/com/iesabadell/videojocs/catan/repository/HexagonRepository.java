package com.iesabadell.videojocs.catan.repository;

import com.iesabadell.videojocs.catan.models.Hexagon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HexagonRepository extends JpaRepository<Hexagon, Integer> {
}
