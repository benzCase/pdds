package pe.edu.pucp.tasfb2b.algoritmo;

import pe.edu.pucp.tasfb2b.model.*;

import java.util.*;

/**
 * Fase de Construcción Greedy Aleatorizada del algoritmo GRASP.
 *
 * Para cada envío (en orden de prioridad: más maletas primero, plazo menor primero)
 * genera las rutas factibles, construye la RCL (Lista Restringida de Candidatos)
 * parametrizada por α y selecciona una ruta aleatoriamente de la RCL.
 *
 * Referencias:
 *  - Interian & Ribeiro (2017): estructura general del GRASP con RCL parametrizada.
 *  - Tanash & As'Ad (2025): priorización de envíos en construcción greedy.
 */
public class ConstructorGRASP {

    private final ParametrosGRASP params;
    private final EvaluadorFO evaluador;
    private final Random random;

    public ConstructorGRASP(ParametrosGRASP params, EvaluadorFO evaluador, Random random) {
        this.params = params;
        this.evaluador = evaluador;
        this.random = random;
    }

    /**
     * Construye una solución greedy aleatorizada completa.
     *
     * @param envios  Lista de envíos a planificar
     * @param red     Red logística con vuelos disponibles y capacidades actuales
     * @return        Mapa idEnvio -> Ruta (null si no se pudo asignar)
     */
    public Map<String, Ruta> construir(List<Envio> envios, RedLogistica red) {
        Map<String, Ruta> solucion = new LinkedHashMap<>();

        // Ordenar envíos por prioridad: mayor cantidad de maletas primero,
        // desempate por plazo más corto primero
        List<Envio> ordenados = new ArrayList<>(envios);
        ordenados.sort(Comparator
            .comparingInt(Envio::getCantidadMaletas).reversed()
            .thenComparingInt(Envio::getPlazoMaxMinutos));

        for (Envio envio : ordenados) {
            // Calcular todas las rutas factibles para este envío
            List<Ruta> candidatos = generarRutasFactibles(envio, red);

            if (candidatos.isEmpty()) {
                solucion.put(envio.getIdEnvio(), null); // no asignado -> penalidad big-M
                continue;
            }

            // Calcular el costo greedy de cada candidato
            for (Ruta r : candidatos) {
                // El costo ya se calcula en evaluarRutaGreedy y está en Ruta.costoGreedy
            }

            // Construir la RCL parametrizada por α
            Ruta elegida = seleccionarDeRCL(candidatos, envio);

            // Asignar ruta y actualizar capacidades en la red
            if (elegida != null) {
                solucion.put(envio.getIdEnvio(), elegida);
                actualizarCapacidades(elegida, envio, red);
                envio.setEstado(Envio.Estado.ASIGNADO);
            } else {
                solucion.put(envio.getIdEnvio(), null);
            }
        }

        return solucion;
    }

    /**
     * Genera todas las rutas factibles para un envío usando BFS con poda.
     *
     * Una ruta es factible si:
     *  1. Llega al destino correcto
     *  2. Cada vuelo tiene capacidad residual suficiente
     *  3. El tiempo total no supera plazoMax * 1.5 (holgura para búsqueda local)
     *  4. No usa más de maxEscalas vuelos
     *  5. El tiempo de espera en cada escala ≤ tiempoMaxEspera
     */
    public List<Ruta> generarRutasFactibles(Envio envio, RedLogistica red) {
        List<Ruta> rutasFactibles = new ArrayList<>();

        // Estado de BFS: (codigoNodoActual, minutosActuales, vuelosUsados)
        // Usamos una cola de objetos EstadoBFS
        Deque<EstadoBFS> cola = new ArrayDeque<>();

        // Minutos disponibles desde la llegada del envío (se usa la hora del timestamp)
        int minutoInicio = envio.getTimestamp().getHour() * 60 + envio.getTimestamp().getMinute();

        cola.add(new EstadoBFS(
            envio.getOrigen().getCodigo(),
            minutoInicio,
            new ArrayList<>()
        ));

        int plazoConHolgura = (int) (envio.getPlazoMaxMinutos() * 1.5);

        while (!cola.isEmpty()) {
            EstadoBFS estado = cola.poll();

            // Si llegamos al destino, registrar la ruta si cumple el plazo
            if (estado.nodoActual.equals(envio.getDestino().getCodigo())) {
                int tiempoTotal = estado.minutoActual - minutoInicio;
                if (tiempoTotal <= plazoConHolgura) {
                    List<Vuelo> vuelosCopia = new ArrayList<>(estado.vuelosUsados);
                    Ruta ruta = construirRuta(envio, vuelosCopia, tiempoTotal, red);
                    rutasFactibles.add(ruta);
                }
                continue; // No seguir explorando desde el destino
            }

            // Poda de profundidad: no más de maxEscalas vuelos
            if (estado.vuelosUsados.size() >= params.getMaxEscalas() + 1) {
                continue;
            }

            // Expandir: vuelos disponibles desde nodoActual
            List<Vuelo> vuelosDisponibles = red.getVuelosDesde(
                estado.nodoActual,
                estado.minutoActual,
                estado.vuelosUsados.isEmpty() ? 0 : params.getTiempoMinimoConexionMinutos()
            );

            for (Vuelo vuelo : vuelosDisponibles) {
                // Poda de capacidad: el vuelo debe tener espacio suficiente
                if (!vuelo.puedeAceptar(envio.getCantidadMaletas())) continue;

                // Poda temporal: el tiempo de espera en escala no puede ser excesivo
                int tiempoEspera = vuelo.getMinutosSalida() - estado.minutoActual;
                if (!estado.vuelosUsados.isEmpty() && tiempoEspera > params.getTiempoMaxEsperaMinutos()) {
                    continue;
                }

                // Poda de plazo: el tiempo acumulado hasta llegada no debe exceder plazo con holgura
                int tiempoAcumulado = vuelo.getMinutosLlegada() - minutoInicio;
                if (tiempoAcumulado > plazoConHolgura) continue;

                // Evitar ciclos: no visitar un aeropuerto ya visitado
                if (yaVisitado(estado.vuelosUsados, vuelo.getDestino().getCodigo())) continue;

                List<Vuelo> nuevosVuelos = new ArrayList<>(estado.vuelosUsados);
                nuevosVuelos.add(vuelo);

                cola.add(new EstadoBFS(
                    vuelo.getDestino().getCodigo(),
                    vuelo.getMinutosLlegada(),
                    nuevosVuelos
                ));
            }
        }

        return rutasFactibles;
    }

