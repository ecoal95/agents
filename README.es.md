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

[statement]: statement.pdf
[ant]: https://ant.apache.org
