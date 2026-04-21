package pe.edu.pucp.tasfb2b.model;

/**
 * Representa un aeropuerto en la red logística de Tasf.B2B.
 * Continente: AMERICA_SUR, EUROPA, ASIA
 */
public class Aeropuerto {

    public enum Continente { AMERICA_SUR, EUROPA, ASIA }

    private final String codigo;       // ej. SKBO
    private final String ciudad;       // ej. Bogota
    private final String pais;
    private final Continente continente;
    private final int gmtOffset;       // horas respecto a UTC
    private final int capacidadAlmacen; // máximo de maletas en depósito
    private int maletasEnAlmacen;      // estado actual

    public Aeropuerto(String codigo, String ciudad, String pais,
                      Continente continente, int gmtOffset, int capacidadAlmacen) {
        this.codigo = codigo;
        this.ciudad = ciudad;
        this.pais = pais;
        this.continente = continente;
        this.gmtOffset = gmtOffset;
        this.capacidadAlmacen = capacidadAlmacen;
        this.maletasEnAlmacen = 0;
    }

    /** Intenta agregar maletas al almacén. Retorna false si supera la capacidad. */
    public boolean agregarMaletas(int cantidad) {
        if (maletasEnAlmacen + cantidad > capacidadAlmacen) return false;
        maletasEnAlmacen += cantidad;
        return true;
    }

    public void retirarMaletas(int cantidad) {
        maletasEnAlmacen = Math.max(0, maletasEnAlmacen - cantidad);
    }

    /** Porcentaje de ocupación del almacén [0.0, 1.0+]. */
    public double getOcupacionAlmacen() {
        return (double) maletasEnAlmacen / capacidadAlmacen;
    }

    public String getCodigo() { return codigo; }
    public String getCiudad() { return ciudad; }
    public Continente getContinente() { return continente; }
    public int getCapacidadAlmacen() { return capacidadAlmacen; }
    public int getMaletasEnAlmacen() { return maletasEnAlmacen; }
    public void setMaletasEnAlmacen(int n) { this.maletasEnAlmacen = n; }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", codigo, ciudad, continente);
    }
}
