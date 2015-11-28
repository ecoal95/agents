# Práctica de agentes

 * Asignatura: Progamación avanzada
 * Statement: See [statement.pdf][statement]

## Compilando

Usamos [Apache Ant](https://ant.apache.org) como nuestra infraestructura para
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
