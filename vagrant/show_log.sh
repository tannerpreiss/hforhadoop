#!/bin/sh

del="rm log-frosty-*.txt"
echo $del
eval $del
make="touch log-frosty-.txt"
echo $make
eval $make
touch log-frosty-1.txt
touch log-frosty-2.txt
touch log-frosty-3.txt
touch log-frosty-4.txt
touch log-frosty-5.txt
see="tail -f log-frosty-*.txt"
echo $see
eval $see
