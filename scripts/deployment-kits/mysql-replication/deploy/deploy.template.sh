#!/bin/bash
#
#  Copyright 2011 National ICT Australia Limited
#
#  Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#Script to deploy and config MySQL and Faban with specified workloads

if [ "${#}" -lt "3" ]; then
  echo "This script takes addresses of Faban and MySQL instances, as"
  echo "well as number of Thin servers and concurrent users to deploy "
  echo "the test environment."
  echo ""
  echo "Usage:"
  echo "   ${0} [Faban] [MySQL] [Num_User]"
  exit 0
fi

FABAN_INSTANCE="${1}"
MYSQL_INSTANCE_RUN="${2}"
MYSQL_INSTANCE_PAUSE="${3}"
NUM_OF_USER=${4}
NUM_OF_SCALE=${4}
# Define the proxy and address if database is connected via load balancer, you must start
# a proxy (e.g. MySQL Proxy) before specifying this variable.
# After specifying PROXY_ADDRESS_PORT, you also need to modify script snippet in 
# deploy_master_faban function
PROXY_ADDRESS_PORT="FABAN-PROXY.us-west-1.compute.amazonaws.com:PORT"
# This variable is used to enable variable loads, once this value is set to true.
# Please make sure that a load.txt file is configured in this folder and also ensure
# that the scale of the run matches the largest thread count in the load variation file,
# and also the total time of the load variation matches the steady state time of the run
# More details can be found in 
# http://www.opensparc.net/sunsource/faban/www/1.0/docs/howdoi/loadvariation.html
VARIABLE_LOAD=false

REPL_PASSWORD=password
DIST_FOLDER=/root/mysql-data

deploy_database()
{
  # Copy a snapshot to slave
  ssh root@$1 "cp $DIST_FOLDER/mysql-$2.tar.bz2 /var/tmp/mysql.tar.bz2"
  ssh root@$1 "killall -w mysqld"

  # Restore databases
  ssh root@$1 "cd /usr/local/mysql/data && rm -rf * \
  && tar jxf /var/tmp/mysql.tar.bz2 \
  && rm /var/tmp/mysql.tar.bz2"
}

deploy_running_slave_database()
{
  deploy_database $1 $3

  # Stop slave and prepare logs
  master_mysql_log=`ssh root@$1 "cat $DIST_FOLDER/mysql-$3.tar.bz2.log"`
  master_mysql_log_file_name=`echo $master_mysql_log | awk '{print $1;}' | awk -F. '{print $1;}'`
  # Not really sure the reason, but each start of MySQL incrases log file number by 1
  # As we have to start MySQL master from scratch, thus we need to +1 based on the original logs
  # "10#" to fix the "value too great for base (error token is "08")" bug
  master_mysql_log_file_number="10#`echo $master_mysql_log | awk '{print $1;}' | awk -F. '{print $2;}'`"
  master_mysql_log_file_number=`echo $((master_mysql_log_file_number+1))`
  master_mysql_log_file=$master_mysql_log_file_name.`printf "%06d" $master_mysql_log_file_number` && \
  master_mysql_log_pos=`echo $master_mysql_log | awk '{print $2;}'` && \

  # Restart slave
  ssh root@$1 "/etc/init.d/mysql.server start" && \
  ssh root@$1 "mysql -u root -e \"STOP SLAVE; \
  CHANGE MASTER TO \
  MASTER_HOST = '$2', \
  MASTER_USER = 'repl', \
  MASTER_PASSWORD = '$REPL_PASSWORD', \
  MASTER_LOG_FILE = '$master_mysql_log_file', \
  MASTER_LOG_POS = $master_mysql_log_pos; \
  START SLAVE;\""

  # Error log link
  ssh root@$1 "ln -s /usr/local/mysql/data/\`hostname\`.err /usr/local/mysql/data/$mysql.err"
}

deploy_paused_slave_database()
{
  deploy_running_slave_database $1 $2 $3
  ssh root@$1 "killall -w mysqld"
}

deploy_master_database()
{
  deploy_database $1 $2

  ssh root@$1 "/etc/init.d/mysql.server start"
  ssh root@$1 "mysql -u root -e \"UNLOCK TABLES;\""
  ssh root@$1 "mysql -u root -e \"ALTER TABLE heartbeats.heartbeats ADD id INT PRIMARY KEY AUTO_INCREMENT;\""
  ssh root@$1 "mysql -u root -e \"ALTER TABLE heartbeats.heartbeats ENGINE = MEMORY;\""

  # Error log link
  ssh root@$1 "ln -s /usr/local/mysql/data/\`hostname\`.err /usr/local/mysql/data/$mysql.err"
}

