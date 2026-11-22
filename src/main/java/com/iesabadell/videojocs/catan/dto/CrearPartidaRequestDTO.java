package com.iesabadell.videojocs.catan.dto;

import java.util.List;

public record CrearPartidaRequestDTO(List<String> jugadors, Integer puntsPerGuanyar) {
}
