#!/bin/bash
set -e

cmd="$@"

read -r -d '' rootCreate <<-EOSQL
  SET @@SESSION.SQL_LOG_BIN=0;
  DELETE FROM mysql.user WHERE user NOT IN ('mysql.sys', 'mysqlxsys', 'root') OR host NOT IN ('localhost') ;
  SET PASSWORD FOR 'root'@'localhost'=PASSWORD('${MYSQL_PASSWORD}') ;
  GRANT ALL ON *.* TO 'root'@'localhost' WITH GRANT OPTION ;
  DROP DATABASE IF EXISTS test ;
  FLUSH PRIVILEGES ;
EOSQL

# Start Mysql
service mysql start

if [[ ${#MYSQL_HOST} -eq 0 ]];then
    echo "Please enter env MYSQL_HOST"
    exit;
fi

mysql --host=$MYSQL_HOST --user=$MYSQL_USER -e 'select version();' &> /dev/null;
isMysqlInit=$?
if [[ ${isMysqlInit} -eq 0 ]];then
  # mysql is init
  mysql --host=$MYSQL_HOST --user=$MYSQL_USER -e "use mysql;UPDATE user SET plugin='mysql_native_password' WHERE User='root';FLUSH PRIVILEGES;"
  mysql --host=$MYSQL_HOST --user=$MYSQL_USER -e "${rootCreate}"

  service mysql restart
fi

# First: waiting mysql up to do next cmd
# start monitor mysql start up or not every one second
until mysql --host=$MYSQL_HOST --user=$MYSQL_USER --password=$MYSQL_PASSWORD -e 'select version();' &> /dev/null; do
  >&2 echo "mysql is unavailable - retry 1s"
  sleep 1
done

>&2 echo "mysql is up"

# Second: Create database
mysql --host=$MYSQL_HOST --user=$MYSQL_USER --password=$MYSQL_PASSWORD -e 'Create Database If Not Exists flow_api_db Character Set UTF8;'
mysql --host=$MYSQL_HOST --user=$MYSQL_USER --password=$MYSQL_PASSWORD -e 'Create Database If Not Exists flow_cc_db Character Set UTF8;'

# Third running migration to update table stucture
MIGRATION_PATH=./migration

# use flyway control database structure update
echo "running migration"

# run migration to flow_api_db
/flyway/flyway -user=$MYSQL_USER -password=$MYSQL_PASSWORD -ignoreMissingMigrations=true -baselineOnMigrate=true -baselineVersion=1.0 -locations=filesystem:$MIGRATION_PATH/api -url=jdbc:mysql://$MYSQL_HOST:3306/flow_api_db migrate

# run migration to flow_cc_db
/flyway/flyway -user=$MYSQL_USER -password=$MYSQL_PASSWORD -ignoreMissingMigrations=true -baselineOnMigrate=true -baselineVersion=1.0 -locations=filesystem:$MIGRATION_PATH/cc -url=jdbc:mysql://$MYSQL_HOST:3306/flow_cc_db  migrate
echo "finish migration"


# Four: everything ready, to run tomcat
exec $cmd