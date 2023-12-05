#!/usr/bin/env bash

sleep 10

mongosh db-1:27017/admin --file /ws/mongo_init_rs.js
mongosh db-1:27017/admin --file /ws/mongo_init_auth.js
