package pe.edu.pucp.tasfb2b;

import pe.edu.pucp.tasfb2b.algoritmo.*;
import pe.edu.pucp.tasfb2b.experimentacion.ExperimentacionNumerica;
import pe.edu.pucp.tasfb2b.model.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Clase principal de demostración del planificador GRASP de Tasf.B2B.
 *
 * Muestra los tres escenarios:
 *   1. Simulación de periodo (5 días)
 *   2. Operación en tiempo real con replanificación por cancelación
 *   3. Simulación hasta colapso
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println(" TASF.B2B - PLANIFICADOR GRASP");
        System.out.println("========================================\n");

        // ---- ESCENARIO 1: SIMULACIÓN DE PERIODO ----
        System.out.println(">>> ESCENARIO 1: SIMULACIÓN DE PERIODO (1 día de prueba)\n");
        escenarioSimulacionPeriodo();

        // ---- ESCENARIO 2: TIEMPO REAL + CANCELACIÓN ----
        System.out.println("\n>>> ESCENARIO 2: TIEMPO REAL + REPLANIFICACIÓN POR CANCELACIÓN\n");
        escenarioTiempoReal();

        // ---- ESCENARIO 3: SIMULACIÓN HASTA COLAPSO ----
        System.out.println("\n>>> ESCENARIO 3: SIMULACIÓN HASTA COLAPSO\n");
        escenarioColapso();
    }

    // -------------------------------------------------------
    static void escenarioSimulacionPeriodo() {
        ParametrosGRASP params = new ParametrosGRASP();
        params.setAlpha(0.25);
        params.setMaxIteraciones(100);
        params.setMaxSinMejora(25);
        params.setEscenario(ParametrosGRASP.Escenario.SIMULACION_PERIODO);

        RedLogistica red = RedLogistica.crearRedEjemplo();
        List<Envio> envios = ExperimentacionNumerica.generarEnviosDePrueba(red);

        System.out.printf("Planificando %d envíos con GRASP [α=%.2f, iter=%d]...%n",
            envios.size(), params.getAlpha(), params.getMaxIteraciones());

        PlanificadorGRASP grasp = new PlanificadorGRASP(params, 42L);
        Solucion solucion = grasp.planificar(envios, red);

        System.out.println("\n--- Resultado ---");
        System.out.println(solucion);
        System.out.printf("Tiempo de ejecución: %d ms%n", grasp.getTiempoEjecucionMs());
        System.out.printf("Iteraciones: %d/%d%n",
            grasp.getIteracionesEjecutadas(), params.getMaxIteraciones());

        // Reporte de monitoreo con semáforos
        grasp.imprimirReporteMonitoreo(red);

        // Plan de viaje por envío (primeros 5)
        System.out.println("--- Plan de viaje (primeros 5 envíos) ---");
        int count = 0;
        for (Map.Entry<String, Ruta> e : solucion.getRutasPorEnvio().entrySet()) {
            if (count++ >= 5) break;
            Ruta ruta = e.getValue();
            if (ruta != null) {
                System.out.printf("  %s%n", ruta);
            } else {
                System.out.printf("  Envio[%s]: SIN RUTA ASIGNADA%n", e.getKey());
            }
        }
    }

    // -------------------------------------------------------
    static void escenarioTiempoReal() {
        ParametrosGRASP params = new ParametrosGRASP();
        params.setAlpha(0.20);
        params.setMaxIteraciones(50);  // iteraciones reducidas para tiempo real
        params.setMaxSinMejora(15);
        params.setEscenario(ParametrosGRASP.Escenario.TIEMPO_REAL);

        RedLogistica red = RedLogistica.crearRedEjemplo();
        List<Envio> envios = ExperimentacionNumerica.generarEnviosDePrueba(red);

        // Planificación inicial
        PlanificadorGRASP grasp = new PlanificadorGRASP(params, 42L);
        Solucion solucion = grasp.planificar(envios, red);
        System.out.println("Planificación inicial: " + solucion);

        // Simular cancelación de un vuelo
        String vueloCancelado = "SKBO-SEQM-0334";
        boolean cancelado = red.cancelarVuelo(vueloCancelado);
        System.out.printf("%nCancelación de vuelo [%s]: %s%n",
            vueloCancelado, cancelado ? "OK" : "No encontrado");

        // Identificar envíos afectados
        List<Envio> afectados = new ArrayList<>();
        for (Map.Entry<String, Ruta> entrada : solucion.getRutasPorEnvio().entrySet()) {
            Ruta ruta = entrada.getValue();
            if (ruta == null) continue;
            boolean afectado = ruta.getVuelos().stream()
                .anyMatch(v -> v.getId().equals(vueloCancelado));
            if (afectado) {
                // Buscar el envío correspondiente
                for (Envio env : envios) {
                    if (env.getIdEnvio().equals(entrada.getKey())) {
                        afectados.add(env);
                        break;
                    }
                }
            }
        }

        System.out.printf("Envíos afectados por cancelación: %d%n", afectados.size());

        // Replanificación parcial
        if (!afectados.isEmpty()) {
            grasp.replanificar(afectados, solucion, red);
            System.out.println("Solución tras replanificación: " + solucion);
        } else {
            System.out.println("Ningún envío afectado por la cancelación.");
        }
    }

    // -------------------------------------------------------
    static void escenarioColapso() {
        ParametrosGRASP params = new ParametrosGRASP();
        params.setAlpha(0.20);
        params.setMaxIteraciones(50);
        params.setMaxSinMejora(15);
        params.setEscenario(ParametrosGRASP.Escenario.COLAPSO);
        // Pesos ajustados para maximizar carga antes del colapso
        params.setPesos(0.20, 0.50, 0.30);

        RedLogistica red = RedLogistica.crearRedEjemplo();
        Random rng = new Random(99);

        int paso = 0;
        boolean colapso = false;

        System.out.println("Simulando incremento de demanda hasta colapso...\n");

        while (!colapso && paso < 20) {
            paso++;
            // Aumentar demanda en cada paso
            List<Envio> envios = generarEnviosEscalados(red, rng, paso * 30);

            red.reiniciarCapacidades();
            PlanificadorGRASP grasp = new PlanificadorGRASP(params, paso);
            Solucion sol = grasp.planificar(envios, red);

            // Verificar si algún vuelo o almacén supera el 100%
            boolean vueloSaturado = red.getTodosLosVuelos().stream()
                .anyMatch(v -> !v.isCancelado() && v.getOcupacion() > 1.0);
            boolean almacenSaturado = red.getAeropuertos().values().stream()
                .anyMatch(a -> a.getOcupacionAlmacen() > 1.0);

            System.out.printf("Paso %2d | Envíos: %4d | FO: %10.2f | Cumpl: %4.1f%% | Vuelo>100%%: %s | Almacén>100%%: %s%n",
                paso, envios.size(), sol.getValorFO(), sol.getTasaCumplimiento() * 100,
                vueloSaturado ? "SÍ" : "no", almacenSaturado ? "SÍ" : "no");

            if (vueloSaturado || almacenSaturado) {
                colapso = true;
                System.out.printf("%n*** COLAPSO DETECTADO en paso %d con %d envíos ***%n",
                    paso, envios.size());
            }
        }

        if (!colapso) {
            System.out.println("La red aguantó sin colapso en los 20 pasos simulados.");
        }
    }

    static List<Envio> generarEnviosEscalados(RedLogistica red, Random rng, int numEnvios) {
        List<Envio> envios = new ArrayList<>();
        String[] codigos = {"SKBO", "SEQM", "SVMI", "LATI", "EDDI", "LOWW", "VIDP", "OSDI", "OERK"};
        LocalDateTime base = LocalDateTime.of(2025, 1, 2, 0, 0);
        int id = 1;
        for (int i = 0; i < numEnvios; i++) {
            String orig, dest;
            do {
                orig = codigos[rng.nextInt(codigos.length)];
                dest = codigos[rng.nextInt(codigos.length)];
            } while (orig.equals(dest));
            Aeropuerto aOrig = red.getAeropuerto(orig);
            Aeropuerto aDest = red.getAeropuerto(dest);
            if (aOrig == null || aDest == null) continue;
            int maletas = 5 + rng.nextInt(30);
            LocalDateTime ts = base.plusHours(rng.nextInt(20)).plusMinutes(rng.nextInt(60));
            boolean mismo = aOrig.getContinente() == aDest.getContinente();
            int plazo = mismo ? 1440 : 2880;
            envios.add(new Envio(String.format("%08d", id++), ts, aOrig, aDest, maletas,
                String.format("%07d", rng.nextInt(9999999)), plazo));
        }
        return envios;
    }
}
