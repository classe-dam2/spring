package com.iesabadell.videojocs.catan.config;

import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import com.iesabadell.videojocs.catan.service.PartidaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.service.JugadorService;

/*
 * Un CommandLineRunner s'executa UN COP en arrencar l'aplicació: quan la BD ja està connectada
 * i les taules creades, i abans d'atendre cap petició.
 *
 * Amb ddl-auto=update les dades es conserven entre execucions: si ja n'hi ha, no es tornen a carregar.
 * Amb ddl-auto=create les taules són noves a cada arrencada: sempre es carreguen.
 */
@Component
public class CarregaInicial implements CommandLineRunner {

    @Autowired
    private PartidaService service;

    @Autowired
    private PartidaRepository vRepository;

    @Override
    public void run(String... args) throws Exception {
        if (vRepository.count() > 0) {
            System.out.println("La BD ja té dades: no es carreguen els CSV");
            return;
        }
        int n = service.carregarDades();
        System.out.println("Registres del tauler carregats dels CSV: " + n);
    }

}
