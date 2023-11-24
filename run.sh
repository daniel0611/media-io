#!/usr/bin/env bash
set -e

export SBT_OPTS="-Xmx2G"

ARGS=""

for arg in "$@"; do
ARGS="$ARGS $arg"
done

sbt "run $ARGS"
