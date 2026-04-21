package pe.edu.pucp.tasfb2b.algoritmo;

/**
 * Parámetros de configuración del algoritmo GRASP para Tasf.B2B.
 * Todos los parámetros son configurables para experimentación numérica.
 */
public class ParametrosGRASP {

    public enum Escenario { TIEMPO_REAL, SIMULACION_PERIODO, COLAPSO }

    // ---- Parámetros del mecanismo GRASP ----

    /** Factor de restricción de la RCL [0.0 = greedy puro, 1.0 = aleatorio puro]. */
    private double alpha = 0.25;

    /** Número máximo de iteraciones del ciclo GRASP externo. */
    private int maxIteraciones = 200;

    /** Máximo de iteraciones consecutivas sin mejora para parada anticipada. */
    private int maxSinMejora = 40;

    // ---- Parámetros del dominio del problema ----

    /** Máximo número de vuelos (escalas) por ruta. */
    private int maxEscalas = 2;

    /** Tiempo máximo de espera en aeropuerto intermedio (minutos). */
    private int tiempoMaxEsperaMinutos = 360; // 6 horas

    /** Tiempo mínimo de conexión entre vuelos en el mismo aeropuerto (minutos). */
    private int tiempoMinimoConexionMinutos = 30;

    /** Horizonte de planificación en horas. */
    private int horizonteHoras = 24;

    // ---- Pesos de la función objetivo ----

    /** Peso de penalidad por retraso de envíos. */
    private double w1 = 0.60;

    /** Peso de sobrecarga de vuelos. */
    private double w2 = 0.25;

    /** Peso de sobrecarga de almacenes. */
    private double w3 = 0.15;

    /** Penalidad big-M para envíos sin ruta asignada (en minutos equivalentes). */
    private double penalidadBigM = 100_000.0;

    // ---- Parámetros de semáforo (visualización) ----

    /** Umbral para semáforo verde (≤ umbralVerde = verde). */
    private double umbralVerde = 0.70;

    /** Umbral para semáforo ámbar (≤ umbralAmbar = ámbar, > umbralAmbar = rojo). */
    private double umbralAmbar = 0.90;

    // ---- Escenario de operación ----
    private Escenario escenario = Escenario.SIMULACION_PERIODO;

    // ---- Constructor con valores por defecto ----
    public ParametrosGRASP() {}

    /** Constructor para experimentación numérica variando alpha y maxIteraciones. */
    public ParametrosGRASP(double alpha, int maxIteraciones, int maxSinMejora, Escenario escenario) {
        this.alpha = alpha;
        this.maxIteraciones = maxIteraciones;
        this.maxSinMejora = maxSinMejora;
        this.escenario = escenario;
    }

    /** Valida que los parámetros sean consistentes. */
    public void validar() {
        if (alpha < 0.0 || alpha > 1.0) throw new IllegalArgumentException("alpha debe estar en [0,1]");
        if (maxIteraciones <= 0) throw new IllegalArgumentException("maxIteraciones debe ser > 0");
        if (maxSinMejora <= 0) throw new IllegalArgumentException("maxSinMejora debe ser > 0");
        if (w1 + w2 + w3 < 0.99) throw new IllegalArgumentException("Los pesos w1+w2+w3 deben sumar 1.0");
        if (umbralVerde >= umbralAmbar) throw new IllegalArgumentException("umbralVerde debe ser < umbralAmbar");
    }

    // ---- Getters y Setters ----
    public double getAlpha() { return alpha; }
    public void setAlpha(double alpha) { this.alpha = alpha; }
    public int getMaxIteraciones() { return maxIteraciones; }
    public void setMaxIteraciones(int maxIteraciones) { this.maxIteraciones = maxIteraciones; }
    public int getMaxSinMejora() { return maxSinMejora; }
    public void setMaxSinMejora(int maxSinMejora) { this.maxSinMejora = maxSinMejora; }
    public int getMaxEscalas() { return maxEscalas; }
    public void setMaxEscalas(int maxEscalas) { this.maxEscalas = maxEscalas; }
    public int getTiempoMaxEsperaMinutos() { return tiempoMaxEsperaMinutos; }
    public void setTiempoMaxEsperaMinutos(int t) { this.tiempoMaxEsperaMinutos = t; }
    public int getTiempoMinimoConexionMinutos() { return tiempoMinimoConexionMinutos; }
    public void setTiempoMinimoConexionMinutos(int t) { this.tiempoMinimoConexionMinutos = t; }
    public int getHorizonteHoras() { return horizonteHoras; }
    public void setHorizonteHoras(int h) { this.horizonteHoras = h; }
    public double getW1() { return w1; }
    public double getW2() { return w2; }
    public double getW3() { return w3; }
    public void setPesos(double w1, double w2, double w3) {
        this.w1 = w1; this.w2 = w2; this.w3 = w3;
    }
    public double getPenalidadBigM() { return penalidadBigM; }
    public void setPenalidadBigM(double m) { this.penalidadBigM = m; }
    public double getUmbralVerde() { return umbralVerde; }
    public void setUmbralVerde(double u) { this.umbralVerde = u; }
    public double getUmbralAmbar() { return umbralAmbar; }
    public void setUmbralAmbar(double u) { this.umbralAmbar = u; }
    public Escenario getEscenario() { return escenario; }
    public void setEscenario(Escenario escenario) { this.escenario = escenario; }

    @Override
    public String toString() {
        return String.format("ParametrosGRASP[α=%.2f iter=%d sinMejora=%d escalas=%d escenario=%s]",
            alpha, maxIteraciones, maxSinMejora, maxEscalas, escenario);
    }
}
