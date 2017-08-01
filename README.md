# Checker for new loans on Zonky
Checks for new loans on Zonky every 5 minutes and prints them to the console.

## Implementations
There are 2 implementations:
* Java implementation with entry point in `com.github.schmeedy.zonky.java.JavaMain`
* Scala implementation in `com.github.schmeedy.zonky.scala.ScalaMain`

## Building
`./build.sh` produces a docker image `schmeedy/zebraman:latest`

## Running
You can use image from Docker Hub:
* Java version: `docker run --rm -it schmeedy/zebraman`
* Scala version: `docker run --rm -it schmeedy/zebraman -scala`