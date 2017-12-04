#!/bin/bash
set -e

cmd="$@"

# First: waiting mysql up to do next cmd
# start monitor mysql start up or not every one second
until mysql --host=db --user=$MYSQL_USER --password=$MYSQL_PASSWORD -e 'select version();' &> /dev/null; do
  >&2 echo "mysql is unavailable - retry 1s"
  sleep 1
done

>&2 echo "mysql is up"

# Second: Create database
mysql --host=db --user=$MYSQL_USER --password=$MYSQL_PASSWORD -e 'Create Database If Not Exists flow_api_db Character Set UTF8;'
mysql --host=db --user=$MYSQL_USER --password=$MYSQL_PASSWORD -e 'Create Database If Not Exists flow_cc_db Character Set UTF8;'

# Third running migration to update table stucture
MIGRATION_PATH=./migration

# use flyway control database structure update
echo "running migration"

# run migration to flow_api_db
/flyway/flyway -user=$MYSQL_USER -password=$MYSQL_PASSWORD -ignoreMissingMigrations=true -baselineOnMigrate=true -baselineVersion=1.0 -locations=filesystem:$MIGRATION_PATH/api -url=jdbc:mysql://db:3306/flow_api_db migrate

# run migration to flow_cc_db
/flyway/flyway -user=$MYSQL_USER -password=$MYSQL_PASSWORD -ignoreMissingMigrations=true -baselineOnMigrate=true -baselineVersion=1.0 -locations=filesystem:$MIGRATION_PATH/cc -url=jdbc:mysql://db:3306/flow_cc_db  migrate
echo "finish migration"


# Four: everything ready, to run tomcat
exec $cmd