    /**
     * Construye un objeto Ruta con su costo greedy calculado.
     */
    private Ruta construirRuta(Envio envio, List<Vuelo> vuelos, int tiempoTotal, RedLogistica red) {
        // Simular asignación temporal para calcular saturación
        double costoGreedy = calcularCostoRuta(envio, vuelos, tiempoTotal);
        return new Ruta(envio, vuelos, tiempoTotal, costoGreedy);
    }

    private double calcularCostoRuta(Envio envio, List<Vuelo> vuelos, int tiempoTotal) {
        int retraso = Math.max(0, tiempoTotal - envio.getPlazoMaxMinutos());
        double retrasoNorm = (double) retraso / Math.max(1, envio.getPlazoMaxMinutos());
        double saturacion = vuelos.stream()
            .mapToDouble(Vuelo::getOcupacion)
            .average().orElse(0);
        double escalasNorm = (double) (vuelos.size() - 1) / Math.max(1, params.getMaxEscalas());
        return params.getW1() * retrasoNorm
             + params.getW2() * saturacion
             + params.getW3() * escalasNorm;
    }

    /**
     * Construye la RCL y selecciona una ruta aleatoriamente.
     * RCL = {r : costo(r) ≤ costoMin + α * (costoMax - costoMin)}
     */
    private Ruta seleccionarDeRCL(List<Ruta> candidatos, Envio envio) {
        if (candidatos.isEmpty()) return null;

        double costoMin = candidatos.stream().mapToDouble(Ruta::getCostoGreedy).min().orElse(0);
        double costoMax = candidatos.stream().mapToDouble(Ruta::getCostoGreedy).max().orElse(0);
        double umbralRCL = costoMin + params.getAlpha() * (costoMax - costoMin);

        List<Ruta> rcl = new ArrayList<>();
        for (Ruta r : candidatos) {
            if (r.getCostoGreedy() <= umbralRCL + 1e-9) {
                rcl.add(r);
            }
        }

        if (rcl.isEmpty()) rcl.add(candidatos.get(0)); // fallback al mejor

        return rcl.get(random.nextInt(rcl.size()));
    }

    /**
     * Actualiza las capacidades residuales en la red al asignar una ruta.
     */
    private void actualizarCapacidades(Ruta ruta, Envio envio, RedLogistica red) {
        for (Vuelo vuelo : ruta.getVuelos()) {
            vuelo.asignarMaletas(envio.getCantidadMaletas());
        }
        // Si hay escalas, las maletas pasan por el almacén del aeropuerto intermedio
        List<Vuelo> vuelos = ruta.getVuelos();
        for (int i = 0; i < vuelos.size() - 1; i++) {
            Aeropuerto escala = vuelos.get(i).getDestino();
            escala.agregarMaletas(envio.getCantidadMaletas());
        }
    }

    private boolean yaVisitado(List<Vuelo> vuelos, String codigoDestino) {
        for (Vuelo v : vuelos) {
            if (v.getOrigen().getCodigo().equals(codigoDestino)) return true;
        }
        return false;
    }

    /** Estado interno del BFS para generación de rutas. */
    private static class EstadoBFS {
        final String nodoActual;
        final int minutoActual;
        final List<Vuelo> vuelosUsados;

        EstadoBFS(String nodoActual, int minutoActual, List<Vuelo> vuelosUsados) {
            this.nodoActual = nodoActual;
            this.minutoActual = minutoActual;
            this.vuelosUsados = vuelosUsados;
        }
    }
}
