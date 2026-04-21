# GRASP Tasf.B2B — Planificador de Rutas de Maletas

## Estructura del proyecto

```
src/main/java/pe/edu/pucp/tasfb2b/
│
├── model/
│   ├── Aeropuerto.java       Nodo del grafo: aeropuerto con almacén
│   ├── Vuelo.java            Arista del grafo: vuelo con capacidad
│   ├── Envio.java            Solicitud de traslado de maletas
│   ├── Ruta.java             Asignación de vuelos a un envío
│   └── Solucion.java         Colección de rutas + métricas de la FO
│
├── algoritmo/
│   ├── ParametrosGRASP.java  Todos los parámetros configurables
│   ├── RedLogistica.java     Grafo de la red + lista de adyacencia
│   ├── EvaluadorFO.java      Función objetivo ponderada (w1/w2/w3)
│   ├── ConstructorGRASP.java Fase de construcción greedy aleatorizada (RCL)
│   ├── BuscadorLocal.java    Fase de búsqueda local (swap + reasignación)
│   └── PlanificadorGRASP.java Orquestador principal + replanificación
│
├── experimentacion/
│   └── ExperimentacionNumerica.java Barrido de α e iteraciones
│
└── Main.java                 Demo de los 3 escenarios
```

## Compilación

```bash
# Desde la raíz del proyecto
javac -d out -sourcepath src/main/java \
    $(find src/main/java -name "*.java")
```

## Ejecución

```bash
# Escenarios de demostración
java -cp out pe.edu.pucp.tasfb2b.Main

# Experimentación numérica (barrido de parámetros)
java -cp out pe.edu.pucp.tasfb2b.experimentacion.ExperimentacionNumerica
```

## Parámetros clave

| Parámetro | Valor recomendado | Descripción |
|-----------|-------------------|-------------|
| α (alpha) | 0.20–0.30 | Balance greedy/aleatorio en la RCL |
| maxIteraciones | 200 (periodo), 50 (tiempo real) | Iteraciones GRASP |
| maxSinMejora | 30–40 | Parada anticipada |
| maxEscalas | 2 | Máximo vuelos por ruta |
| w1, w2, w3 | 0.60, 0.25, 0.15 | Pesos de la función objetivo |
| umbralVerde | 0.70 | Semáforo verde ≤ 70% |
| umbralAmbar | 0.90 | Semáforo ámbar 71–90% |

## Bibliografía

- Granado et al. (2025). GRASP multiobjetivo para ruteo de flota. COR, 174.
- Tanash & As'Ad (2025). Heurísticas para VRPTW heterogéneo. IEEE Access, 13.
- Interian & Ribeiro (2017). GRASP con path-relinking para STSP. ITOR, 24(6).
- Cardinaël et al. (2026). GRASP memético para ruteo periódico de técnicos. LNCS 15926.
