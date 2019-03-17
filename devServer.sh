#!/usr/bin/env bash
ARGS=""
	
for arg in "$@"; do
ARGS="$ARGS $arg"
done

sbt "~;reStart serve -r $ARGS"
