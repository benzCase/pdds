package pe.edu.pucp.tasfb2b.algoritmo;

import pe.edu.pucp.tasfb2b.model.*;

import java.util.*;

/**
 * Orquestador principal del algoritmo GRASP para Tasf.B2B.
 *
 * Implementa el ciclo GRASP completo:
 *   PARA iter = 1..maxIteraciones:
 *     1. Construcción greedy aleatorizada (α-parametrizada)
 *     2. Búsqueda local (First Improvement, dos movimientos)
 *     3. Actualización de la mejor solución
 *     4. Verificación de parada anticipada (maxSinMejora)
 *
 * Soporta los tres escenarios del sistema:
 *   - SIMULACION_PERIODO: ejecución completa sobre todos los envíos del periodo
 *   - TIEMPO_REAL: ejecución incremental con replanificación parcial
 *   - COLAPSO: maximiza el tiempo hasta saturación de la red
 *
 * Referencias:
 *  - Interian & Ribeiro (2017): ciclo GRASP con restarts y path-relinking.
 *  - Granado et al. (2025): adaptación GRASP multiobjetivo para ruteo de flota.
 *  - Tanash & As'Ad (2025): VRPTW heterogéneo con prioridad de clientes.
 *  - Cardinaël et al. (2026): GRASP memético para ruteo periódico de técnicos.
 */
public class PlanificadorGRASP {

    private final ParametrosGRASP params;
    private final EvaluadorFO evaluador;
    private final ConstructorGRASP constructor;
    private final BuscadorLocal buscador;
    private final Random random;

    // Métricas de la última ejecución
    private int iteracionesEjecutadas;
    private long tiempoEjecucionMs;
    private List<Double> historialFO;

    public PlanificadorGRASP(ParametrosGRASP params) {
        this.params = params;
        this.evaluador = new EvaluadorFO(params);
        this.random = new Random(42); // semilla fija para reproducibilidad en experimentación
        this.constructor = new ConstructorGRASP(params, evaluador, random);
        this.buscador = new BuscadorLocal(params, evaluador, constructor);
        this.historialFO = new ArrayList<>();
    }

    /**
     * Constructor con semilla personalizada (útil para experimentación numérica).
     */
    public PlanificadorGRASP(ParametrosGRASP params, long semilla) {
        this.params = params;
        this.evaluador = new EvaluadorFO(params);
        this.random = new Random(semilla);
        this.constructor = new ConstructorGRASP(params, evaluador, random);
        this.buscador = new BuscadorLocal(params, evaluador, constructor);
        this.historialFO = new ArrayList<>();
    }

    /**
     * Ejecuta el algoritmo GRASP completo sobre el conjunto de envíos y la red dada.
     *
     * @param envios  Lista de envíos a planificar
     * @param red     Red logística (aeropuertos + vuelos)
     * @return        Mejor solución encontrada
     */
    public Solucion planificar(List<Envio> envios, RedLogistica red) {
        params.validar();
        historialFO.clear();

        long inicio = System.currentTimeMillis();

        Map<String, Ruta> mejorSolucion = null;
        double mejorFO = Double.MAX_VALUE;
        int iterSinMejora = 0;

        for (int iter = 1; iter <= params.getMaxIteraciones(); iter++) {

            // Reiniciar capacidades de la red para esta iteración
            red.reiniciarCapacidades();

            // === FASE 1: CONSTRUCCIÓN GREEDY ALEATORIZADA ===
            Map<String, Ruta> solucionActual = constructor.construir(envios, red);

            // === FASE 2: BÚSQUEDA LOCAL ===
            double foLocal = buscador.mejorar(solucionActual, envios, red);

            historialFO.add(foLocal);

            // === ACTUALIZACIÓN DE LA MEJOR SOLUCIÓN ===
            if (foLocal < mejorFO) {
                mejorFO = foLocal;
                mejorSolucion = new LinkedHashMap<>(solucionActual);
                iterSinMejora = 0;

                System.out.printf("[GRASP iter=%d] Nueva mejor FO: %.4f%n", iter, mejorFO);
            } else {
                iterSinMejora++;
            }

            // === CRITERIO DE PARADA ANTICIPADA ===
            if (iterSinMejora >= params.getMaxSinMejora()) {
                System.out.printf("[GRASP] Parada anticipada en iter=%d (sinMejora=%d)%n",
                    iter, iterSinMejora);
                iteracionesEjecutadas = iter;
                break;
            }

            iteracionesEjecutadas = iter;
        }

        tiempoEjecucionMs = System.currentTimeMillis() - inicio;

        // Restaurar la mejor solución en la red
        red.reiniciarCapacidades();
        if (mejorSolucion != null) {
            restaurarSolucion(mejorSolucion, envios, red);
        }

        // Construir y retornar objeto Solucion
        Solucion resultado = new Solucion();
        if (mejorSolucion != null) {
            for (Map.Entry<String, Ruta> entrada : mejorSolucion.entrySet()) {
                resultado.asignarRuta(entrada.getKey(), entrada.getValue());
            }
        }
        resultado.setValorFO(mejorFO);
        return resultado;
    }

