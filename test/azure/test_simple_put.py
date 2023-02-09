#!/bin/python

from app_tester import AppTester
from sys import argv
import time

# Send 10 PUT requests to a peer followed by 10 GET requests to the other peer
# Measure the time it takes to complete the requests

if len(argv) != 4:
    print("Usage: {} <leader> <peer1> <peer2>".format(argv[0]))
    exit(1)

leader, peer1, peer2 = argv[1], argv[2], argv[3]

appTester = AppTester()

put_times = []
get_times = []


for i in range(10):
    start = time.time()
    shortened = appTester.put(peer1, "http://www.google.com")
    end = time.time()
    put_times.append(end - start)

    shortenedUrl = "http://" + peer2 + "/" + shortened
    start = time.time()
    appTester.get(shortenedUrl)
    end = time.time()
    get_times.append(end - start)

print("PUT times: {} avg: {}".format(
    put_times, sum(put_times) / len(put_times)))
print("GET times: {} avg: {}".format(
    get_times, sum(get_times) / len(get_times)))
