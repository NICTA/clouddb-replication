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
#Script to autorun everything by calling install, deploy, run and data 
#collect scripts. Feel free to disable some scripts if they are not 
#necessary in your experiments.
#
#
#########################################################
#
#The script uses root + ssh key to access EC2 instances.
#So always run scripts in an experimental environment.
#Make sure to check each script for NOTE, in case of any
#conflicts to your system.
#
#########################################################
#
#NOTE:
#1. It is suggested to refer to Cloudstone/Faban for configuration details
#   http://www.opensparc.net/sunsource/faban/www/
#2. The MYSQL_INSTANCE_PAUSE is used for CloudDB AutoAdmin, it is suggested 
#   to kept it commented or empty if you only need to run our benchmark.
#3. 7-zip is pre-required for the data archieve.
#5. To integerated with CloudDB AutoAdmin, you have to enable 
#   i.  MYSQL_INSTANCE_PAUSE in this file
#   ii. Replace JDBC driver in the middle, and PROXY_ADDRESS_PORT at the 
#       beginning, of the deploy.sh
#   iii.Variable workload can be enabled in deploy.sh, with 
#       specification in load.txt.
#       http://www.opensparc.net/sunsource/faban/www/1.0/docs/howdoi/loadvariation.html
#   iv. Match replication heartbeat with heartbeat interval in CloudDB 
#       Autoadmin configure.
#4. To change replication heartbeat frequency, edit interval-write value in 
#   deploy/faban-deploy.sh
#5. To use MySQL build-in time/date function, edit interval-read value to 
#   negative, or use positive for customized time/date function in 
#   deploy/faban-deploy.sh
#6. To use SQL data deploy, you need to specify MYSQL_DATA_SOURCE in 
#   deploy/mysql-sql-deploy.sh

LOCATION=`pwd`

check_errs()
{
  # Function. Parameter 1 is the return code
  # Para. 2 is text to display on failure.
  if [ "${1}" -ne "0" ]; then
    echo "ERROR # ${1} : ${2}"
    # as a bonus, make our script exit with the right error code.
    exit ${1}
  fi
}

FABAN_INSTANCE=("FABAN1.us-west-1.compute.amazonaws.com" \
    			"FABAN2.us-west-1.compute.amazonaws.com")
MYSQL_INSTANCE_RUN=("MYSQL1.us-west-1.compute.amazonaws.com" \
	                "MYSQL2.us-west-1.compute.amazonaws.com")
MYSQL_INSTANCE_PAUSE=("MYSQL3.us-west-1.compute.amazonaws.com")
NUM_OF_USERS=("15" "30" "45" "60" "75" "85" "95" "110" "125" "140" "155")
ARCHIVE_PATH="Downloads"
STEP=1

# PLATFORM can use "ec2" or "rds"
# DATA_FORMAT can use "sql" or "raw"
# The combination should be "rds-sql" or "ec2-sql" or "ec2-raw",
# There is NO "rds-raw"
DATA_FORMAT=sql
PLATFORM=ec2

DATABASE_USER=olio
DATABASE_PASSWORD=olio

echo "Experiments start at `date`"
rm ~/.ssh/known_hosts > /dev/null 2>&1

