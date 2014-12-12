#!/bin/sh

n=$1

if [ "$n" = "" ]; then
    n='2'
fi

stop="vagrant halt frosty-$n --machine-readable"
echo $stop
eval $stop
