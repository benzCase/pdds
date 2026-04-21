package pe.edu.pucp.tasfb2b.model;

/**
 * Representa un vuelo en el plan de operaciones de Tasf.B2B.
 * Formato de archivo: ORIG-DEST-HH:MM-HH:MM-CAPACIDAD
 * Ej: SKBO-SEQM-03:34-04:21-0300
 */
public class Vuelo {

    private final String id;               // identificador único (ej. SKBO-SEQM-0334)
    private final Aeropuerto origen;
    private final Aeropuerto destino;
    private final int minutosSalida;       // minutos desde medianoche
    private final int minutosLlegada;      // minutos desde medianoche
    private final int capacidadMaxima;     // maletas
    private int capacidadUsada;           // maletas ya asignadas en esta instancia
    private boolean cancelado;

    public Vuelo(String id, Aeropuerto origen, Aeropuerto destino,
                 int minutosSalida, int minutosLlegada, int capacidadMaxima) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.minutosSalida = minutosSalida;
        this.minutosLlegada = minutosLlegada;
        this.capacidadMaxima = capacidadMaxima;
        this.capacidadUsada = 0;
        this.cancelado = false;
    }

    /**
     * Parsea una línea del archivo de plan de vuelos.
     * Formato: ORIG-DEST-HH:MM-HH:MM-CAPACIDAD
     */
    public static Vuelo parsear(String linea, java.util.Map<String, Aeropuerto> aeropuertos) {
        String[] partes = linea.trim().split("-");
        if (partes.length < 5) throw new IllegalArgumentException("Línea inválida: " + linea);

        String codigoOrig = partes[0];
        String codDest = partes[1];
        String[] salida = partes[2].split(":");
        String[] llegada = partes[3].split(":");
        int capacidad = Integer.parseInt(partes[4]);

        Aeropuerto orig = aeropuertos.get(codigoOrig);
        Aeropuerto dest = aeropuertos.get(codDest);
        if (orig == null || dest == null)
            throw new IllegalArgumentException("Aeropuerto no encontrado: " + codigoOrig + " o " + codDest);

        int minSalida = Integer.parseInt(salida[0]) * 60 + Integer.parseInt(salida[1]);
        int minLlegada = Integer.parseInt(llegada[0]) * 60 + Integer.parseInt(llegada[1]);
        // Si llegada < salida, el vuelo cruza medianoche
        if (minLlegada < minSalida) minLlegada += 24 * 60;

        String idVuelo = codigoOrig + "-" + codDest + "-" + partes[2].replace(":", "");
        return new Vuelo(idVuelo, orig, dest, minSalida, minLlegada, capacidad);
    }

    /** Capacidad disponible restante. */
    public int getCapacidadResidual() {
        return capacidadMaxima - capacidadUsada;
    }

    /** Porcentaje de ocupación [0.0, 1.0+]. */
    public double getOcupacion() {
        return (double) capacidadUsada / capacidadMaxima;
    }

    public boolean puedeAceptar(int maletas) {
        return !cancelado && (capacidadUsada + maletas <= capacidadMaxima);
    }

    public void asignarMaletas(int maletas) { capacidadUsada += maletas; }
    public void liberarMaletas(int maletas) { capacidadUsada = Math.max(0, capacidadUsada - maletas); }

    /** ¿El vuelo implica cambio de continente? */
    public boolean esContinenteDistinto() {
        return origen.getContinente() != destino.getContinente();
    }

    // --- Getters ---
    public String getId() { return id; }
    public Aeropuerto getOrigen() { return origen; }
    public Aeropuerto getDestino() { return destino; }
    public int getMinutosSalida() { return minutosSalida; }
    public int getMinutosLlegada() { return minutosLlegada; }
    public int getCapacidadMaxima() { return capacidadMaxima; }
    public int getCapacidadUsada() { return capacidadUsada; }
    public void setCapacidadUsada(int n) { this.capacidadUsada = n; }
    public boolean isCancelado() { return cancelado; }
    public void setCancelado(boolean cancelado) { this.cancelado = cancelado; }

    @Override
    public String toString() {
        return String.format("Vuelo[%s %s->%s sal=%dmin cap=%d/%d]",
            id, origen.getCodigo(), destino.getCodigo(),
            minutosSalida, capacidadUsada, capacidadMaxima);
    }
}