faban_instance_all=${FABAN_INSTANCE[0]}
for ((k=1; k<${#FABAN_INSTANCE[*]}; k++)) do
	faban_instance_all=$faban_instance_all" "${FABAN_INSTANCE[$k]}
done
mysql_instance_all=${MYSQL_INSTANCE_RUN[0]}
for ((k=1; k<${#MYSQL_INSTANCE_RUN[*]}; k++)) do
	mysql_instance_all=$mysql_instance_all" "${MYSQL_INSTANCE_RUN[$k]}
done
mysql_instance_pause=${MYSQL_INSTANCE_PAUSE[0]}
mysql_instance_all=$mysql_instance_all" "${MYSQL_INSTANCE_PAUSE[0]}
for ((k=1; k<${#MYSQL_INSTANCE_PAUSE[*]}; k++)) do
	mysql_instance_all=$mysql_instance_all" "${MYSQL_INSTANCE_PAUSE[$k]}
	mysql_instance_pause=$mysql_instance_pause" "${MYSQL_INSTANCE_PAUSE[$k]}
done

echo "Start installing Faban instance (1/2)"
cd "$LOCATION/install" && ./faban-install.sh "$faban_instance_all" > /dev/null 2>&1
cd "$LOCATION/update" && ./faban-update.sh "$faban_instance_all" > /dev/null 2>&1
echo "Start installing MySQL instance (2/2)"
cd "$LOCATION/install" && ./mysql-install.sh "$mysql_instance_all" > /dev/null 2>&1
cd "$LOCATION/update" && ./mysql-update.sh "$mysql_instance_all" > /dev/null 2>&1
cd "$LOCATION/update" && ./mysql-${DATA_FORMAT}-${PLATFORM}-update.sh "$mysql_instance_run" "$mysql_instance_pause" > /dev/null 2>&1
check_errs $? "Install instances failed."

for ((k=${#MYSQL_INSTANCE_RUN[*]}; k>=1; k=$[$k-$STEP])) do
	mysql_instance_run=${MYSQL_INSTANCE_RUN[0]}
	for ((m=1; m < k; m++)) do
		mysql_instance_run=$mysql_instance_run" "${MYSQL_INSTANCE_RUN[$m]}
	done
	echo "Installing Faban and MySQL instances for running ${#NUM_OF_USERS[*]} benchmarks ..."
	for ((i=0; i < ${#NUM_OF_USERS[*]}; i++)) do
		echo "Running Benchmark ($[i+1]/${#NUM_OF_USERS[*]}) ..."
		# Deploy instances
		
		echo ".. (1/3) Deploy Faban and MySQL instances for ${NUM_OF_USERS[$i]} concurrent users"
        echo "Start deploying Faban instance (1/2)"
		cd "$LOCATION/deploy" && ./faban-deploy.sh "$faban_instance_all" "$mysql_instance_run" "$mysql_instance_pause" ${NUM_OF_USERS[$i]} $DATABASE_USER $DATABASE_PASSWORD > /dev/null 2>&1
        echo "Start deploying MySQL instance (2/2)"
		cd "$LOCATION/deploy" && ./mysql-${DATA_FORMAT}-deploy.sh "$mysql_instance_run" "$mysql_instance_pause" ${NUM_OF_USERS[$i]} $DATABASE_USER $DATABASE_PASSWORD > /dev/null 2>&1
		check_errs $? "Deploy instances failed."
		
		# Start test from command line
		cd "$LOCATION"
		task_name=`ssh root@${FABAN_INSTANCE[0]} "ulimit -Hn 32768 \
			&& ulimit -Sn 32768 \
			&& ~/faban/bin/fabancli submit OlioDriver dbadmin ~/faban/config/profiles/dbadmin/run.xml.OlioDriver"`
		status=`ssh root@${FABAN_INSTANCE[0]} "~/faban/bin/fabancli status $task_name"`
		echo ".. (2/3) Start a new benchmark as $task_name, in the status of $status"
		if [ "$PLATFORM" == "rds" -a "$status" == "STARTED" ]; then
			cd "$LOCATION/../supports/heartbeat" && ./heartbeat.sh ${MYSQL_INSTANCE_RUN[0]} $DATABASE_USER $DATABASE_PASSWORD &
		fi
		while [ "$status" == "STARTED" ]; do
			for ((t=0; t<30; t++)) do
				printf ".";
				sleep 1;
			done
			status=`ssh root@${FABAN_INSTANCE[0]} "~/faban/bin/fabancli status $task_name"`
		done
		printf "\n";
		sleep 10
		
		echo ".. (3/3) Collect result set for benchmark $task_name"
		# Collecting data from instances
		mysql_instance_run_a=($mysql_instance_run)
		mysql_instance_s=`echo ${#mysql_instance_run_a[*]} 1 | awk '{ printf("%d",($1-$2))}'`
        echo "Start downloading result set from Faban (1/2)"
		cd "$LOCATION/post" && ./faban-resultset.sh "${FABAN_INSTANCE[0]}" $task_name $ARCHIVE_PATH > /dev/null 2>&1
        echo "Start downloading result set from MySQL (2/2)"
		if [ "$PLATFORM" == "rds" ]; then
			cd "$LOCATION/post" && ./mysql-heartbeatset.sh "$mysql_instance_all" $task_name $ARCHIVE_PATH $DATABASE_USER $DATABASE_PASSWORD > /dev/null 2>&1
		fi
		cd "$LOCATION/post" && ./mysql-resultset.sh "$mysql_instance_all" $task_name $ARCHIVE_PATH > /dev/null 2>&1
		time=`date +"%s"`
		archive="OlioDriver_${mysql_instance_s}Database_${NUM_OF_USERS[$i]}User_${time}"
		mv ~/$ARCHIVE_PATH/$task_name ~/$ARCHIVE_PATH/$archive
		7za a -t7z ~/$ARCHIVE_PATH/$archive.7z ~/$ARCHIVE_PATH/$archive
		rm -fr ~/$ARCHIVE_PATH/$archive
	done
done
echo "Experiments end at `date`"
