package pe.edu.pucp.tasfb2b.algoritmo;

import pe.edu.pucp.tasfb2b.model.*;

import java.util.*;

/**
 * Calcula la función objetivo del planificador GRASP de Tasf.B2B.
 *
 * f(S) = w1 * Σ penalidad(sᵢ) + w2 * Σ sobrecargaVuelo(e) + w3 * Σ sobrecargaAlmacen(v)
 *
 * Referencia metodológica: Granado et al. (2025) para enfoque multiobjetivo ponderado.
 */
public class EvaluadorFO {

    private final ParametrosGRASP params;

    public EvaluadorFO(ParametrosGRASP params) {
        this.params = params;
    }

    /**
     * Evalúa la función objetivo de una solución completa.
     *
     * @param solucion  Mapa idEnvio -> Ruta (null si no asignado)
     * @param envios    Lista completa de envíos (para acceder a los no asignados)
     * @param red       Red logística (para métricas de vuelos y almacenes)
     * @return          Valor de la FO (minimizar)
     */
    public double evaluar(Map<String, Ruta> solucion, List<Envio> envios, RedLogistica red) {
        double penalidades = calcularPenalidades(solucion, envios);
        double sobrecargaVuelos = calcularSobrecargaVuelos(red);
        double sobrecargaAlmacenes = calcularSobrecargaAlmacenes(red);

        return params.getW1() * penalidades
             + params.getW2() * sobrecargaVuelos
             + params.getW3() * sobrecargaAlmacenes;
    }

    /**
     * Penalidad por envío: 0 si a tiempo, big-M si sin ruta,
     * o coeficiente lineal por minutos de retraso.
     */
    public double calcularPenalidades(Map<String, Ruta> solucion, List<Envio> envios) {
        double total = 0.0;
        for (Envio envio : envios) {
            Ruta ruta = solucion.get(envio.getIdEnvio());
            if (ruta == null) {
                // Envío no asignado: penalidad big-M
                total += params.getPenalidadBigM();
            } else {
                // Penalidad proporcional al retraso
                total += ruta.minutosRetraso();
            }
        }
        return total;
    }

    /**
     * Sobrecarga de vuelos: suma de exceso relativo de capacidad por vuelo.
     */
    public double calcularSobrecargaVuelos(RedLogistica red) {
        double total = 0.0;
        for (Vuelo v : red.getTodosLosVuelos()) {
            if (!v.isCancelado()) {
                double exceso = Math.max(0, v.getOcupacion() - 1.0);
                total += exceso;
            }
        }
        return total;
    }

    /**
     * Sobrecarga de almacenes: suma de exceso relativo de capacidad por aeropuerto.
     */
    public double calcularSobrecargaAlmacenes(RedLogistica red) {
        double total = 0.0;
        for (Aeropuerto a : red.getAeropuertos().values()) {
            double exceso = Math.max(0, a.getOcupacionAlmacen() - 1.0);
            total += exceso;
        }
        return total;
    }

    /**
     * Evalúa el costo greedy de una ruta individual (usado en la construcción GRASP).
     * Costo = w1 * retraso_normalizado + w2 * saturacion_promedio + w3 * escalas_normalizadas
     */
    public double evaluarRutaGreedy(Ruta ruta, Envio envio) {
        double retrasoNorm = (double) ruta.minutosRetraso() / Math.max(1, envio.getPlazoMaxMinutos());
        double saturacion  = ruta.getSaturacionPromedio();
        double escalasNorm = (double) ruta.getNumEscalas() / params.getMaxEscalas();

        return params.getW1() * retrasoNorm
             + params.getW2() * saturacion
             + params.getW3() * escalasNorm;
    }

    /**
     * Estado de semáforo para un nivel de ocupación dado.
     */
    public String semaforoOcupacion(double ocupacion) {
        if (ocupacion <= params.getUmbralVerde()) return "VERDE";
        if (ocupacion <= params.getUmbralAmbar()) return "AMBAR";
        return "ROJO";
    }
}
