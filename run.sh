#!/bin/bash
set -e

ARGS=""

for arg in "$@"; do
ARGS="$ARGS $arg"
done

sbt "run $ARGS"
