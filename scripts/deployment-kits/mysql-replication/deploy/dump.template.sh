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
#Script to auto generate dump of MySQL database for various workloads, 
#all dumps are compressed and archived.

if [ "${#}" -lt "1" ]; then
  echo "This script takes addresses of MySQL master database and number "
  echo "of concurrent users to generate necessary data dumps."
  echo ""
  echo "Usage:"
  echo "   ${0} [MySQL]"
  exit 0
fi

MASTER_INSTANCE="${1}"
NUM_OF_USERS=( "50" "100" "150" "200" \
              "250" "300" "350" "400" \
              "450" "500" "550" "600" \
			  "650" "700" "750" "800")

REPL_PASSWORD=password
DIST_FOLDER=/root/mysql-data
DISK_DISK=/dev/sdf5

# Initializing MySQL databases
master_mysql=$MASTER_INSTANCE

generate_database_dump()
{
  num_mysql=1
  # Copying my.cnf
  cp my.cnf.template my_$num_mysql.cnf
  perl -p -i -e "s/#MYSQL_SERVER_ID#/$num_mysql/" my_$num_mysql.cnf
  scp -r my_$num_mysql.cnf root@$master_mysql:/etc/my.cnf
  rm my_$num_mysql.cnf
  
  # Kill all mysqld
  ssh root@$1 "killall -w mysqld"
  # Remove all exisiting data
  ssh root@$1 "rm -rf /usr/local/mysql/data/*"
  ssh root@$1 "cd /usr/local/mysql \
  && ./scripts/mysql_install_db --basedir=/usr/local/mysql --user=mysql \
  && /etc/init.d/mysql.server start"

  # Create replication and olio account
  ssh root@$1 "/etc/init.d/mysql.server restart" && \
  ssh root@$1 "mysql -u root -e \"DELETE FROM mysql.user WHERE user = '';\" \
  && mysql -u root -e \
  \"CREATE USER 'repl'@'%' IDENTIFIED BY '$REPL_PASSWORD';\" \
  && mysql -u root -e \
  \"GRANT REPLICATION SLAVE ON *.* TO repl@'%' IDENTIFIED BY '$REPL_PASSWORD';\" \
  && mysql -u root -e \
  \"CREATE USER 'olio'@'%' IDENTIFIED BY 'olio';\" \
  && mysql -u root -e \
  \"GRANT ALL PRIVILEGES ON *.* TO 'olio'@'%' IDENTIFIED BY 'olio';\" \
  && mysql -u root -e \"FLUSH PRIVILEGES;\""

  # Create Olio database
  ssh root@$1 "mysql -u root -e \
  \"CREATE DATABASE IF NOT EXISTS olio;\"" && \
  ssh root@$1 "mysql -u root -e \
  \"CREATE DATABASE IF NOT EXISTS heartbeats;\"" && \
  ssh root@$1 "cd /var/app/olio \
  && /var/lib/gems/1.8/bin/rake db:migrate"
  # Generate Olio data
  ssh root@$1 "mkdir ~/faban/benchmarks/OlioDriver"
  scp -r ../../packages/OlioDriver.jar root@$1:~/faban/benchmarks/OlioDriver/OlioDriver.jar
  ssh root@$1 "cd ~/faban/benchmarks/OlioDriver \
  && jar xf OlioDriver.jar \
  && chmod +x ~/faban/benchmarks/OlioDriver/bin/*.*"
  ssh root@$1 "cd ~/faban/benchmarks/OlioDriver/bin \
  && ./dbloader.sh localhost $2"

  # Create microsec function
  ssh root@$1 "mysql -u root -e \
  \"CREATE FUNCTION now_microsec RETURNS STRING SONAME 'now_microsec.so';\""
  # Implement heartbeats table
  ssh root@$1 "mysql -uolio -polio -e \
  \"CREATE TABLE IF NOT EXISTS heartbeats.heartbeats(sys_mill CHAR(26), db_micro CHAR(26)) ENGINE = MEMORY;\""

  # Flush all logs
  ssh root@$1 "mysqladmin flush-logs"

  # Snapshot databases in master
  master_mysql_log=`ssh root@$1 "mysql -u root -e \
  \"FLUSH TABLES WITH READ LOCK; \
  SHOW MASTER STATUS;\"" | grep mysql-bin`
  ssh root@$1 "mysqladmin shutdown"
  ssh root@$1 "tar jcf $DIST_FOLDER/mysql.tar.bz2 -C /usr/local/mysql/data/ ."

  # Delete snapshot after copying to slaves
  ssh root@$1 "mv $DIST_FOLDER/mysql.tar.bz2 $DIST_FOLDER/mysql-$2.tar.bz2"
  ssh root@$1 "echo $master_mysql_log > $DIST_FOLDER/mysql-$2.tar.bz2.log"
}

ssh root@$1 "mount $DISK_DISK $DIST_FOLDER/"
ssh root@$1 "rm $DIST_FOLDER/mysql-*"
# Deploy MySQL instance
for ((i=0; i < ${#NUM_OF_USERS[*]}; i++)) do
  echo "($[i+1]/${#NUM_OF_USERS[*]}) Start deploying MySQL instance for ${NUM_OF_USERS[$i]} users on `date` ..."
  generate_database_dump $master_mysql ${NUM_OF_USERS[$i]} > /dev/null 2>&1
done
ssh root@$1 "rm -rf /usr/local/mysql/data/*"
