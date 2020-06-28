#!/bin/bash

OPT="../../tmp/coclust6"
CONFIG="test_config.yml"

mkdir -p $OPT

java -jar "target/binder-0.0.1-SNAPSHOT.jar" #--config="$CONFIG" -DOPT="$OPT"
