#!/bin/bash
HOME=/home/han
AGENT="ssh-agent -s"
if [ ! -d $HOME/.ssh/agent ]; then
        mkdir -p $HOME/.ssh/agent
fi
#
# Start an agent if there isn't one running already.
#
pid=`ps -u$LOGNAME | grep ssh-age | awk '{print $1}'`
if [ -z "$pid" ]; then
        $AGENT | grep -v echo > $HOME/.ssh/agent/$HOST & pid=$!
        sleep 1 # Let it fork and stuff
fi
ssh-add $HOME/.ssh/id_dsa
#
# Get our parent to pick up the required SSH env vars.
#
. $HOME/.ssh/agent/$HOST
/usr/bin/java -jar /home/han/Documents/xiaode-backend/target/xiaode.jar --json /home/han/Documents/xiaode-backend/database.json --kw /home/han/Documents/xiaode-backend/keywords.json
cd /home/han/Documents/xiaode-backend/
/usr/bin/git add database.json
/usr/bin/git add keywords.json
/usr/bin/git add update.sh
/usr/bin/git commit -m"regular update of database `date`"
/usr/bin/git push
