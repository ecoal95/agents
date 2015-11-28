# Agents practice

 * Subject: Advanced Programming
 * Statement: See [statement.pdf][statement]

## Building

We use [Apache Ant](https://ant.apache.org) as our build tool, and also as our
run tool.

To build a single:

```sh
$ ant
```

Should be enough, and it should download the necessary dependencies and build
the application.

## Running

To run it you have two options:

```sh
$ ant run
```

Which runs it with just the info required by the statement, saving the debug
info in `run-debug.txt`, and

```sh
$ ant run-debug
```

Which displays all the information to the screen (disclaimer: huge).

[statement]: statement.pdf