deploy_master_faban()
{
  # Prepare profile for Faban master
  num_host_list=0
  for mysql in $MYSQL_INSTANCE_RUN; do
    db_host_list=$db_host_list"$mysql "
    num_host_list=$[$num_host_list+1]
  done
  for mysql in $MYSQL_INSTANCE_PAUSE; do
    db_host_list=$db_host_list"$mysql "
    num_host_list=$[$num_host_list+1]
  done
  db_host_list=`echo $db_host_list | sed -e 's/,$//'`

  for mysql in $MYSQL_INSTANCE_RUN; do
    db_serv_list=$db_serv_list"$mysql,"
  done
  if [ "$num_host_list" -eq "1" ]; then
    db_serv_list=$db_serv_list"$mysql,"
  fi
  db_serv_list=`echo $db_serv_list | sed -e 's/,$//'`

  num_agent=0
  for agent in $FABAN_INSTANCE; do
    num_agent=$[$num_agent+1]
    agent_serv_list=$agent_serv_list"$agent "
  done
  agent_serv_list=`echo $agent_serv_list | sed -e 's/,$//'`

  cp run.xml.OlioDriver.template run.xml.OlioDriver && \
  perl -p -i -e "s/#FABAN_HOST#/$agent_serv_list/" run.xml.OlioDriver && \
  perl -p -i -e "s/#NUM_OF_USER#/$NUM_OF_USER/" run.xml.OlioDriver && \
  perl -p -i -e "s/#NUM_OF_SCALE#/$NUM_OF_SCALE/" run.xml.OlioDriver && \
  perl -p -i -e "s/#DATABASE_HOST#/$db_host_list/" run.xml.OlioDriver && \
  perl -p -i -e "s/#NUM_OF_AGENT#/$num_agent/" run.xml.OlioDriver && \
  ###
  # The following three lines for replacing
  perl -p -i -e "s/#JDBC_DRIVER#/com.mysql.jdbc.ReplicationDriver/" run.xml.OlioDriver && \
  perl -p -i -e "s/#JDBC_CONNECTOR#/jdbc:mysql:replication:/" run.xml.OlioDriver && \
  perl -p -i -e "s/#DATABASE_SERVER#/$db_serv_list/" run.xml.OlioDriver && \
  ###
  # Script snippet for using com.mysql.jdbc.ReplicationDriver driver
  ## perl -p -i -e "s/#JDBC_DRIVER#/com.mysql.jdbc.ReplicationDriver/" run.xml.OlioDriver && \
  ## perl -p -i -e "s/#JDBC_CONNECTOR#/jdbc:mysql:replication:/" run.xml.OlioDriver && \
  ## perl -p -i -e "s/#DATABASE_SERVER#/$db_serv_list/" run.xml.OlioDriver && \
  ###
  # Script snippet for using com.mysql.jdbc.Driver driver
  ## perl -p -i -e "s/#JDBC_DRIVER#/com.mysql.jdbc.Driver/" run.xml.OlioDriver && \
  ## perl -p -i -e "s/#JDBC_CONNECTOR#/jdbc:mysql:/" run.xml.OlioDriver && \
  ## perl -p -i -e "s/#DATABASE_SERVER#/$PROXY_ADDRESS_PORT/" run.xml.OlioDriver && \
  ###
  perl -p -i -e "s/#VARIABLE_LOAD#/$VARIABLE_LOAD/" run.xml.OlioDriver

  ssh root@$1 "mkdir ~/faban/config/profiles/dbadmin"
  scp -r run.xml.OlioDriver root@$1:~/faban/config/profiles/dbadmin/run.xml.OlioDriver && \
  rm run.xml.OlioDriver
  scp -r load.txt.template root@$1:/tmp/load.txt
  # Staring Faban system
  ssh root@$1 "./faban/master/bin/startup.sh"
}

deploy_agent_faban()
{
  # Staring Faban system
  scp -r load.txt.template root@$1:/tmp/load.txt
  ssh root@$1 "./faban/bin/agent"
}

# Deploy Faban system
echo "Start deploying Faban instance (1/2)"
num_agent=0
for agent in $FABAN_INSTANCE; do
  num_agent=$[$num_agent+1]
  if [ "$num_agent" -eq "1" ]; then
    # Initializing MySQL databases
    master_faban=$agent
    deploy_master_faban $agent &
  else
    deploy_agent_faban $agent &
  fi
done
wait

# Deploy MySQL instance
echo "Start deploying MySQL instance (2/2)"
num_mysql=0
for mysql in $MYSQL_INSTANCE_RUN; do
  num_mysql=$[$num_mysql+1]
  # Copying my.cnf
  cp my.cnf.template my_$num_mysql.cnf
  perl -p -i -e "s/#MYSQL_SERVER_ID#/$num_mysql/" my_$num_mysql.cnf
  scp -r my_$num_mysql.cnf root@$mysql:/etc/my.cnf
  rm my_$num_mysql.cnf

  if [ "$NUM_OF_USER" -eq "1" ]; then
    num_scale=50
  else
    num_scale=$NUM_OF_SCALE
  fi

  if [ "$num_mysql" -eq "1" ]; then
    # Initializing MySQL databases
    master_mysql=$mysql
    deploy_master_database $master_mysql $num_scale &
  else
    deploy_running_slave_database $mysql $master_mysql $num_scale &
  fi
done

for mysql in $MYSQL_INSTANCE_PAUSE; do
  num_mysql=$[$num_mysql+1]
  # Copying my.cnf
  cp my.cnf.template my_$num_mysql.cnf
  perl -p -i -e "s/#MYSQL_SERVER_ID#/$num_mysql/" my_$num_mysql.cnf
  scp -r my_$num_mysql.cnf root@$mysql:/etc/my.cnf
  rm my_$num_mysql.cnf

  if [ "$NUM_OF_USER" -eq "1" ]; then
    num_scale=50
  else
    num_scale=$NUM_OF_SCALE
  fi
  deploy_paused_slave_database $mysql $master_mysql $num_scale &
done
wait

# Check master and slave databases
num_mysql=0
for mysql in $MYSQL_INSTANCE_RUN; do
  num_mysql=$[$num_mysql+1]
  if [ "$num_mysql" -eq "1" ]; then
    echo "MySQL database id: $num_mysql (Master) - $mysql"
  else
    echo "MySQL database id: $num_mysql (Running Slave) - $mysql"
  fi
done
for mysql in $MYSQL_INSTANCE_PAUSE; do
  num_mysql=$[$num_mysql+1]
  echo "MySQL database id: $num_mysql (Paused Slave) - $mysql"
done

echo -e ""
echo -e "Deployment finished .."
echo -e "   Please visit following website to start testing."
echo -e "   http://$master_faban:9980"
