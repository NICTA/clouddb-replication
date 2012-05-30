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
MYSQL_DATA_SOURCE="ec2-50-18-133-245.us-west-1.compute.amazonaws.com"
NUM_OF_USER=${3}
NUM_OF_SCALE=${3}

RDS_USER=olio
RDS_PASSWORD=olioolio
DIST_FOLDER=/root/mysql-data

deploy_database()
{
  # Drop exisiting databases
  mysql -u${RDS_USER} -p${RDS_PASSWORD} -h $1 -e "DROP DATABASE olio;"
  mysql -u${RDS_USER} -p${RDS_PASSWORD} -h $1 -e "DROP DATABASE heartbeats;"

  # Create databases
  mysql -u${RDS_USER} -p${RDS_PASSWORD} -h $1 -e "CREATE DATABASE IF NOT EXISTS olio;"
  mysql -u${RDS_USER} -p${RDS_PASSWORD} -h $1 -e "CREATE DATABASE IF NOT EXISTS heartbeats;"
  mysql -u${RDS_USER} -p${RDS_PASSWORD} -h $1 -e \
  "CREATE TABLE IF NOT EXISTS heartbeats.heartbeats(sys_mill CHAR(26), db_micro CHAR(26)) ENGINE = MEMORY;"
  mysql -u${RDS_USER} -p${RDS_PASSWORD} -h $1 -e \
  "ALTER TABLE heartbeats.heartbeats ADD id INT PRIMARY KEY AUTO_INCREMENT;"
}

deploy_master_database()
{
  deploy_database $1

  # Import SQL dump from source to the master
  scp root@$MYSQL_DATA_SOURCE:$DIST_FOLDER/olio-$2.sql.gz /var/tmp/olio.sql.gz
  gunzip < /var/tmp/olio.sql.gz | mysql -u${RDS_USER} -p${RDS_PASSWORD} -h $1
}

# Deploy MySQL instance
num_mysql=0
for mysql in $MYSQL_INSTANCE_RUN; do
  num_mysql=$[$num_mysql+1]

  if [ "$NUM_OF_USER" -eq "1" ]; then
    num_scale=50
  else
    num_scale=$NUM_OF_SCALE
  fi

  if [ "$num_mysql" -eq "1" ]; then
    # Initializing MySQL databases
    master_mysql=$mysql
    deploy_master_database $master_mysql $num_scale &
  fi
done
wait

# Sleep $NUM_OF_SCALE seconds so that data can be synced
sleep $(( $NUM_OF_SCALE / 2 ))
