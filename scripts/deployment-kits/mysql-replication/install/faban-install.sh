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
#Script to install all necessary tools, Faban and MySQL.
#
#NOTE:
#This script will replace config file in .ssh in remote machine
#We need a private key for EC2 access in this folder.
#Rename the key to dbadmin.pem, or change script to suit your need.
#
#This script is for Ubuntu EC2 instances. For other Linux distributions,
#please make corresponding changes.
#

if [ "${#}" -lt "1" ]; then
  echo "This script takes addresses of Ubuntu instances to install "
  echo "Faban and other softwares for the test environment."
  echo ""
  echo "Usage:"
  echo "   ${0} [Faban]"
  exit 0
fi

FABAN_INSTANCE="${1}"

# Acquire SSH policy
# It is a pre-requirement of all following installation 
install_ssh_policy()
{
  # Grand root access
  ssh ubuntu@$1 "sudo cp /home/ubuntu/.ssh/authorized_keys /root/.ssh/"

  # Deploy ssh policy
  scp -r ./conf/config-ssh root@$1:~/.ssh/config
  scp -r ./conf/dbadmin.pem root@$1:~/.ssh/dbadmin.pem && \
  ssh root@$1 "chmod 600 ~/.ssh/dbadmin.pem"
}

# Install Java environment
# It is a pre-requirement of Faban system
install_java_env()
{
  # Install Java
  ssh root@$1 "apt-get update \
  && aptitude -y install openjdk-6-jdk \
  && echo 'export JAVA_HOME=/usr/lib/jvm/java-1.6.0-openjdk' | cat - ~/.bashrc > ~/.bashrc_r \
  && mv ~/.bashrc_r ~/.bashrc"
}

install_faban_sys()
{
  # Upload and untar Faban
  scp -r ../../packages/faban.tar.bz2 root@$1:~/faban.tar.bz2 && \
  ssh root@$1 "tar -jxvf faban.tar.bz2 \
  && rm faban.tar.bz2"
  ssh root@$1 "mkdir ~/faban/config/profiles"
}

# Install Olio platform
install_olio_sys()
{
  # Setup MySQL library path for compiling
  ssh root@$1 "echo '/usr/local/mysql/lib' >> /etc/ld.so.conf \
  && ldconfig"
  # Upload and untar Olio to master
  ssh root@$1 "aptitude -y install build-essential ruby ruby-dev \
  rubygems libopenssl-ruby libfreeimage-dev \
  && gem install rake -V --no-ri --no-rdoc -v=0.8.7 \
  && gem install rails -V --no-ri --no-rdoc -v=2.3.8 \
  && gem install mysql -V --no-ri --no-rdoc -v=2.7 -- --with-mysql-dir=/usr/local/mysql \
  && gem install rcov -V --no-ri --no-rdoc -v=0.9.11 \
  && gem install will_paginate -V --no-ri --no-rdoc -v=2.3.16"
  scp -r ../../packages/olio.tar.bz2 root@$1:~/olio.tar.bz2 && \
  ssh root@$1 "mkdir /var/app" && \
  ssh root@$1 "tar -jxvf olio.tar.bz2 -C /var/app \
  && rm olio.tar.bz2"
}

# Install extra tools for system monitoring
install_sys_tools()
{
  # Install misc tools
  ssh root@$1 "aptitude -y install sysstat"
}

setup_faban_inst()
{
  # Install SSH policy, Java runtime environment, Faban system
  install_ssh_policy $1
  install_java_env $1
  install_faban_sys $1

  # Install misc tools
  install_sys_tools $1
}

# Setup Faban instance
for agent in $FABAN_INSTANCE; do
  setup_faban_inst $agent > /dev/null &
done
wait
