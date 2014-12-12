#!/bin/sh
name=$1

cd vagrant/
vagrant up $name --machine-readable
vagrant ssh $name -c "ip address show eth1 | grep 'inet ' | sed -e 's/^.*inet //' -e 's/\/.*$//'"
vagrant provision $name --machine-readable