    /**
     * Replanificación parcial: solo reasigna los envíos afectados por una cancelación.
     * Mantiene fijas las rutas de los demás envíos.
     *
     * @param enviosAfectados  Envíos cuya ruta quedó inválida por cancelación de vuelo
     * @param solucionBase     Solución existente (se actualiza in-place)
     * @param red              Red logística con el vuelo ya marcado como cancelado
     */
    public void replanificar(List<Envio> enviosAfectados, Solucion solucionBase, RedLogistica red) {
        System.out.printf("[GRASP] Replanificando %d envíos afectados...%n", enviosAfectados.size());

        // Liberar las rutas afectadas de las capacidades de la red
        for (Envio envio : enviosAfectados) {
            Ruta rutaAnterior = solucionBase.getRuta(envio.getIdEnvio());
            if (rutaAnterior != null) {
                for (Vuelo v : rutaAnterior.getVuelos()) {
                    v.liberarMaletas(envio.getCantidadMaletas());
                }
                solucionBase.asignarRuta(envio.getIdEnvio(), null);
                envio.setEstado(Envio.Estado.PENDIENTE);
            }
        }

        // Ejecutar construcción GRASP solo sobre los envíos afectados (iteraciones reducidas)
        int maxIterOrig = params.getMaxIteraciones();
        params.setMaxIteraciones(Math.min(50, maxIterOrig));

        Map<String, Ruta> nuevasRutas = constructor.construir(enviosAfectados, red);
        buscador.mejorar(nuevasRutas, enviosAfectados, red);

        // Incorporar las nuevas rutas a la solución base
        for (Map.Entry<String, Ruta> entrada : nuevasRutas.entrySet()) {
            solucionBase.asignarRuta(entrada.getKey(), entrada.getValue());
        }

        params.setMaxIteraciones(maxIterOrig);
        System.out.println("[GRASP] Replanificación completada.");
    }

    /**
     * Restaura el estado de capacidades de la red según una solución.
     */
    private void restaurarSolucion(Map<String, Ruta> solucion, List<Envio> envios, RedLogistica red) {
        Map<String, Envio> enviosMap = new HashMap<>();
        for (Envio e : envios) enviosMap.put(e.getIdEnvio(), e);

        for (Map.Entry<String, Ruta> entrada : solucion.entrySet()) {
            Ruta ruta = entrada.getValue();
            if (ruta == null) continue;
            Envio envio = enviosMap.get(entrada.getKey());
            if (envio == null) continue;
            for (Vuelo v : ruta.getVuelos()) {
                v.asignarMaletas(envio.getCantidadMaletas());
            }
        }
    }

    /**
     * Imprime el reporte de monitoreo con semáforos en consola.
     */
    public void imprimirReporteMonitoreo(RedLogistica red) {
        System.out.println("\n===== REPORTE DE MONITOREO TASF.B2B =====");
        System.out.println("-- Almacenes --");
        for (Aeropuerto a : red.getAeropuertos().values()) {
            double ocup = a.getOcupacionAlmacen();
            String semaforo = evaluador.semaforoOcupacion(ocup);
            System.out.printf("  %-6s %-12s: %3.0f%% [%s]%n",
                a.getCodigo(), a.getCiudad(), ocup * 100, semaforo);
        }
        System.out.println("-- Vuelos --");
        for (Vuelo v : red.getTodosLosVuelos()) {
            if (v.isCancelado()) continue;
            double ocup = v.getOcupacion();
            String semaforo = evaluador.semaforoOcupacion(ocup);
            System.out.printf("  %-25s: %3.0f%% [%s]%n", v.getId(), ocup * 100, semaforo);
        }
        System.out.println("-- Métricas de ejecución --");
        System.out.printf("  Iteraciones ejecutadas: %d%n", iteracionesEjecutadas);
        System.out.printf("  Tiempo de ejecución: %d ms%n", tiempoEjecucionMs);
        System.out.println("==========================================\n");
    }

    // --- Getters de métricas ---
    public int getIteracionesEjecutadas() { return iteracionesEjecutadas; }
    public long getTiempoEjecucionMs() { return tiempoEjecucionMs; }
    public List<Double> getHistorialFO() { return Collections.unmodifiableList(historialFO); }
    public ParametrosGRASP getParams() { return params; }
}
