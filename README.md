# SmartField - Capa de orquestación (patrones integrados)

Trabajo Práctico Final de Ingeniería de Datos 2 - UADE

Profesor: Fernandez, Alfonso Martin

Alumnos: Ametller Mercedes, Galli Lucila, Sack Nicolas, Suarez Juan Pablo y Vila Galeano Sofia - Grupo 8

## Requisitos

- Java 17 o superior y Maven 3.8+.
- Los tres motores levantados y **con los datos del anexo cargados**. Sin los datos, las
  consultas devuelven vacío.

## Cómo ejecutarlo

```bash
cd orquestacion-java
mvn spring-boot:run
```

Al arrancar, la demo imprime por consola los resultados de los cuatro patrones.
Además, la API queda disponible en `http://localhost:8090`, por ejemplo:

```bash
curl http://localhost:8090/api/patrones/vecinos-en-riesgo/lote-117?ventanaDias=14
curl "http://localhost:8090/api/patrones/correlacion-lluvia-plaga/lote-117?ventanaDias=5&umbralMm=25"
curl http://localhost:8090/api/patrones/biografia/lote-117
curl "http://localhost:8090/api/patrones/zona-critica/lote-117?ventanaDias=14&umbralHumedad=18"
```

La configuración (URIs, credenciales, fecha de referencia) está en
`src/main/resources/application.yml`.
La propiedad `smartfield.fecha-referencia` fija el "ahora" en 2026-05-31 para
reproducir el dataset histórico; en un sistema real se usaría la fecha actual.
