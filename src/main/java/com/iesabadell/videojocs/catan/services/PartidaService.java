package com.iesabadell.videojocs.catan.services;

import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PartidaService {
    @Autowired
    private JugadorRepository jugador;

    @Autowired
    private PartidaRepository partida;
}
