#!/bin/bash
set +e

cmd="$@"

# if mysql user is null, set default user root
if [[ ! -n $MYSQL_USER ]]; then
    # set default mysql user name root
	export MYSQL_USER=root
fi

# set mysql host, default 127.0.0.1
if [[ ! -n $MYSQL_HOST ]]; then
	export $MYSQL_HOST=127.0.0.1
fi

# set default port, default is 2181
if [[ ! -n $FLOW_ZOOKEEPER_PORT ]]; then
    FLOW_ZOOKEEPER_PORT=2181
fi

# set zookeeper is embedded or not, default is true
if [[ ! -n $FLOW_ZOOKEEPER_EMBEDDED ]]; then
    FLOW_ZOOKEEPER_EMBEDDED=true
fi

if [[ $FLOW_ZOOKEEPER_EMBEDDED = "true" ]]; then
	FLOW_ZOOKEEPER_HOST=$FLOW_API_DOMAIN
else
	if [[ ! -n $FLOW_ZOOKEEPER_HOST ]]; then
		echo "Because you select auto define zookeeper, Please set FLOW_ZOOKEEPER_HOST"
		exit 1;
	fi
fi


# set default port, default is 8080
if [[ ! -n $PORT ]]; then
	export PORT=8080
fi

# PASSWORD NOT NULL
if [[ ! -n $MYSQL_PASSWORD ]]; then
    echo "Please Set MYSQL_PASSWORD"
    exit;
fi

# update db user
read -r -d '' rootCreate <<-EOSQL || true
  use mysql;
  update user set password=PASSWORD('${MYSQL_PASSWORD}') where User='root';
  update user set plugin='mysql_native_password';
EOSQL

# detect mysql init or not
DATA_DIRECTORY=/var/lib/mysql
if [ "`ls -A $DATA_DIRECTORY`" = "" ]; then
  # init mysql data
  mysql_install_db
fi

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
  mysql --host=$MYSQL_HOST --user=$MYSQL_USER -e "${rootCreate}"
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

# Third running migration to update table structure
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