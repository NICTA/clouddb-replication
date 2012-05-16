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
#Script to import private key onto all EC2 regions

export EC2_PRIVATE_KEY=private-key.pem
export EC2_CERT=cert.pem

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

keypair=dbadmin  # or some name that is meaningful to you
publickeyfile=dbadmin.pub
regions=$(ec2-describe-regions | cut -f2)

for region in $regions; do
  echo "Import key to region : "$region
  ec2-delete-keypair --region $region $keypair > /dev/null
  check_errs $? "Delete existing key in region $region failed."
  ec2-import-keypair --region $region --public-key-file $publickeyfile $keypair > /dev/null
  check_errs $? "Import key to region $region failed."
done
