#!/bin/bash

set -e

./gradlew jar

java -jar build/libs/*.jar $@
