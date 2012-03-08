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
  echo "This script takes data folder of one experiment as a parameter to generate "
  echo "runtime of operations in benchmark."
  echo ""
  echo "Usage:"
  echo "   ${0} [Data folder of one experiment]"
  exit 0;
fi

log_path_name=distill/oliodriver-detail-operations.csv
temp=content.tmp

COLUMN_1="\"Operation\""
COLUMN_2="\"Start time\""
printf "%s\t%s\n" "$COLUMN_1" "$COLUMN_2" > $log_path_name

start_line=`cat $1/log.xml | grep -n "Ramp up completed" | sed -n 's/^\([0-9]*\)[:].*/\1/p'`
end_line=`cat $1/log.xml | grep -n "Steady state completed" | sed -n 's/^\([0-9]*\)[:].*/\1/p'`
sed -n ${start_line},${end_line}p log.xml | sed 's/\./ /g' | grep ": Invoking " | \
awk '{ print $4 "\t" $7 }' > $temp
start_time=`sed -n 1,1p $temp | awk '{print $2}'`
awk -v s=$start_time '{ print $1 "\t" ($2-s)/1000000000 }' $temp >> $log_path_name
rm $temp