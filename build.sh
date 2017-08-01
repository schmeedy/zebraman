#!/bin/bash -e -u

docker run --rm -it -v "$PWD:/app" -v "$HOME/.ivy2:/root/.ivy2" --entrypoint=sbt bigtruedata/sbt:0.13.15-2.12.2 assembly
docker build -t schmeedy/zebraman .