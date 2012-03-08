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
	echo "This script takes folder of results of mysqlheartbeats as a parameter "
	echo "to generate analysis of percentile results."
	echo ""
	echo "Usage:"
	echo "   ${0} [Data folder]"
	exit 0;
fi

log_path_name=$1/mysqlheartbeats-average-by-run_pct.csv
temp_file=temp
temp_cdf=cdf

rm $log_path_name

num_db_list=(`ls $1/mysqlheartbeats-analysis-* | grep -o -E 'analysis\-[0-9]*\-' | awk -F- '{print $2;}' | sort | uniq`)
workload_list=(`ls $1/mysqlheartbeats-analysis-* | grep -o -E '\-[0-9]*\-[id]' | awk -F- '{print $2;}' | sort -n -k 2 | uniq`)

printf "%s\t%s\t%s\n" "#num_db" "workload" "avg" >> $log_path_name
for ((nindex=0; nindex < ${#num_db_list[*]}; nindex++)) do
	for ((windex=0; windex < ${#workload_list[*]}; windex++)) do
		instance_list=(`ls $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-${workload_list[$windex]}-* | grep -o -E [ipdomU]+\-[A-Z0-9\-]* | sort -n -k 2 | uniq`)
		for ((iindex=0; iindex < ${#instance_list[*]}; iindex++)) do
			cat $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-${workload_list[$windex]}-${instance_list[$iindex]}.csv | awk '{last=$3; if ($1==$2) print $3; else total+=$3;} END {if (total!=0 && total!=last) print (total-last)}' > $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-${workload_list[$windex]}-${instance_list[$iindex]}_comb.csv
			./misc/mysqlheartbeats-cdf.py $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-${workload_list[$windex]}-${instance_list[$iindex]}_comb.csv > $temp_cdf
			five_pct=`cat $temp_cdf | grep -m 1 "^0.05" | awk '{print $2;}'`
			ninefive_pct=`cat $temp_cdf | grep -m 1 "^0.95" | awk '{print $2;}'`
			rm $temp_cdf
			cat $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-${workload_list[$windex]}-${instance_list[$iindex]}_comb.csv | awk -v "five=$five_pct" -v "ninefive=$ninefive_pct" '{if ($1>five && $1<ninefive) {total+=$1; count+=1;}} END {print total, count, total/count}' > $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-${workload_list[$windex]}-${instance_list[$iindex]}_stat.csv
		done
	done
	avg_nowork=`cat $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-1-*_stat.csv | awk '{total+=$1; count+=$2;} END {print total/count}'`
	for ((windex=1; windex < ${#workload_list[*]}; windex++)) do
		printf "%s\t%s\t" "${num_db_list[$nindex]}" "${workload_list[$windex]}" >> $log_path_name
		avg=`cat $1/mysqlheartbeats-analysis-${num_db_list[$nindex]}-${workload_list[$windex]}-*_stat.csv | awk '{total+=$1; count+=$2;} END {print total/count}'`
		echo $avg $avg_nowork | awk '{ printf("%f\n",$1-$2)}' >> $log_path_name
	done
done
