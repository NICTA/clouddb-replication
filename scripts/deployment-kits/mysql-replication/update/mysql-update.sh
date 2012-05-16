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
#Script to do some light patch (e.g. update MySQL system and etc.) 
#without running a heavy-weight mysql-install.sh

if [ "${#}" -lt "1" ]; then
  echo "This script takes addresses of Ubuntu instances to update "
  echo "MySQL and other softwares for the test environment."
  echo ""
  echo "Usage:"
  echo "   ${0} [MySQL]"
  exit 0
fi

MYSQL_INSTANCE="${1}"
NTP_TIME_SERVER="0.us.pool.ntp.org 1.us.pool.ntp.org 2.us.pool.ntp.org 3.us.pool.ntp.org"

install_faban_sys()
{
  # Upload and untar Faban
  ssh root@$1 "cd ~/faban/master/bin \
  && ./shutdown.sh \
  && killall java \
  && rm -fr ~/faban"
  scp -r ../../packages/faban.tar.bz2 root@$1:~/faban.tar.bz2 && \
  ssh root@$1 "tar -jxvf faban.tar.bz2 \
  && rm faban.tar.bz2"
  ssh root@$1 "rm -fr ~/faban/benchmarks/*.* ~/faban/benchmarks/*"
  scp -r ../../packages/OlioDriver.jar root@$1:~/faban/benchmarks/.
  ssh root@$1 "mkdir ~/faban/config/profiles"
  scp -r ../../packages/logging.properties root@$1:~/faban/config/logging.properties
  
  # Install ethtool to make nicstat work
  ssh root@$1 "apt-get update \
  && aptitude -y install install ethtool"

  # Update hosts file in /etc
  ssh root@$1 'echo -e `wget -qO - http://icanhazip.com/` \\t `hostname` >> /etc/hosts'
}

enable_time_sync()
{
  # Start time sync
  ssh root@$1 "service ntp stop"
  ssh root@$1 "killall bash"
  ssh root@$1 "while true; do ntpdate -b -p 8 -u $NTP_TIME_SERVER > /dev/null 2>&1; sleep 1; done < /dev/null > /dev/null 2>&1 &"
}

install_database_sys()
{
  install_faban_sys $1
  enable_time_sync $1
}

# Setup MySQL instance
for mysql in $MYSQL_INSTANCE; do
  install_database_sys $mysql > /dev/null &
done
wait
