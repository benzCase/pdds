package pe.edu.pucp.tasfb2b.algoritmo;

import pe.edu.pucp.tasfb2b.model.Aeropuerto;
import pe.edu.pucp.tasfb2b.model.Vuelo;

import java.util.*;

/**
 * Grafo de la red logística de Tasf.B2B.
 * Mantiene los aeropuertos (V) y los vuelos disponibles (E),
 * con una lista de adyacencia para búsqueda eficiente de vuelos por origen.
 */
public class RedLogistica {

    private final Map<String, Aeropuerto> aeropuertos;              // código -> Aeropuerto
    private final List<Vuelo> todosLosVuelos;
    private final Map<String, List<Vuelo>> vuelosPorOrigen;         // código origen -> vuelos

    public RedLogistica() {
        this.aeropuertos = new LinkedHashMap<>();
        this.todosLosVuelos = new ArrayList<>();
        this.vuelosPorOrigen = new HashMap<>();
    }

    public void agregarAeropuerto(Aeropuerto aeropuerto) {
        aeropuertos.put(aeropuerto.getCodigo(), aeropuerto);
        vuelosPorOrigen.putIfAbsent(aeropuerto.getCodigo(), new ArrayList<>());
    }

    public void agregarVuelo(Vuelo vuelo) {
        todosLosVuelos.add(vuelo);
        vuelosPorOrigen
            .computeIfAbsent(vuelo.getOrigen().getCodigo(), k -> new ArrayList<>())
            .add(vuelo);
    }

    /**
     * Devuelve los vuelos que salen de un aeropuerto y cuya salida
     * es posterior a 'minutoDesde' + tiempo mínimo de conexión.
     */
    public List<Vuelo> getVuelosDesde(String codigoOrigen, int minutoDesde, int minConexion) {
        List<Vuelo> candidatos = vuelosPorOrigen.getOrDefault(codigoOrigen, Collections.emptyList());
        List<Vuelo> disponibles = new ArrayList<>();
        for (Vuelo v : candidatos) {
            if (!v.isCancelado() && v.getMinutosSalida() >= minutoDesde + minConexion) {
                disponibles.add(v);
            }
        }
        // Ordenar por hora de salida para facilitar la búsqueda en orden cronológico
        disponibles.sort(Comparator.comparingInt(Vuelo::getMinutosSalida));
        return disponibles;
    }

    /**
     * Cancela un vuelo por su ID y retorna true si se encontró.
     */
    public boolean cancelarVuelo(String idVuelo) {
        for (Vuelo v : todosLosVuelos) {
            if (v.getId().equals(idVuelo)) {
                v.setCancelado(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Reinicia las capacidades usadas de todos los vuelos (para nueva iteración GRASP).
     */
    public void reiniciarCapacidades() {
        for (Vuelo v : todosLosVuelos) {
            v.setCapacidadUsada(0);
        }
        for (Aeropuerto a : aeropuertos.values()) {
            a.setMaletasEnAlmacen(0);
        }
    }

    /**
     * Copia el estado actual de capacidades (para restaurar en búsqueda local).
     */
    public Map<String, Integer> snapCapacidadesVuelos() {
        Map<String, Integer> snap = new HashMap<>();
        for (Vuelo v : todosLosVuelos) {
            snap.put(v.getId(), v.getCapacidadUsada());
        }
        return snap;
    }

    public void restaurarCapacidadesVuelos(Map<String, Integer> snap) {
        for (Vuelo v : todosLosVuelos) {
            Integer val = snap.get(v.getId());
            if (val != null) v.setCapacidadUsada(val);
        }
    }

    // --- Getters ---
    public Map<String, Aeropuerto> getAeropuertos() { return Collections.unmodifiableMap(aeropuertos); }
    public Aeropuerto getAeropuerto(String codigo) { return aeropuertos.get(codigo); }
    public List<Vuelo> getTodosLosVuelos() { return Collections.unmodifiableList(todosLosVuelos); }

    /**
     * Carga la red de ejemplo con los 9 aeropuertos del enunciado.
     */
    public static RedLogistica crearRedEjemplo() {
        RedLogistica red = new RedLogistica();

        // América del Sur
        red.agregarAeropuerto(new Aeropuerto("SKBO", "Bogota",   "Colombia",      Aeropuerto.Continente.AMERICA_SUR, -5, 430));
        red.agregarAeropuerto(new Aeropuerto("SEQM", "Quito",    "Ecuador",       Aeropuerto.Continente.AMERICA_SUR, -5, 410));
        red.agregarAeropuerto(new Aeropuerto("SVMI", "Caracas",  "Venezuela",     Aeropuerto.Continente.AMERICA_SUR, -4, 400));
        // Europa
        red.agregarAeropuerto(new Aeropuerto("LATI", "Tirana",   "Albania",       Aeropuerto.Continente.EUROPA,      +2, 410));
        red.agregarAeropuerto(new Aeropuerto("EDDI", "Berlin",   "Alemania",      Aeropuerto.Continente.EUROPA,      +2, 480));
        red.agregarAeropuerto(new Aeropuerto("LOWW", "Viena",    "Austria",       Aeropuerto.Continente.EUROPA,      +2, 430));
        // Asia
        red.agregarAeropuerto(new Aeropuerto("VIDP", "Delhi",    "India",         Aeropuerto.Continente.ASIA,        +5, 480));
        red.agregarAeropuerto(new Aeropuerto("OSDI", "Damasco",  "Siria",         Aeropuerto.Continente.ASIA,        +3, 400));
        red.agregarAeropuerto(new Aeropuerto("OERK", "Riad",     "Arabia Saudita",Aeropuerto.Continente.ASIA,        +3, 420));

        // Vuelos del plan de ejemplo (del enunciado)
        Map<String, Aeropuerto> ap = new HashMap<>(red.getAeropuertos());
        String[] planesVuelo = {
            "SKBO-SEQM-03:34-04:21-0300",
            "SEQM-SKBO-04:29-05:16-0340",
            "SKBO-SEQM-14:22-15:09-0320",
            "SEQM-SKBO-08:05-08:52-0320",
            "SKBO-SEQM-19:01-19:48-0340",
            "SEQM-SKBO-19:55-20:42-0360",
            "SKBO-SVMI-07:24-09:47-0300",
            // Vuelos intercontinentales de ejemplo
            "SKBO-EDDI-10:00-22:00-0250",
            "EDDI-SKBO-08:00-18:00-0250",
            "EDDI-VIDP-06:00-14:00-0200",
            "VIDP-EDDI-16:00-22:00-0200",
            "LOWW-OERK-07:00-13:00-0180",
            "OERK-LOWW-14:00-20:00-0180",
            "SVMI-EDDI-09:00-21:00-0220",
            "LATI-VIDP-11:00-19:00-0190",
        };

        for (String linea : planesVuelo) {
            try {
                red.agregarVuelo(Vuelo.parsear(linea, ap));
            } catch (Exception e) {
                System.err.println("Error al parsear vuelo: " + linea + " - " + e.getMessage());
            }
        }

        return red;
    }
}
