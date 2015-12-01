---
title: Cámbiame de grupo
subtitle: Programación Avanzada -- Práctica de agentes 2015
author:
  - Emilio Cobos Álvarez (70912324) <emiliocobos@usal.es>
lang: es-ES
toc-depth: 4
babel-lang: spanish
polyglossia-lang:
  options: []
  name: spanish
---

# Práctica de agentes

 * Asignatura: Progamación avanzada
 * Statement: See [statement.pdf][statement]

## Requerimientos

 * JVM 8+ (version tested: `javac 1.8.0_66-internal`, `OpenJDK Runtime
     Environment (build 1.8.0_66-internal-b17)`.
 * [Apache Ant][ant]

## Compilando

Usamos [Apache Ant][ant] como nuestra infraestructura para
compilar, y también (por simplicidad) para ejecutar la práctica.

Para compilar debería de ser suficiente con escribir el comando `ant`:

```sh
$ ant
```

Descargará las dependencias necesarias y compilará la aplicación.

## Ejecutando

Para ejecutar la práctica tienes dos opciones:

```sh
$ ant run
```

Que la ejecuta mostrando sólo la información requerida por el enunciado,
guardando la información de debug en `run-debug.txt`, y

```sh
$ ant run-debug
```

Que muestra toda la información en pantalla (aviso: grande).

## Ejecutando desde Eclipse

La práctica **no ha sido desarrollada con Eclipse**, si no con un sistema
GNU/Linux y VIM.

Por lo tanto, la práctica no está configurada para correr en eclipse, aunque es
posible de forma relativamente sencilla. No obstante si no quieres ver todo el
`stderr`, lo recomendado es que sigas estos pasos:

 * Ir al proyecto, hacer click en **Propiedades**, crear un **builder** que
     utilice **ant**.
 * Configurar como buildfile el archivo `build.xml`. Poniendo lo siguiente
     debería de ser suficiente: `${project_loc}/build.xml`
 * Configurar como directorio base el directorio del proyecto
     (`${project_loc}`).
 * En la pestaña `Targets`, configurar para que en el manual build ejecute las
     tareas `compile, run`.
 * Hacer click en **Proyecto** -> Compilar.
 * Si no hace nada, toca de manera no significativa un archivo de código fuente
     (eclipse se salta la "compilación" si no se ha tocado ningún archivo).
 * Te debería haber aparecido algo similar a esto:

 ![Práctica ejecutándose desde Eclipse](screenshots/running-from-eclipse.png)

[statement]: statement.pdf
[ant]: https://ant.apache.org
