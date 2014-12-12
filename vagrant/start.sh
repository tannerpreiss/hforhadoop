#!/bin/sh

n=$1

if [ "$n" = "" ]; then
    n='2'
fi

vagrant up frosty-$n --machine-readable
vagrant ssh frosty-$n -c "ip address show eth1 | grep 'inet ' | sed -e 's/^.*inet //' -e 's/\/.*$//'"
vagrant provision frosty-$n --machine-readable
