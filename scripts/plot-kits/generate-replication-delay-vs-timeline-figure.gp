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
#Script to draw plots from analysis results

set ylabel "Replication delay (ms)"
set xlabel "Timeline (min:sec)"
set timefmt "%s"
set xdata time
set yrange [-10:200]
set key outside bottom
set title "Replication delay with respect to XXX user workload"
# Enable the following two lines for outputing to files
set terminal png size 1024 480
set terminal png
set output "mysqlheartbeats-analysis-xx.png"
plot "./mysqlheartbeats-analysis-xx-ip-xx-xx-xx-xx.csv" using 1:4 title 'Slave 1: xx.xx.xx.xx' with dots linecolor rgb "red"
