package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.exception.ReglaJocException;
import com.iesabadell.videojocs.catan.models.Partida;
import com.iesabadell.videojocs.catan.models.*;
import com.iesabadell.videojocs.catan.models.abstracts.Ciutat;
import com.iesabadell.videojocs.catan.models.abstracts.Construccio;
import com.iesabadell.videojocs.catan.models.abstracts.Poblat;
import com.iesabadell.videojocs.catan.models.enums.EstatPartida;
import com.iesabadell.videojocs.catan.models.enums.TipusCarta;
import com.iesabadell.videojocs.catan.models.enums.TipusRecurs;
import com.iesabadell.videojocs.catan.models.enums.TipusTerreny;
import com.iesabadell.videojocs.catan.util.Missatges;
import org.springframework.http.HttpStatus;

/**
 * Regles i operacions que comparteixen els serveis: la comprovació comuna d'una acció,
 * el recompte de punts, els recursos i la victòria.
 */
final class ReglesJoc {
    private ReglesJoc() {
    }

    /*
     * Comprovació comuna a totes les accions d'un jugador: partida no finalitzada, torn del jugador
     * i, en EN_CURS, daus tirats. Si permetPreparacio és fals, l'acció només val en EN_CURS.
     * Si l'acció no es pot fer, llança ReglaJocException (409) amb el motiu.
     */
    static void validarAccio(Partida partida, Jugador jugador, boolean permetPreparacio) {
        if (partida.getEstat() == EstatPartida.FINALITZADA) throw conflicte(Missatges.PARTIDA_FINALITZADA);
        if (!esElSeuTorn(jugador, partida)) throw conflicte(Missatges.NO_ES_EL_SEU_TORN);
        if (partida.getEstat() == EstatPartida.PREPARACIO) {
            if (!permetPreparacio) throw conflicte(Missatges.NOMES_EN_CURS);
            return;
        }
        if (partida.isLladrePendent()) throw conflicte(Missatges.LLADRE_PENDENT);
        if (!partida.isDausTirats()) throw conflicte(Missatges.DAUS_NO_TIRATS);
    }

    /*
     * Comprovació de tirar els daus: partida en curs, torn del jugador i daus encara sense tirar.
     * No es pot fer servir validarAccio perquè aquesta exigeix justament que els daus ja estiguin tirats.
     */
    static void validarTirada(Partida partida, Jugador jugador) {
        if (partida.getEstat() == EstatPartida.FINALITZADA) throw conflicte(Missatges.PARTIDA_FINALITZADA);
        if (!esElSeuTorn(jugador, partida)) throw conflicte(Missatges.NO_ES_EL_SEU_TORN);
        if (partida.getEstat() == EstatPartida.PREPARACIO) throw conflicte(Missatges.NOMES_EN_CURS);
        if (partida.isDausTirats()) throw conflicte(Missatges.DAUS_JA_TIRATS);
    }

    // Rebuig d'una acció per l'estat de la partida; el rollback el fa el @Transactional del servei
    static ReglaJocException conflicte(String missatge) {
        return new ReglaJocException(HttpStatus.CONFLICT, missatge);
    }

    static boolean esElSeuTorn(Jugador jugador, Partida partida) {
        Jugador actiu = partida.getJugadorActiu();
        return actiu != null && actiu.getId().equals(jugador.getId());
    }

    static int quantitat(Jugador jugador, TipusRecurs tipus) {
        int total = 0;
        for (RecursJugador r : jugador.getRecursos()) {
            if (r.getTipus() == tipus) total += r.getQuantitat();
        }
        return total;
    }

    static int totalRecursos(Jugador jugador) {
        int total = 0;
        for (RecursJugador r : jugador.getRecursos()) {
            total += r.getQuantitat();
        }
        return total;
    }

    // Resta n unitats del recurs; les comprovacions de quantitat es fan abans de cridar-lo
    static void descomptar(Jugador jugador, TipusRecurs tipus, int n) {
        for (RecursJugador r : jugador.getRecursos()) {
            if (r.getTipus() == tipus) {
                r.setQuantitat(r.getQuantitat() - n);
                return;
            }
        }
    }

