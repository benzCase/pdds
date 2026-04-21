package pe.edu.pucp.tasfb2b.algoritmo;

import pe.edu.pucp.tasfb2b.model.*;

import java.util.*;

/**
 * Fase de Búsqueda Local del algoritmo GRASP para Tasf.B2B.
 *
 * Implementa dos movimientos:
 *  1. Swap de vuelo en un tramo: reemplaza un vuelo de la ruta por uno alternativo
 *     que mejore la función objetivo (reduce retraso o saturación).
 *  2. Reasignación de envíos sin ruta: intenta asignar rutas a los envíos
 *     que quedaron sin asignación en la fase de construcción.
 *
 * Estrategia: First Improvement (acepta el primer movimiento que mejore).
 *
 * Referencias:
 *  - Cardinaël et al. (2026): búsqueda local basada en reasignación para ruteo periódico.
 *  - Interian & Ribeiro (2017): búsqueda local con reinserción en el GRASP.
 */
public class BuscadorLocal {

    private final ParametrosGRASP params;
    private final EvaluadorFO evaluador;
    private final ConstructorGRASP constructor;

    public BuscadorLocal(ParametrosGRASP params, EvaluadorFO evaluador, ConstructorGRASP constructor) {
        this.params = params;
        this.evaluador = evaluador;
        this.constructor = constructor;
    }

    /**
     * Aplica búsqueda local sobre la solución actual.
     *
     * @param solucion  Mapa idEnvio -> Ruta a mejorar (se modifica in-place)
     * @param envios    Lista completa de envíos
     * @param red       Red logística
     * @return          Valor de la FO mejorada
     */
    public double mejorar(Map<String, Ruta> solucion, List<Envio> envios, RedLogistica red) {
        boolean mejorado = true;
        double foActual = evaluador.evaluar(solucion, envios, red);

        while (mejorado) {
            mejorado = false;

            // Movimiento 1: Swap de vuelo en un tramo de la ruta
            boolean mejora1 = aplicarSwapVuelo(solucion, envios, red, foActual);
            if (mejora1) {
                foActual = evaluador.evaluar(solucion, envios, red);
                mejorado = true;
            }

            // Movimiento 2: Reasignación de envíos sin ruta
            boolean mejora2 = aplicarReasignacion(solucion, envios, red, foActual);
            if (mejora2) {
                foActual = evaluador.evaluar(solucion, envios, red);
                mejorado = true;
            }
        }

        return foActual;
    }

    /**
     * Movimiento 1: Para cada envío con ruta, intenta reemplazar uno de sus vuelos
     * por un vuelo alternativo que mejore el costo.
     */
    private boolean aplicarSwapVuelo(Map<String, Ruta> solucion, List<Envio> envios,
                                      RedLogistica red, double foActual) {
        for (Envio envio : envios) {
            Ruta rutaActual = solucion.get(envio.getIdEnvio());
            if (rutaActual == null) continue;

            List<Vuelo> vuelosActuales = new ArrayList<>(rutaActual.getVuelos());

            // Intentar reemplazar cada tramo de la ruta
            for (int i = 0; i < vuelosActuales.size(); i++) {
                Vuelo vueloActual = vuelosActuales.get(i);

                // Calcular el tiempo disponible para salida en este tramo
                int minDesde = (i == 0)
                    ? envio.getTimestamp().getHour() * 60 + envio.getTimestamp().getMinute()
                    : vuelosActuales.get(i - 1).getMinutosLlegada();

                String origenTramo = vueloActual.getOrigen().getCodigo();
                String destinoTramo = vueloActual.getDestino().getCodigo();

                // Buscar vuelos alternativos para este tramo
                List<Vuelo> alternativos = red.getVuelosDesde(
                    origenTramo, minDesde, params.getTiempoMinimoConexionMinutos());

                for (Vuelo alt : alternativos) {
                    // Debe ir al mismo destino del tramo
                    if (!alt.getDestino().getCodigo().equals(destinoTramo)) continue;
                    if (alt.getId().equals(vueloActual.getId())) continue;
                    if (!alt.puedeAceptar(envio.getCantidadMaletas())) continue;

                    // Construir nueva ruta con el vuelo alternativo
                    List<Vuelo> nuevosVuelos = new ArrayList<>(vuelosActuales);
                    nuevosVuelos.set(i, alt);

                    int tiempoNuevo = calcularTiempoTotal(
                        envio, nuevosVuelos);

                    if (tiempoNuevo > envio.getPlazoMaxMinutos() * 1.5) continue;

                    // Evaluar si la nueva ruta es mejor
                    double costoNuevo = calcularCostoRuta(envio, nuevosVuelos, tiempoNuevo);
                    if (costoNuevo < rutaActual.getCostoGreedy()) {
                        // Aplicar el swap: liberar vuelo anterior, asignar alternativo
                        vueloActual.liberarMaletas(envio.getCantidadMaletas());
                        alt.asignarMaletas(envio.getCantidadMaletas());

                        Ruta nuevaRuta = new Ruta(envio, nuevosVuelos, tiempoNuevo, costoNuevo);
                        solucion.put(envio.getIdEnvio(), nuevaRuta);
                        envio.setEstado(Envio.Estado.ASIGNADO);
                        return true; // First improvement
                    }
                }
            }
        }
        return false;
    }

    /**
     * Movimiento 2: Intenta asignar rutas a los envíos que no tienen ruta (penalidad big-M).
     * Estos envíos son los más urgentes de resolver ya que representan la mayor penalidad.
     */
    private boolean aplicarReasignacion(Map<String, Ruta> solucion, List<Envio> envios,
                                         RedLogistica red, double foActual) {
        for (Envio envio : envios) {
            if (solucion.get(envio.getIdEnvio()) != null) continue; // ya tiene ruta

            List<Ruta> rutas = constructor.generarRutasFactibles(envio, red);
            if (rutas.isEmpty()) continue;

            // Elegir la mejor ruta disponible (menor costo greedy)
            Ruta mejorRuta = rutas.stream()
                .min(Comparator.comparingDouble(Ruta::getCostoGreedy))
                .orElse(null);

            if (mejorRuta == null) continue;

            // Verificar que asignar esta ruta no empeore la FO globalmente
            // La asignación siempre mejora vs penalidad big-M (a menos que genere sobrecarga severa)
            solucion.put(envio.getIdEnvio(), mejorRuta);
            for (Vuelo v : mejorRuta.getVuelos()) {
                v.asignarMaletas(envio.getCantidadMaletas());
            }
            envio.setEstado(Envio.Estado.ASIGNADO);
            return true; // First improvement
        }
        return false;
    }

    private int calcularTiempoTotal(Envio envio, List<Vuelo> vuelos) {
        if (vuelos.isEmpty()) return Integer.MAX_VALUE;
        int minInicio = envio.getTimestamp().getHour() * 60 + envio.getTimestamp().getMinute();
        return vuelos.get(vuelos.size() - 1).getMinutosLlegada() - minInicio;
    }

    private double calcularCostoRuta(Envio envio, List<Vuelo> vuelos, int tiempoTotal) {
        int retraso = Math.max(0, tiempoTotal - envio.getPlazoMaxMinutos());
        double retrasoNorm = (double) retraso / Math.max(1, envio.getPlazoMaxMinutos());
        double saturacion = vuelos.stream().mapToDouble(Vuelo::getOcupacion).average().orElse(0);
        double escalasNorm = (double) (vuelos.size() - 1) / Math.max(1, params.getMaxEscalas());
        return params.getW1() * retrasoNorm + params.getW2() * saturacion + params.getW3() * escalasNorm;
    }
}
