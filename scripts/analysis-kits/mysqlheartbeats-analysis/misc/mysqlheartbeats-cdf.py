#!/usr/bin/python

import sys

filename = sys.argv[1]
fd = open(filename, 'rb')

responseTimes = []
for line in fd.readlines():
	if len(line.strip()):
		responseTimes.append(float(line.strip()))

responseTimes.sort()

prob = 0
for time in responseTimes:
	prob += 1.0 / len(responseTimes)
	print "%.4f\t%.4f" % (prob, time)