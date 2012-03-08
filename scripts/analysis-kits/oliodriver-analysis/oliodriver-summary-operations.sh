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

log_path_name=distill/oliodriver-summary-operations.csv
COLUMN_1="\"# Num of DB\""
COLUMN_2="\"# Concurrent Users\""

COLUMN_3="\"HomePage avg resp\""
COLUMN_4="\"Login avg resp \""
COLUMN_5="\"TagSearch avg resp \""
COLUMN_6="\"EventDetail avg resp \""
COLUMN_7="\"PersonDetail avg resp \""
COLUMN_8="\"AddPerson avg resp \""
COLUMN_9="\"AddEvent avg resp \""

COLUMN_10="\"# HomePage \""
COLUMN_11="\"# Login \""
COLUMN_12="\"# TagSearch \""
COLUMN_13="\"# EventDetail \""
COLUMN_14="\"# PersonDetail \""
COLUMN_15="\"# AddPerson \""
COLUMN_16="\"# AddEvent \""

COLUMN_17="\"HomePage p90th resp\""
COLUMN_18="\"Login p90th resp \""
COLUMN_19="\"TagSearch p90th resp \""
COLUMN_20="\"EventDetail p90th resp \""
COLUMN_21="\"PersonDetail p90th resp \""
COLUMN_22="\"AddPerson  p90th resp \""
COLUMN_23="\"AddEvent p90th resp \""

printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
       "$COLUMN_1" "$COLUMN_2" "$COLUMN_3" "$COLUMN_4" \
       "$COLUMN_5" "$COLUMN_6" "$COLUMN_7" "$COLUMN_8" \
       "$COLUMN_9" "$COLUMN_10" "$COLUMN_11" "$COLUMN_12" \
       "$COLUMN_13" "$COLUMN_14" "$COLUMN_15" "$COLUMN_16" \
       "$COLUMN_17" "$COLUMN_18" "$COLUMN_19" "$COLUMN_20" \
       "$COLUMN_21" "$COLUMN_22" "$COLUMN_23" > $log_path_name

for i in $1/OlioDriver_*; do
  num_db=`echo $i | grep -o -E '[0-9]*Database' | grep -o -E '[0-9]*'`
  num_users=`echo $i | grep -o -E '[0-9]*User' | grep -o -E '[0-9]*'`
  avg_opt=(`cat $i/summary.xml | grep "avg" | awk -F'[><]' '{print $3;}' | sed 's/^\&gt\; //'`)
  p90th_opt=(`cat $i/summary.xml | grep "p90th" | awk -F'[><]' '{print $3;}' | sed 's/^\&gt\; //'`)
  num_opt=(`cat $i/summary.xml | grep "successes" | awk -F'[><]' '{print $3;}'`)

  printf "%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n" \
         $num_db  $num_users \
         ${avg_opt[0]} ${avg_opt[1]} ${avg_opt[2]} ${avg_opt[3]} ${avg_opt[4]} ${avg_opt[5]} ${avg_opt[6]} \
         ${num_opt[0]} ${num_opt[1]} ${num_opt[2]} ${num_opt[3]} ${num_opt[4]} ${num_opt[5]} ${num_opt[6]} \
         ${p90th_opt[0]} ${p90th_opt[1]} ${p90th_opt[2]} ${p90th_opt[3]} ${p90th_opt[4]} ${p90th_opt[5]} ${p90th_opt[6]} >> $log_path_name
done