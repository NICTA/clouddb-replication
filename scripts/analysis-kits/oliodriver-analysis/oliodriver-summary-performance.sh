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

log_path_name=distill/oliodriver-summary-performance.csv
COLUMN_1="\"# Num of DB\""
COLUMN_2="\"# Concurrent Users\""
COLUMN_3="\"WR Metric\""
COLUMN_4="\"W Metric\""
COLUMN_5="\"R Metric\""
printf "%s\t%s\t%s\t%s\t%s\n" "$COLUMN_1" "$COLUMN_2" "$COLUMN_3" "$COLUMN_4" "$COLUMN_5" > $log_path_name

for i in $1/OlioDriver_*; do
  num_db=`echo $i | grep -o -E '[0-9]*Database' | grep -o -E '[0-9]*'`
  num_users=`echo $i | grep -o -E '[0-9]*User' | grep -o -E '[0-9]*'`
  wr_metric=`cat $i/summary.xml | grep "metric unit" | uniq | awk -F'[><]' '{print $3;}'`
  num_opt=(`cat $i/summary.xml | grep "successes" | awk -F'[><]' '{print $3;}'`)
  w_metric=0
  for i in {0..4}
  do
    w_metric=$(($w_metric + ${num_opt[$i]}))
  done
  w_metric=`echo $w_metric | awk '{ printf("%f",$1/300)}'`

  r_metric=0
  for i in {5..6}
  do
    r_metric=$(($r_metric + ${num_opt[$i]}))
  done
  r_metric=`echo $r_metric | awk '{ printf("%f",$1/300)}'`

  printf "%d\t%d\t%s\t%s\t%s\n" $num_db  $num_users $wr_metric $w_metric $r_metric >> $log_path_name
done