#!/bin/sh

name=$1

cd vagrant/
vagrant halt $name --machine-readable
