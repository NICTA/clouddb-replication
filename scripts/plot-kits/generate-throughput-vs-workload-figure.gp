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

set ylabel "Queres per second (qps)"
set xlabel "Workload (# of concurrent users)"
set yrange [0:700]
set key outside bottom
set title "Queres with respect to numbers of slaves"
avgwd=46.658
f(x)=avgwd*(x/25)
# Enable the following two lines for outputing to files
set terminal png size 1024 480
set terminal png
set output "mysqlstats-analysis.png"
plot f(x) title 'With unlimited scale' with dots linecolor rgb "black", \
     "./mysqlstats-xdb-analysis.csv" using 1:6 title '1 Slave' with line linecolor rgb "red"
