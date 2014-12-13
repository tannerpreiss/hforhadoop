#!/bin/sh
clean-up.sh
hadoop-daemon.sh start datanode
yarn-daemon.sh start nodemanager
