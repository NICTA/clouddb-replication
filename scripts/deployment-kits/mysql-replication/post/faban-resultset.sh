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
#Script to collect Faban data and more.

if [ "${#}" -lt "3" ]
then
  echo "This script takes addresses of Faban instances and name "
  echo "of task and archive path to download result sets."
  echo ""
  echo "Usage:"
  echo "   ${0} [Faban] [Taks_Name] [Archive_Path]"
  exit 0;
fi

FABAN_INSTANCE="${1}"
TASK_NAME="${2}"
ARCHIVE_PATH="${3}"

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

# Download result set
scp -r root@$FABAN_INSTANCE:~/faban/output/$TASK_NAME ~/$ARCHIVE_PATH/. > /dev/null
check_errs $? "Download result set failed in $FABAN_INSTANCE."
