#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
LOGS_DIR=$DEPLOY_DIR/logs
if [ ! -d $LOGS_DIR ]; then
    mkdir $LOGS_DIR
fi
STDOUT_FILE=$LOGS_DIR/stdout.log

PID=`ps -ef | grep -v grep | grep "$DEPLOY_DIR/conf" | awk '{print $2}'` 
echo "PID: $PID"
if [ -z "$PID" ]; then
    echo "ERROR: The proxy server does not started!"
    exit 1
fi

echo -e "Stopping the proxy server ...\c"
kill $PID > $STDOUT_FILE 2>&1

COUNT=0
while [ $COUNT -lt 1 ]; do    
    echo -e ".\c"
    sleep 1
    COUNT=1
    PID_EXIST=`ps -f -p $PID | grep java`
    if [ -n "$PID_EXIST" ]; then
        COUNT=0
    fi
done

echo "stopped"
echo "PID: $PID"
 