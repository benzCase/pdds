package pe.edu.pucp.tasfb2b.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa la ruta asignada a un envío: secuencia de vuelos
 * desde el aeropuerto origen hasta el destino.
 */
public class Ruta {

    private final Envio envio;
    private final List<Vuelo> vuelos;       // secuencia ordenada de vuelos
    private final int tiempoTotalMinutos;   // desde timestamp del envío hasta llegada final
    private final double costoGreedy;       // costo calculado durante construcción GRASP

    public Ruta(Envio envio, List<Vuelo> vuelos, int tiempoTotalMinutos, double costoGreedy) {
        this.envio = envio;
        this.vuelos = new ArrayList<>(vuelos);
        this.tiempoTotalMinutos = tiempoTotalMinutos;
        this.costoGreedy = costoGreedy;
    }

    /** ¿La ruta cumple el plazo comprometido? */
    public boolean cumplePlazo() {
        return tiempoTotalMinutos <= envio.getPlazoMaxMinutos();
    }

    /** Minutos de retraso sobre el plazo (0 si cumple). */
    public int minutosRetraso() {
        return Math.max(0, tiempoTotalMinutos - envio.getPlazoMaxMinutos());
    }

    /** Número de escalas (vuelos - 1). */
    public int getNumEscalas() {
        return Math.max(0, vuelos.size() - 1);
    }

    /** Saturación promedio de los vuelos de la ruta [0.0, 1.0+]. */
    public double getSaturacionPromedio() {
        if (vuelos.isEmpty()) return 0;
        return vuelos.stream()
            .mapToDouble(Vuelo::getOcupacion)
            .average()
            .orElse(0);
    }

    /** Minuto de llegada al destino final (minutos desde medianoche del día de operación). */
    public int getMinutosLlegadaFinal() {
        if (vuelos.isEmpty()) return 0;
        return vuelos.get(vuelos.size() - 1).getMinutosLlegada();
    }

    /** Crea una copia de esta ruta (para gestión de soluciones candidatas). */
    public Ruta copiar() {
        return new Ruta(envio, new ArrayList<>(vuelos), tiempoTotalMinutos, costoGreedy);
    }

    // --- Getters ---
    public Envio getEnvio() { return envio; }
    public List<Vuelo> getVuelos() { return Collections.unmodifiableList(vuelos); }
    public int getTiempoTotalMinutos() { return tiempoTotalMinutos; }
    public double getCostoGreedy() { return costoGreedy; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Ruta[%s", envio.getIdEnvio()));
        for (Vuelo v : vuelos) {
            sb.append(" -> ").append(v.getDestino().getCodigo());
        }
        sb.append(String.format(" | %dmin | %s]",
            tiempoTotalMinutos, cumplePlazo() ? "A_TIEMPO" : "RETRASADO"));
        return sb.toString();
    }
}
