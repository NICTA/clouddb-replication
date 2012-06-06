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
#
#This script is used for EC2 instances, initially. Therefore, Olio and
#Heartbeats tables are separated in two databases. And Heartbeats records
#timestamps in microsecond granularity.

if [ "${#}" -lt "3" ]; then
  echo "This script send a heartbeat to MySQL master database every second. " 
  echo ""
  echo "Usage:"
  echo "   ${0} [MySQL] [DATABASE_USER] [DATABASE_PASSWORD]"
  exit 0
fi

MASTER_INSTANCE="${1}"

DATABASE_USER=${2}
DATABASE_PASSWORD=${3}
RAMP_UP=600
STEADY_STAGE=1200

master_mysql=$MASTER_INSTANCE

generate_heartbeat()
{
  mysql -u${DATABASE_USER} -p${DATABASE_PASSWORD} -h $1 -e \
  "SET SESSION binlog_format = 'STATEMENT'; INSERT INTO heartbeats.heartbeats(sys_mill, db_micro) VALUES (`date +%s000000`, sysdate()) /* HeartbeatOperation */;"
}

# Run heartbeat
sleep $RAMP_UP
for ((i=0; i < ${STEADY_STAGE}; i++)) do
  generate_heartbeat $master_mysql > /dev/null 2>&1
  sleep 1
done
