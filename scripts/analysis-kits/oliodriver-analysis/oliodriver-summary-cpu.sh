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
  echo "This script takes data folder as a parameter to generate data of"
  echo "metric information of benchmark."
  echo ""
  echo "Usage:"
  echo "   ${0} [Data folder]"
  exit 0;
fi

log_path_name=distill/oliodriver-summary-cpu.csv
COLUMN_1="\"# Num of DB\""
COLUMN_2="\"# Concurrent Users\""
COLUMN_3="\"Host name\""
COLUMN_4="\"Host type\""
COLUMN_5="\"Avg CPU (User)\""
COLUMN_6="\"Avg CPU (Sys)\""
COLUMN_7="\"Avg CPU (Total)\""
printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\n" "$COLUMN_1" "$COLUMN_2" "$COLUMN_3" "$COLUMN_4" "$COLUMN_5" "$COLUMN_6" "$COLUMN_7" > $log_path_name

for i in $1/OlioDriver_*; do
  num_db=`echo $i | grep -o -E '[0-9]*Database' | grep -o -E '[0-9]*'`
  num_users=`echo $i | grep -o -E '[0-9]*User' | grep -o -E '[0-9]*'`
  db_server_host=(`cat $i/META-INF/hosttypes | grep DbServer | awk '{print $1;}'`)

  for (( index=0; index < ${#db_server_host[*]}; index++ )) do
	cpu_user=`cat $i/post/vmstat.log.${db_server_host[$index]}.html | grep -A5 "(USR)" | grep '[0-9]' | awk '{printf $1;}'`
	cpu_sys=`cat $i/post/vmstat.log.${db_server_host[$index]}.html | grep -A5 "(SYS)" | grep '[0-9]' | awk '{printf $1;}'`
	cpu_total=`cat $i/post/vmstat.log.${db_server_host[$index]}.html | grep -A5 "(Total)" | grep '[0-9]' | awk '{printf $1;}'`

    if [ "$index" -eq "0" ]; then
      printf "%d\t%d\t%s\t%s\t%s\t%s\t%s\n" $num_db  $num_users ${db_server_host[$index]} "Master" $cpu_user $cpu_sys $cpu_total>> $log_path_name
    else
	  printf "%d\t%d\t%s\t%s\t%s\t%s\t%s\n" $num_db  $num_users ${db_server_host[$index]} "Slave" $cpu_user $cpu_sys $cpu_total>> $log_path_name
    fi
  done
done
