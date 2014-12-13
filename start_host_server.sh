#!/bin/sh
n=$1

if [ "$n" = "" ]; then
    echo "here"
    n=5
fi
if [ "$n" -gt 5 ]; then
    n=5
fi
if [ "$n" -lt 1 ]; then
    n=1
fi

touch log-host-server.txt

echo "Run $n host servers..."
echo "[TEST] Start VM run" >> log-host-server.txt
for i in `seq 1 $n`;
do
    cmd="java -jar host_server.jar $i >> log-host-server.txt 2>&1 &"
    printf "==> Run frosty-$i: $cmd\n"
    eval $cmd
    sleep 3
done

tail -f log-host-server.txt
