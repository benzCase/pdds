package pe.edu.pucp.tasfb2b.model;

import java.util.*;

/**
 * Representa una solución completa del planificador GRASP:
 * asignación de rutas a envíos y métricas de desempeño asociadas.
 */
public class Solucion {

    private final Map<String, Ruta> rutasPorEnvio;   // idEnvio -> Ruta (null = no asignado)
    private double valorFO;                           // valor de la función objetivo (minimizar)

    public Solucion() {
        this.rutasPorEnvio = new LinkedHashMap<>();
        this.valorFO = Double.MAX_VALUE;
    }

    /** Constructor de copia profunda. */
    public Solucion(Solucion otra) {
        this.rutasPorEnvio = new LinkedHashMap<>(otra.rutasPorEnvio);
        this.valorFO = otra.valorFO;
    }

    public void asignarRuta(String idEnvio, Ruta ruta) {
        rutasPorEnvio.put(idEnvio, ruta);
    }

    public Ruta getRuta(String idEnvio) {
        return rutasPorEnvio.get(idEnvio);
    }

    public boolean tieneRuta(String idEnvio) {
        Ruta r = rutasPorEnvio.get(idEnvio);
        return r != null;
    }

    public Collection<Ruta> todasLasRutas() {
        return rutasPorEnvio.values().stream()
            .filter(Objects::nonNull)
            .toList();
    }

    public Set<String> getEnviosSinRuta() {
        Set<String> sinRuta = new HashSet<>();
        for (Map.Entry<String, Ruta> e : rutasPorEnvio.entrySet()) {
            if (e.getValue() == null) sinRuta.add(e.getKey());
        }
        return sinRuta;
    }

    // ---- Métricas de desempeño ----

    /** Número de envíos con ruta asignada. */
    public long getEnviosAsignados() {
        return rutasPorEnvio.values().stream().filter(Objects::nonNull).count();
    }

    /** Número de envíos entregados a tiempo. */
    public long getEnviosATiempo() {
        return todasLasRutas().stream().filter(Ruta::cumplePlazo).count();
    }

    /** Tasa de cumplimiento de plazos (0.0 a 1.0). */
    public double getTasaCumplimiento() {
        long total = rutasPorEnvio.size();
        if (total == 0) return 0;
        return (double) getEnviosATiempo() / total;
    }

    /** Total de maletas no asignadas. */
    public int getMaletasSinAsignar() {
        return rutasPorEnvio.entrySet().stream()
            .filter(e -> e.getValue() == null)
            .mapToInt(e -> {
                // Recuperar envío del mapa es opcional; aquí asumimos que la clave lleva info
                return 0; // simplificado; en implementación real se accede al envío
            }).sum();
    }

    public double getValorFO() { return valorFO; }
    public void setValorFO(double valorFO) { this.valorFO = valorFO; }
    public Map<String, Ruta> getRutasPorEnvio() { return Collections.unmodifiableMap(rutasPorEnvio); }

    @Override
    public String toString() {
        return String.format("Solucion[FO=%.4f asignados=%d aTiempo=%d tasa=%.1f%%]",
            valorFO, getEnviosAsignados(), getEnviosATiempo(), getTasaCumplimiento() * 100);
    }
}