    // Suma n unitats al recurs; si el jugador encara no té la fila d'aquest tipus, la crea (Jugador.recursos fa cascade)
    static void afegir(Jugador jugador, TipusRecurs tipus, int n) {
        for (RecursJugador r : jugador.getRecursos()) {
            if (r.getTipus() == tipus) {
                r.setQuantitat(r.getQuantitat() + n);
                return;
            }
        }
        RecursJugador nou = new RecursJugador();
        nou.setJugador(jugador);
        nou.setTipus(tipus);
        nou.setQuantitat(n);
        jugador.getRecursos().add(nou);
    }

    // Recurs que produeix un terreny (null al desert)
    static TipusRecurs recursDe(TipusTerreny terreny) {
        return switch (terreny) {
            case BOSC -> TipusRecurs.FUSTA;
            case TURONS -> TipusRecurs.MAO;
            case PASTURES -> TipusRecurs.LLANA;
            case CAMPS -> TipusRecurs.BLAT;
            case MUNTANYES -> TipusRecurs.MINERAL;
            case DESERT -> null;
        };
    }

    // Els recomptes recorren els vèrtexs i arestes de la partida, que sempre estan sincronitzats amb les construccions
    static int comptarPoblats(Jugador jugador) {
        int total = 0;
        for (Vertex v : jugador.getPartida().getVertexs()) {
            if (v.getAssentament() instanceof Poblat && esDe(v.getAssentament(), jugador)) total++;
        }
        return total;
    }

    static int comptarCiutats(Jugador jugador) {
        int total = 0;
        for (Vertex v : jugador.getPartida().getVertexs()) {
            if (v.getAssentament() instanceof Ciutat && esDe(v.getAssentament(), jugador)) total++;
        }
        return total;
    }

    static int comptarCarreteres(Jugador jugador) {
        int total = 0;
        for (Aresta a : jugador.getPartida().getArestas()) {
            if (a.getCarretera() != null && esDe(a.getCarretera(), jugador)) total++;
        }
        return total;
    }

    // Últim poblat col·locat pel jugador (el de l'id més alt), o null si no en té
    static Poblat ultimPoblat(Jugador jugador) {
        Poblat ultim = null;
        for (Vertex v : jugador.getPartida().getVertexs()) {
            if (v.getAssentament() instanceof Poblat p && esDe(p, jugador)
                    && (ultim == null || p.getId() > ultim.getId())) {
                ultim = p;
            }
        }
        return ultim;
    }

    private static boolean esDe(Construccio c, Jugador jugador) {
        return c.getJugador().getId().equals(jugador.getId());
    }

    static Jugador exercitMesGran(Partida partida) {
        Jugador millor = null;
        int maxim = 2;
        boolean empat = false;
        for (Jugador j : partida.getJugadors()) {
            int cavallers = j.getCavallersJugats();
            if (cavallers > maxim) {
                maxim = cavallers;
                millor = j;
                empat = false;
            } else if (cavallers == maxim && millor != null) {
                empat = true;
            }
        }
        return empat ? null : millor;
    }

    static int calcularPunts(Jugador jugador) {
        int cartes = 0;
        for (CartaDesenvolupament c : jugador.getCartes()) {
            if (c.getTipus() == TipusCarta.PUNT_VICTORIA) cartes++;
        }
        Jugador exercit = exercitMesGran(jugador.getPartida());
        return comptarPoblats(jugador) + 2 * comptarCiutats(jugador) + cartes
                + (exercit != null && exercit.getId().equals(jugador.getId()) ? 2 : 0);
    }

    /*
     * Si el jugador ha arribat als punts per guanyar, la partida passa a FINALITZADA i es guarda el guanyador.
     * Torna cert si la partida ha acabat; el missatge de victòria (INFO_GUANYADOR) el munta qui crida.
     */
    static boolean comprovarVictoria(Partida partida, Jugador jugador) {
        if (partida.getEstat() == EstatPartida.FINALITZADA) return true;
        int punts = calcularPunts(jugador);
        if (punts < partida.getPuntsPerGuanyar()) return false;
        partida.setEstat(EstatPartida.FINALITZADA);
        partida.setJugadorGuanyador(jugador);
        return true;
    }
}
