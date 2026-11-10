package com.iesabadell.videojocs.catan.controller;

import com.iesabadell.videojocs.catan.service.PartidaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// Prefix de totes les URL d'aquesta classe: localhost:8080/api/plataformes
@RequestMapping(path="/partida")
public class PartidaController {

    @Autowired
    private PartidaService service;
}
