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


if [ "${#}" -ne "1" ]
then
  echo "This script takes address of MySQL instance as a parameter to generate data "
  echo "size of workload archived."
  echo ""
  echo "Usage:"
  echo "   ${0} [MySQL address]"
  exit 0;
fi

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


deploy_master_database()
{
  deploy_database $1 $2

  ssh root@$1 "/etc/init.d/mysql.server start"
  ssh root@$1 "mysql -u root -e \"UNLOCK TABLES;\""
}

log_path_name=distill/mysqldatabase-datasize.csv
workload_list=(`ssh root@$1 "ls mysql-data/mysql-*.tar.bz2 | grep -o -E '[0-9][0-9][0-9]*' | sort -n -k 1 | uniq"`)

COLUMN_1="\"# of Workloads\""
COLUMN_2="\"Database size\""
COLUMN_3="\"Data size\""
COLUMN_4="\"Index size\""
COLUMN_5="\"# of Users\""
printf "%s\t%s\t%s\t%s\t%s\n" "$COLUMN_1" "$COLUMN_2" "$COLUMN_3" "$COLUMN_4" "$COLUMN_5" > $log_path_name

for ((windex=1; windex < ${#workload_list[*]}; windex++)) do
  deploy_master_database $1 ${workload_list[$windex]} > /dev/null 2>&1
  printf "%s\t" "${workload_list[$windex]}" >> $log_path_name
  echo -e \
  `ssh root@$1 "mysql -N -e \"SELECT sum( data_length + index_length ) / 1024 / 1024, sum( data_length )/ 1024 / 1024, sum( index_length )/ 1024 / 1024 FROM information_schema.TABLES WHERE table_schema like '%olio%' GROUP BY table_schema;\""` \
  `ssh root@$1 "mysql -N -e \"SELECT count(*) FROM olio.users;\""`>>  $log_path_name
done