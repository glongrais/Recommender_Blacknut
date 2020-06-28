#!/usr/bin/env bash

eval_dir="../app"

function execute_command() {
  case $1 in
    run)
      echo "Running..."
      (cd $eval_dir && ./run.sh)
      ;;
    compile)
      echo "Compiling..."
      (cd $eval_dir && mvn package)
      ;;
    help)
      display_usage
      ;;
    open)
      gnome-terminal \
        --tab --working-directory=$eval_dir \
      ;;
    *)
      echo "Invalid argument: $1"
      display_usage
      ;;
  esac
}

function display_usage() {
  cat <<EOF
Usage: $0 [run|compile|help|open]*
EOF
}

if [ $# -eq 0 ]
  then
    display_usage
fi

for arg in $@; do
  execute_command $arg
done
