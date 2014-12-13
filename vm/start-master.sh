#!/bin/sh
clean-up.sh
hdfs namenode -format
hadoop-daemon.sh start namenode
yarn-daemons.sh start resourcemanager