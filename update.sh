#!/bin/bash
/usr/bin/java -jar /home/han/Documents/xiaode-backend/target/xiaode.jar --json /home/han/Documents/xiaode-backend/database.json --kw /home/han/Documents/xiaode-backend/keywords.json
cd /home/han/Documents/xiaode-backend/
/usr/bin/git add database.json
/usr/bin/git add keywords.json
/usr/bin/git add update.sh
/usr/bin/git commit -m"regular update of database"
/usr/bin/git push
