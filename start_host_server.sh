#!/bin/sh

n=$1

if [ "$n" = "" ]; then
    n=5
fi
if [ "$n" > 5 ]; then
    n=5
fi

if [ -e "log-host-server.txt" ]
then
    rm log-host-server.txt
fi
touch log-host-server.txt

for i in `seq 1 $n`;
do
    cmd="java -jar host_server.jar $n & >> log-host-server.txt 2>&1"
    echo $cmd
    eval $cmd
done

tail -f log-host-server.txt
