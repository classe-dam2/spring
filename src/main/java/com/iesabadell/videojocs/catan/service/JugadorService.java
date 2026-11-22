package com.iesabadell.videojocs.catan.service;

import com.iesabadell.videojocs.catan.exception.ReglaJocException;

import com.iesabadell.videojocs.catan.models.*;
import com.iesabadell.videojocs.catan.models.abstracts.Carretera;
import com.iesabadell.videojocs.catan.models.abstracts.Ciutat;
import com.iesabadell.videojocs.catan.models.abstracts.Poblat;
import com.iesabadell.videojocs.catan.models.enums.EstatPartida;
import com.iesabadell.videojocs.catan.models.enums.TipusRecurs;

import java.util.List;
import java.util.Map;

import com.iesabadell.videojocs.catan.repository.ArestaRepository;
import com.iesabadell.videojocs.catan.repository.JugadorRepository;
import com.iesabadell.videojocs.catan.repository.PartidaRepository;
import com.iesabadell.videojocs.catan.repository.VertexRepository;
import com.iesabadell.videojocs.catan.util.Missatges;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JugadorService {
    @Autowired
    private JugadorRepository jugadorRepository;
    
    @Transactional
    public ResponseEntity<?> recursos(Integer idJugador) {

        Jugador jugador = jugadorRepository.findById(idJugador)
                .orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
                        String.format(Missatges.JUGADOR_NO_EXISTEIX, idJugador)));

        List<Map<String, Object>> recursos = jugador.getRecursos().stream()
                .map(r -> Map.<String, Object>of(
                        "tipus", r.getTipus(),
                        "quantitat", r.getQuantitat()
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "id", jugador.getId(),
                "nom", jugador.getNom(),
                "recursos", recursos
        ));
    }
    
    @Autowired
    private PartidaRepository partida;

    @Transactional
    public ResponseEntity<String> passarTorn(Integer idJugador) {

        Jugador j = jugadorRepository.findById(idJugador).orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
                String.format(Missatges.JUGADOR_NO_EXISTEIX, idJugador)));


        Partida p = j.getPartida();
        if (p.getEstat() == EstatPartida.FINALITZADA) {
            throw ReglesJoc.conflicte(Missatges.PARTIDA_NO_EN_CURS);
        }

        if (!ReglesJoc.esElSeuTorn(j, p)) {
            throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.NO_ES_EL_SEU_TORN);
        }

        List<Jugador> jugadors = p.getJugadors();
        int indexActual = jugadors.indexOf(j);
        if (indexActual < 0) {
            throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.NO_ES_EL_SEU_TORN);
        }

        if (p.getEstat() == EstatPartida.PREPARACIO) {
            int ronda = p.getRonda();
            if (ronda < 1 || ronda > 2
                    || ReglesJoc.comptarPoblats(j) != ronda
                    || ReglesJoc.comptarCarreteres(j) != ronda) {
                throw ReglesJoc.conflicte("Cal col·locar un poblat i una carretera abans de passar torn.");
            }
            List<Jugador> ordre = new java.util.ArrayList<>(jugadors);
            ordre.sort(java.util.Comparator.comparingInt(Jugador::getOrdreTorn));
            int index = ordre.indexOf(j);
            int seguent = ronda == 1 ? index + 1 : index - 1;
            if (seguent >= 0 && seguent < ordre.size()) {
                p.setJugadorActiu(ordre.get(seguent));
            } else if (ronda == 1) {
                p.setRonda(2);
                p.setJugadorActiu(ordre.get(ordre.size() - 1));
            } else {
                p.setEstat(EstatPartida.EN_CURS);
                p.setRonda(1);
                p.setJugadorActiu(ordre.get(0));
            }
            p.setDausTirats(false);
            return ResponseEntity.ok(String.format(Missatges.INFO_TORN,
                    p.getJugadorActiu().getNom(), p.getRonda()));
        }
        ReglesJoc.validarAccio(p, j, false);
        List<Jugador> ordre = new java.util.ArrayList<>(jugadors);
        ordre.sort(java.util.Comparator.comparingInt(Jugador::getOrdreTorn));
        int index = ordre.indexOf(j);
        Jugador seguent = ordre.get((index + 1) % ordre.size());
        p.setJugadorActiu(seguent);
        p.setDausTirats(false);
        if (seguent.getOrdreTorn() <= j.getOrdreTorn()) {
            p.setRonda(p.getRonda() + 1);
        }
        return ResponseEntity.ok(String.format(Missatges.INFO_TORN, seguent.getNom(), p.getRonda()));
    }
    
    @Autowired
    private ArestaRepository arestaRepository;

    @Autowired
    private VertexRepository vertexRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public ResponseEntity<String> construir(Integer idJugador, String tipo, Integer posicio) {

    	Jugador j = jugadorRepository.findById(idJugador)
    			.orElseThrow(() -> new ReglaJocException(HttpStatus.NOT_FOUND,
    					String.format(Missatges.JUGADOR_NO_EXISTEIX, idJugador)));

    	Partida p = j.getPartida();

    	boolean preparacio = p.getEstat() == EstatPartida.PREPARACIO;
    	if (!preparacio && p.getEstat() != EstatPartida.EN_CURS) {
    		throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.PARTIDA_NO_EN_CURS);
    	}
    	if (!ReglesJoc.esElSeuTorn(j, p)) {
    		throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.NO_ES_EL_SEU_TORN);
    	}
    	if (preparacio) {
            if (p.getRonda() < 1 || p.getRonda() > 2) {
                throw ReglesJoc.conflicte("Ronda de preparació no vàlida.");
            }
        } else {
            if (!p.isDausTirats()) {
                throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.DAUS_NO_TIRATS);
            }
            if (p.isLladrePendent()) {
                throw new ReglaJocException(HttpStatus.CONFLICT, Missatges.LLADRE_PENDENT);
            }
        }

    	if (tipo == null) {
    		throw new ReglaJocException(HttpStatus.BAD_REQUEST, "Tipus de construcció no vàlid.");
    	}

    	switch (tipo.toUpperCase()) {

    		case "CARRETERA":

    			Aresta aresta = arestaRepository.findById(posicio)
                    	.orElseThrow(() -> ReglesJoc.conflicte(Missatges.ARESTA_NO_TROBADA));

    			if (!aresta.getPartida().getId().equals(p.getId())) {
    				throw ReglesJoc.conflicte("La posició pertany a una altra partida.");
    			}

    			if (aresta.getCarretera() != null) {
                    throw ReglesJoc.conflicte(Missatges.ARESTA_OCUPADA);
    			}

    			if (preparacio) {
                    if (ReglesJoc.comptarPoblats(j) != p.getRonda()
                            || ReglesJoc.comptarCarreteres(j) != p.getRonda() - 1) {
                        throw ReglesJoc.conflicte("Cal col·locar un poblat abans de la carretera.");
                    }
                    Poblat ultim = ReglesJoc.ultimPoblat(j);
                    if (ultim == null || (!Vertex.mateix(ultim.getVertex(), aresta.getVertexA())
                            && !Vertex.mateix(ultim.getVertex(), aresta.getVertexB()))) {
                        throw ReglesJoc.conflicte("La carretera ha de tocar el poblat que acabes de col·locar.");
                    }
                } else if (!carreteraConnectada(aresta, j, p)) {
    				throw ReglesJoc.conflicte(Missatges.CARRETERA_NO_CONNECTADA);
    			}
    			if (ReglesJoc.comptarCarreteres(j) >= 15) {
    				throw ReglesJoc.conflicte(String.format(Missatges.CARRETERA_LIMIT, 15));
    			}
    			if (!preparacio && (ReglesJoc.quantitat(j, TipusRecurs.FUSTA) < 1
    					|| ReglesJoc.quantitat(j, TipusRecurs.MAO) < 1)) {
    				throw ReglesJoc.conflicte(Missatges.CARRETERA_SENSE_RECURSOS);
    			}
    			if (!preparacio) {
                    ReglesJoc.descomptar(j, TipusRecurs.FUSTA, 1);
                    ReglesJoc.descomptar(j, TipusRecurs.MAO, 1);
                }
    			Carretera carretera = new Carretera();
    			carretera.setJugador(j);
    			carretera.setAresta(aresta);
    			entityManager.persist(carretera);
    			aresta.setCarretera(carretera);
    			j.getConstruccions().add(carretera);

    			return ResponseEntity.ok(String.format(Missatges.INFO_CARRETERA, aresta.getId()));


    		case "POBLAT":
                if (preparacio && (ReglesJoc.comptarPoblats(j) != p.getRonda() - 1
                        || ReglesJoc.comptarCarreteres(j) != p.getRonda() - 1)) {
                    throw ReglesJoc.conflicte(Missatges.POBLAT_JA_COL_LOCAT);
                }
                if (ReglesJoc.comptarPoblats(j) >= 5) {
                    throw ReglesJoc.conflicte(String.format(Missatges.POBLAT_LIMIT, 5));
                }

    			Vertex vertex = vertexRepository.findById(posicio)
    			.orElseThrow(() -> ReglesJoc.conflicte(Missatges.VERTEX_NO_TROBAT));

    			if (!vertex.getPartida().getId().equals(p.getId())) {
    				throw ReglesJoc.conflicte("La posició pertany a una altra partida.");
    			}

    			if (vertex.getAssentament() != null) {
    				throw ReglesJoc.conflicte(Missatges.VERTEX_OCUPAT);
    			}
    			if (massaProp(vertex, p)) {
    				throw ReglesJoc.conflicte(Missatges.POBLAT_MASSA_PROP);
    			}
                if (!preparacio && !poblatConnectat(vertex, j, p)) {
                    throw ReglesJoc.conflicte(Missatges.POBLAT_NO_CONNECTAT);
                }
    			if (!preparacio && (ReglesJoc.quantitat(j, TipusRecurs.FUSTA) < 1
    					|| ReglesJoc.quantitat(j, TipusRecurs.MAO) < 1
    					|| ReglesJoc.quantitat(j, TipusRecurs.LLANA) < 1
    					|| ReglesJoc.quantitat(j, TipusRecurs.BLAT) < 1)) {
    				throw ReglesJoc.conflicte(Missatges.POBLAT_SENSE_RECURSOS);
    			}
    			if (!preparacio) {
                    ReglesJoc.descomptar(j, TipusRecurs.FUSTA, 1);
                    ReglesJoc.descomptar(j, TipusRecurs.MAO, 1);
                    ReglesJoc.descomptar(j, TipusRecurs.LLANA, 1);
                    ReglesJoc.descomptar(j, TipusRecurs.BLAT, 1);
                }
    			Poblat poblat = new Poblat();
    			poblat.setJugador(j);
    			poblat.setVertex(vertex);
    			entityManager.persist(poblat);
    			vertex.setAssentament(poblat);
    			j.getConstruccions().add(poblat);
                if (preparacio && ReglesJoc.comptarPoblats(j) == 2) {
                    for (Hexagon h : p.getHexagons()) {
                        if (h.getHexagonVertexs().stream().anyMatch(hv -> Vertex.mateix(hv.getVertex(), vertex))) {
                            TipusRecurs recurs = ReglesJoc.recursDe(h.getTerreny());
                            if (recurs != null) ReglesJoc.afegir(j, recurs, 1);
                        }
                    }
                }
                ReglesJoc.comprovarVictoria(p, j);
    			return ResponseEntity.ok(String.format(Missatges.INFO_POBLAT, vertex.getId()));


    		case "CIUTAT":
                if (preparacio) {
                    throw ReglesJoc.conflicte(Missatges.NOMES_EN_CURS);
                }

	            Vertex vertexCiutat = vertexRepository.findById(posicio)
	                    .orElseThrow(() -> ReglesJoc.conflicte(Missatges.VERTEX_NO_TROBAT));
	
	            if (!vertexCiutat.getPartida().getId().equals(p.getId())) {
	                throw ReglesJoc.conflicte("La posició pertany a una altra partida.");
	            }
	
	            if (!(vertexCiutat.getAssentament() instanceof Poblat)
	                    || !vertexCiutat.getAssentament().getJugador().getId().equals(j.getId())) {
	                throw ReglesJoc.conflicte(Missatges.CIUTAT_SENSE_POBLAT);
	            }
	
	            if (ReglesJoc.comptarCiutats(j) >= 4) {
	                throw ReglesJoc.conflicte(String.format(Missatges.CIUTAT_LIMIT, 4));
	            }
	
	            if (ReglesJoc.quantitat(j, TipusRecurs.BLAT) < 2
	                    || ReglesJoc.quantitat(j, TipusRecurs.MINERAL) < 3) {
	                throw ReglesJoc.conflicte(Missatges.CIUTAT_SENSE_RECURSOS);
	            }
	
	            ReglesJoc.descomptar(j, TipusRecurs.BLAT, 2);
	            ReglesJoc.descomptar(j, TipusRecurs.MINERAL, 3);
	
	            Poblat poblatAntic = (Poblat) vertexCiutat.getAssentament();
	            Ciutat ciutat = new Ciutat();
	            ciutat.setJugador(j);
	            ciutat.setVertex(vertexCiutat);

	            vertexCiutat.setAssentament(null);
	            entityManager.remove(poblatAntic);
	            entityManager.flush();
	            j.getConstruccions().remove(poblatAntic);
	            entityManager.persist(ciutat);
	            vertexCiutat.setAssentament(ciutat);
	            j.getConstruccions().add(ciutat);
                ReglesJoc.comprovarVictoria(p, j);
	            return ResponseEntity.ok(String.format(Missatges.INFO_CIUTAT, vertexCiutat.getId()));
	
	
	        default:
	            throw new ReglaJocException(HttpStatus.BAD_REQUEST, 
	            		"Tipus de construcció no vàlid.");
	    }
    }
    
    private boolean massaProp(Vertex vertex, Partida partida) {

        for (Aresta a : partida.getArestas()) {

            Vertex vei = null;

            if (Vertex.mateix(a.getVertexA(), vertex)) {
                vei = a.getVertexB();
            } else if (Vertex.mateix(a.getVertexB(), vertex)) {
                vei = a.getVertexA();
            }

            if (vei != null && vei.getAssentament() != null) {
                return true;
            }
        }

        return false;
    }
    
    
    private boolean poblatConnectat(Vertex vertex, Jugador jugador, Partida partida) {

        for (Aresta a : partida.getArestas()) {

            if ((Vertex.mateix(a.getVertexA(), vertex)
                    || Vertex.mateix(a.getVertexB(), vertex))
                    && a.getCarretera() != null
                    && a.getCarretera().getJugador().getId().equals(jugador.getId())) {

                return true;
            }
        }

        return false;
    }
    

	private boolean carreteraConnectada(Aresta aresta, Jugador jugador, Partida partida) {
	
	    Vertex a = aresta.getVertexA();
	    Vertex b = aresta.getVertexB();
	
	    if (vertexConnectat(a, jugador, partida, aresta)) {
	        return true;
	    }
	
	    if (vertexConnectat(b, jugador, partida, aresta)) {
	        return true;
	    }
	
	    return false;
	}
	
	private boolean vertexConnectat(Vertex vertex, Jugador jugador, Partida partida, Aresta arestaActual) {
	    if (vertex.getAssentament() != null
	            && vertex.getAssentament().getJugador().getId().equals(jugador.getId())) {
	        return true;
	    }
	
	    // Comprovar si hi ha una altra carretera nostra
	    for (Aresta a : partida.getArestas()) {
	
	        if (a.getId().equals(arestaActual.getId())) {
	            continue;
	        }
	
	        if ((Vertex.mateix(a.getVertexA(), vertex)
	                || Vertex.mateix(a.getVertexB(), vertex))
	                && a.getCarretera() != null
	                && a.getCarretera().getJugador().getId().equals(jugador.getId())) {
	
	            return true;
	        }
	    }
	
	    return false;
	}


}





