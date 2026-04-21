package pe.edu.pucp.tasfb2b.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un envío (solicitud de traslado de maletas) en Tasf.B2B.
 *
 * Formato del archivo de envíos:
 *   id_envío-aaaammdd-hh-mm-dest-###-IdClien
 *   00000001-20250102-01-38-SEQM-006-0007729
 */
public class Envio {

    public enum Estado { PENDIENTE, ASIGNADO, EN_TRANSITO, ENTREGADO, RETRASADO, NO_ASIGNADO }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final String idEnvio;
    private final LocalDateTime timestamp;
    private final Aeropuerto origen;        // aeropuerto donde se entrega a Tasf.B2B
    private final Aeropuerto destino;
    private final int cantidadMaletas;
    private final String idCliente;

    /** Plazo máximo en minutos: 1440 min (24h) mismo continente, 2880 min (48h) distinto */
    private final int plazoMaxMinutos;

    private Estado estado;

    /**
     * Parsea una línea del archivo de envíos.
     * Formato: 00000001-20250102-01-38-SEQM-006-0007729
     */
    public static Envio parsear(String linea, java.util.Map<String, Aeropuerto> aeropuertos,
                                 Aeropuerto aeropuertoOrigen) {
        String[] p = linea.trim().split("-");
        if (p.length < 7) throw new IllegalArgumentException("Línea de envío inválida: " + linea);

        String id = p[0];
        String fechaHora = p[1] + p[2] + p[3];   // aaaammddhhm
        LocalDateTime ts = LocalDateTime.parse(fechaHora, FMT);
        String codDest = p[4];
        int maletas = Integer.parseInt(p[5]);
        String idCli = p[6];

        Aeropuerto dest = aeropuertos.get(codDest);
        if (dest == null) throw new IllegalArgumentException("Destino desconocido: " + codDest);

        boolean mismoContinente = aeropuertoOrigen.getContinente() == dest.getContinente();
        int plazo = mismoContinente ? 1440 : 2880;

        return new Envio(id, ts, aeropuertoOrigen, dest, maletas, idCli, plazo);
    }

    public Envio(String idEnvio, LocalDateTime timestamp, Aeropuerto origen,
                 Aeropuerto destino, int cantidadMaletas, String idCliente, int plazoMaxMinutos) {
        this.idEnvio = idEnvio;
        this.timestamp = timestamp;
        this.origen = origen;
        this.destino = destino;
        this.cantidadMaletas = cantidadMaletas;
        this.idCliente = idCliente;
        this.plazoMaxMinutos = plazoMaxMinutos;
        this.estado = Estado.PENDIENTE;
    }

    /** ¿El envío cruza continentes? */
    public boolean esContinenteDistinto() {
        return origen.getContinente() != destino.getContinente();
    }

    // --- Getters ---
    public String getIdEnvio() { return idEnvio; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Aeropuerto getOrigen() { return origen; }
    public Aeropuerto getDestino() { return destino; }
    public int getCantidadMaletas() { return cantidadMaletas; }
    public String getIdCliente() { return idCliente; }
    public int getPlazoMaxMinutos() { return plazoMaxMinutos; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    @Override
    public String toString() {
        return String.format("Envio[%s %s->%s %dmal plazo=%dmin]",
            idEnvio, origen.getCodigo(), destino.getCodigo(),
            cantidadMaletas, plazoMaxMinutos);
    }
}
