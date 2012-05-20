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
  echo "This script takes addresses of MySQL instances, as well as number "
  echo "of concurrent users to deploy the test environment."
  echo ""
  echo "Usage:"
  echo "   ${0} [MySQL Running] [MySQL Paused] [Num_User]"
  exit 0
fi

MYSQL_INSTANCE_RUN="${1}"
MYSQL_INSTANCE_PAUSE="${2}"
NUM_OF_USER=${3}
NUM_OF_SCALE=${3}

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

# Deploy MySQL instance
echo "Start deploying MySQL instance (2/2)"
num_mysql=0
for mysql in $MYSQL_INSTANCE_RUN; do
  num_mysql=$[$num_mysql+1]
  # Copying my.cnf
  cp ./mysql-conf/my-raw.cnf my-raw_$num_mysql.cnf
  perl -p -i -e "s/#MYSQL_SERVER_ID#/$num_mysql/" my-raw_$num_mysql.cnf
  scp -r my-raw_$num_mysql.cnf root@$mysql:/etc/my.cnf
  rm my-raw_$num_mysql.cnf

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
  cp ./mysql-conf/my-raw.cnf my-raw_$num_mysql.cnf
  perl -p -i -e "s/#MYSQL_SERVER_ID#/$num_mysql/" my-raw_$num_mysql.cnf
  scp -r my-raw_$num_mysql.cnf root@$mysql:/etc/my.cnf
  rm my-raw_$num_mysql.cnf

  if [ "$NUM_OF_USER" -eq "1" ]; then
    num_scale=50
  else
    num_scale=$NUM_OF_SCALE
  fi
  deploy_paused_slave_database $mysql $master_mysql $num_scale &
done
wait
