package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JugadorService {
    @Autowired
    private JugadorRepository jugador;

    @Autowired
    private PartidaRepository partida;
}
