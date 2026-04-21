package pe.edu.pucp.tasfb2b.experimentacion;

import pe.edu.pucp.tasfb2b.algoritmo.*;
import pe.edu.pucp.tasfb2b.model.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Módulo de Experimentación Numérica para el algoritmo GRASP de Tasf.B2B.
 *
 * Evalúa distintas configuraciones de α y otros parámetros para encontrar
 * la configuración óptima del planificador. Genera una tabla comparativa
 * de resultados (FO, tasa de cumplimiento, tiempo de ejecución).
 *
 * Permite también la comparación directa GRASP vs Tabu Search mediante
 * la interfaz IAlgoritmoComparable.
 */
public class ExperimentacionNumerica {

    /** Instancia de experimentación con datos de prueba del enunciado. */
    public static void main(String[] args) {
        System.out.println("=== EXPERIMENTACIÓN NUMÉRICA - GRASP TASF.B2B ===\n");

        RedLogistica red = RedLogistica.crearRedEjemplo();
        List<Envio> envios = generarEnviosDePrueba(red);

        System.out.printf("Red: %d aeropuertos, %d vuelos%n",
            red.getAeropuertos().size(), red.getTodosLosVuelos().size());
        System.out.printf("Envíos a planificar: %d (total maletas: %d)%n%n",
            envios.size(), envios.stream().mapToInt(Envio::getCantidadMaletas).sum());

        // Configuraciones a evaluar
        double[] alphas = {0.0, 0.1, 0.2, 0.3, 0.5, 0.7};
        int[] iteraciones = {50, 100, 200};

        ResultadoExperimento mejorResultado = null;
        List<ResultadoExperimento> resultados = new ArrayList<>();

        System.out.printf("%-8s %-8s %-12s %-12s %-10s %-10s%n",
            "Alpha", "Iter", "FO", "Cumplimiento%", "TiempoMs", "Asignados");
        System.out.println("-".repeat(65));

        for (double alpha : alphas) {
            for (int maxIter : iteraciones) {
                ParametrosGRASP params = new ParametrosGRASP(
                    alpha, maxIter, 30, ParametrosGRASP.Escenario.SIMULACION_PERIODO);

                // Ejecutar 3 réplicas para estabilizar resultados (semillas distintas)
                List<Double> foReplicas = new ArrayList<>();
                List<Double> tasas = new ArrayList<>();
                List<Long> tiempos = new ArrayList<>();

                for (long semilla : new long[]{42L, 123L, 777L}) {
                    RedLogistica redCopia = RedLogistica.crearRedEjemplo(); // red fresca
                    List<Envio> enviosCopia = generarEnviosDePrueba(redCopia);

                    PlanificadorGRASP grasp = new PlanificadorGRASP(params, semilla);
                    Solucion sol = grasp.planificar(enviosCopia, redCopia);

                    foReplicas.add(sol.getValorFO());
                    tasas.add(sol.getTasaCumplimiento());
                    tiempos.add(grasp.getTiempoEjecucionMs());
                }

                double foMedia   = foReplicas.stream().mapToDouble(d -> d).average().orElse(0);
                double tasaMedia = tasas.stream().mapToDouble(d -> d).average().orElse(0);
                double tMedia    = tiempos.stream().mapToLong(l -> l).average().orElse(0);

                ResultadoExperimento res = new ResultadoExperimento(
                    alpha, maxIter, foMedia, tasaMedia, (long) tMedia, envios.size());
                resultados.add(res);

                System.out.printf("%-8.2f %-8d %-12.2f %-12.1f %-10.0f %-10d%n",
                    alpha, maxIter, foMedia, tasaMedia * 100, tMedia, envios.size());

                if (mejorResultado == null || foMedia < mejorResultado.foMedia) {
                    mejorResultado = res;
                }
            }
        }

        System.out.println("-".repeat(65));
        if (mejorResultado != null) {
            System.out.printf("%nMejor configuración: α=%.2f, iter=%d → FO=%.4f, cumplimiento=%.1f%%%n",
                mejorResultado.alpha, mejorResultado.maxIteraciones,
                mejorResultado.foMedia, mejorResultado.tasaCumplimiento * 100);
        }
    }

    /**
     * Genera un conjunto de envíos de prueba para la red de ejemplo.
     * Simula envíos para un día de operación en todos los aeropuertos origen.
     */
    public static List<Envio> generarEnviosDePrueba(RedLogistica red) {
        List<Envio> envios = new ArrayList<>();
        Random rng = new Random(42);

        String[] codigos = {"SKBO", "SEQM", "SVMI", "LATI", "EDDI", "LOWW", "VIDP", "OSDI", "OERK"};
        LocalDateTime baseTime = LocalDateTime.of(2025, 1, 2, 0, 0);

        int idCounter = 1;
        for (String origen : codigos) {
            Aeropuerto aerOrigen = red.getAeropuerto(origen);
            if (aerOrigen == null) continue;

            // Generar 5–10 envíos por aeropuerto
            int numEnvios = 5 + rng.nextInt(6);
            for (int i = 0; i < numEnvios; i++) {
                // Elegir destino distinto al origen
                String destino;
                do {
                    destino = codigos[rng.nextInt(codigos.length)];
                } while (destino.equals(origen));

                Aeropuerto aerDestino = red.getAeropuerto(destino);
                if (aerDestino == null) continue;

                int maletas = 1 + rng.nextInt(50);
                int hora = rng.nextInt(20);
                int minuto = rng.nextInt(60);
                LocalDateTime ts = baseTime.plusHours(hora).plusMinutes(minuto);

                boolean mismoContinente = aerOrigen.getContinente() == aerDestino.getContinente();
                int plazo = mismoContinente ? 1440 : 2880;

                String idEnvio = String.format("%08d", idCounter++);
                Envio envio = new Envio(idEnvio, ts, aerOrigen, aerDestino,
                    maletas, String.format("%07d", rng.nextInt(9999999)), plazo);
                envios.add(envio);
            }
        }

        return envios;
    }

    /** Resultado de un experimento individual para comparación. */
    public static class ResultadoExperimento {
        final double alpha;
        final int maxIteraciones;
        final double foMedia;
        final double tasaCumplimiento;
        final long tiempoMs;
        final int numEnvios;

        ResultadoExperimento(double alpha, int maxIteraciones, double foMedia,
                              double tasaCumplimiento, long tiempoMs, int numEnvios) {
            this.alpha = alpha;
            this.maxIteraciones = maxIteraciones;
            this.foMedia = foMedia;
            this.tasaCumplimiento = tasaCumplimiento;
            this.tiempoMs = tiempoMs;
            this.numEnvios = numEnvios;
        }

        @Override
        public String toString() {
            return String.format("α=%.2f iter=%d FO=%.4f cumpl=%.1f%% t=%dms",
                alpha, maxIteraciones, foMedia, tasaCumplimiento * 100, tiempoMs);
        }
    }
}
