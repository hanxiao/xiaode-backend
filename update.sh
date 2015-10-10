#!/bin/bash
HOME=/home/han
/usr/bin/java -jar /home/han/Documents/xiaode-backend/target/xiaode.jar --json /home/han/Documents/xiaode-backend/database.json --kw /home/han/Documents/xiaode-backend/keywords.json
cp /home/han/Documents/xiaode-backend/*.json /home/han/Documents/hanxiao.github.io/data/
cd /home/han/Documents/hanxiao.github.io/
/usr/bin/git add data/database.json
/usr/bin/git add data/keywords.json
/usr/bin/git add index.html
/usr/bin/git commit -m"regular update of database `date`"
/usr/bin/git push

