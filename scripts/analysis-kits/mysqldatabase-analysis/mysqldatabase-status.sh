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

log_path_name=distill/mysqldatabase-status.csv
echo -e "# Concurrent Users\tqcache_hit_ratio\ttransactions\ttransactions_per_second\tthroughput\tthroughput_per_second\tread_ratio" > $log_path_name

declare -a data_array

for i in $1/OlioDriver_*; do
  temp_key=`echo $i | grep -o -E '[0-9]*User' | grep -o -E '[0-9]*'`
  db_server_host=(`cat $i/META-INF/hosttypes | grep DbServer | awk '{print $1;}'`)
  temp_value=0
  questions=0
  seconds=0
  com_commit=0
  com_rollback=0
  com_select=0
  qcache_hits=0
  com_insert=0
  com_update=0
  com_delete=0
  com_replace=0
  com_heartbeat=0
  for ((index=0; index < ${#db_server_host[*]}; index++)) do
    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Questions" | awk '{print $2;}'`
    questions=`echo $questions $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Uptime" | awk '{print $2;}'`
    seconds=`echo $seconds $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_commit" | awk '{print $2;}'`
    if  [ "$temp_value" == "" ]; then
      temp_value1=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_insert" | awk '{print $2;}'`
      com_heartbeat=`echo $com_heartbeat $temp_value1 | awk '{ printf("%f",$1+$2)}'`
    else
      temp_value1=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_insert" | awk '{print $2;}'`
      com_insert=`echo $com_insert $temp_value1 | awk '{ printf("%f",$1+$2)}'`
    fi
    com_commit=`echo $com_commit $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_rollback" | awk '{print $2;}'`
    com_rollback=`echo $com_rollback $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_select" | awk '{print $2;}'`
    com_select=`echo $com_select $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Qcache_hits" | awk '{print $2;}'`
    qcache_hits=`echo $qcache_hits $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_update" | awk '{print $2;}'`
    com_update=`echo $com_update $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_delete" | awk '{print $2;}'`
    com_delete=`echo $com_delete $temp_value | awk '{ printf("%f",$1+$2)}'`

    temp_value=`cat $i/Mysqlstats.log.${db_server_host[$index]} | grep "Com_replace" | awk '{print $2;}'`
    com_replace=`echo $com_replace $temp_value | awk '{ printf("%f",$1+$2)}'`
  done

  avg_seconds=`echo $seconds $index | awk '{ printf("%f",$1/$2)}'`
  qcache_hit_ratio=`echo $qcache_hits $com_select | awk '{ printf("%f",$1/($1+$2)*100)}'`
  transactions=`echo $com_commit $com_rollback | awk '{ printf("%f",$1+$2)}'`
  transactions_per_second=`echo $transactions $avg_seconds | awk '{ printf("%f",$1/$2)}'`
  throughput=`echo $com_select $qcache_hits $com_insert $com_update $com_delete $com_replace | awk '{ printf("%f",($1+$2+$3+$4+$5+$6))}'`
  throughput_per_second=`echo $throughput $avg_seconds | awk '{ printf("%f",$1/$2)}'`
  read_ratio=`echo $com_select $qcache_hits $throughput | awk '{ printf("%f",($1+$2)/$3*100)}'`
  data_array[$temp_key]="\t"$qcache_hit_ratio"\t"$transactions"\t"$transactions_per_second"\t"$throughput"\t"$throughput_per_second"\t"$read_ratio
done

for i in ${!data_array[*]}; do
  echo -e $i"\t"${data_array[$i]} >> $log_path_name
done
