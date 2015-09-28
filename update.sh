#!/bin/bash
HOME=/home/han
set -e
source $HOME/.keychain/${HOSTNAME}-sh
/usr/bin/java -jar /home/han/Documents/xiaode-backend/target/xiaode.jar --json /home/han/Documents/xiaode-backend/database.json --kw /home/han/Documents/xiaode-backend/keywords.json
/home/han/Documents/xiaode-backend/nohup.out
cd /home/han/Documents/xiaode-backend/
/usr/bin/git add database.json
/usr/bin/git add keywords.json
/usr/bin/git add update.sh
/usr/bin/git commit -m"regular update of database `date`"
/usr/bin/git push
