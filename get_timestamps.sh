#!/bin/sh

cat log-host-server.txt | grep "^\[TEST\]" >  timings.txt
rm log-host-server.txt
touch log-host-server.txt
