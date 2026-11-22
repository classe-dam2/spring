package com.iesabadell.videojocs.catan.dto;

import com.iesabadell.videojocs.catan.models.Partida;
import com.iesabadell.videojocs.catan.models.Jugador;
import com.iesabadell.videojocs.catan.models.Hexagon;
import com.iesabadell.videojocs.catan.models.enums.ColorJugador;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record PartidaResponseDTO(
		Integer id,
		Integer ronda,
		Integer puntsPerGuanyar,
		List<JugadorDTO> jugadors,
		List<HexagonTaulerDTO> hexagons) {
	
	
	public record JugadorDTO(
			Integer id,
			String nom,
			ColorJugador color,
			Integer ordreTorn) {
		
		}
	
	public static PartidaResponseDTO de (Partida p) {
		List<JugadorDTO> jugadors = p.getJugadors().stream()
				.map(j -> new JugadorDTO(
						j.getId(),
						j.getNom(),
						j.getColor(),
						j.getOrdreTorn()
						)).toList();
		
		Integer idLladre = p.getLladre() != null ? p.getLladre().getId() : null;
		
		List<HexagonTaulerDTO> hexagons  = p.getHexagons().stream()
				.map(h -> HexagonTaulerDTO.de(h, h.getId().equals(idLladre)
						)).toList();
		
		return new PartidaResponseDTO(p.getId(), p.getRonda(), p.getPuntsPerGuanyar(), jugadors, hexagons);
	}

}




