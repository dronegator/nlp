#!/bin/bash
from=${1:-/tmp/a.txt}
to=${2:-/var/tmp/$(basename $from .txt).dat}

sbt "index-stream/run $from /tmp/tmp.dat" &&

sbt "index-stream/run $from $to /tmp/tmp.dat" &&

echo "sbt \"repl/run $to\"" &&

sbt "repl/run $to" 

echo "sbt \"repl/run $to\""
