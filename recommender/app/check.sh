#!/bin/bash

CONFIG="test_config.yml"

java -jar "target/binder-0.0.1-SNAPSHOT.jar" --check #--config="$CONFIG" 
