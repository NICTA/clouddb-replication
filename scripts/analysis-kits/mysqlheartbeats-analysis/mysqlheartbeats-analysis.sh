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
  echo "This script takes data folder as a parameter to generate analysis"
  echo "of mysqlstats."
  echo ""
  echo "Usage:"
  echo "   ${0} [Data folder]"
  exit 0;
fi

declare -a host_time_diff

for i in $1/OlioDriver_*; do
  num_users=`echo $i | grep -o -E '[0-9]*User' | grep -o -E '[0-9]*'`
  db_server_host=(`cat $i/META-INF/hosttypes | grep DbServer | awk '{print $1;}'`)

  master_host=${db_server_host[0]}
  line_num_master=`cat $i/Mysqlheartbeat.log.$master_host | wc -l`
  master_time_id=(`sed -n "3,${line_num_master}p" $i/Mysqlheartbeat.log.$master_host | awk -F, '{print $1;}'`)
  master_timestamp=(`sed -n "3,${line_num_master}p" $i/Mysqlheartbeat.log.$master_host | awk -F, '{print $2;}'`)
  
  for ((index=1; index < ${#db_server_host[*]}; index++)) do
    log_path_name=distill/mysqlheartbeats-analysis-${#db_server_host[*]}-$num_users-${db_server_host[$index]}.csv
    if [ -f $log_path_name ]; then
      rm $log_path_name
    fi
    line_num_slave=`cat $i/Mysqlheartbeat.log.${db_server_host[$index]} | wc -l`
    slave_time_id=(`sed -n "3,${line_num_slave}p" $i/Mysqlheartbeat.log.${db_server_host[$index]} | awk -F, '{print $1;}'`)
    slave_timestamp=(`sed -n "3,${line_num_slave}p" $i/Mysqlheartbeat.log.${db_server_host[$index]} | awk -F, '{print $2;}'`)

    for (( indexj=0; indexj < ${#slave_timestamp[*]}; indexj++ )) do
      delay=`echo ${slave_timestamp[$indexj]} ${master_timestamp[$indexj]} | awk '{ printf("%f",($1-$2)/1000)}'`
	  echo -e ${master_time_id[$indexj]}"\t"${slave_time_id[$indexj]}"\t"$delay >> $log_path_name
	done
    for (( ; indexj < ${#master_timestamp[*]}; indexj++ )) do
      delay=`echo ${master_timestamp[$indexj]} ${master_timestamp[$(($indexj-1))]} | awk '{ printf("%f",($1-$2)/1000)}'`
	  echo -e ${master_time_id[$(($indexj-1))]}"\t"${master_time_id[$indexj]}"\t"$delay >> $log_path_name
	done
  done
done
