#!/bin/python

import time
from app_tester import AppTester
import requests
from sys import argv

URL = "https://db.in.tum.de/teaching/ws2223/clouddataprocessing/data/filelist.csv"


if len(argv) != 4:
    print("Usage: {} <leader> <peer1> <peer2>".format(argv[0]))
    exit(1)

leader, peer1, peer2 = argv[1], argv[2], argv[3]


f = requests.get(URL)
f_list = f.text.splitlines()
appTester = AppTester()
put_times = []
get_times = []

# Send 10% of the urls as PUT requests to the peers followed by 10% of the urls as GET requests to the peers
# Measure the time it takes to complete the requests

line = f_list[0]
urls = requests.get(line).text.splitlines()
# Split the urls into 10% chunks
N = 100
url_lists = [urls[i:i + len(urls)//N]
             for i in range(0, len(urls), len(urls)//N)]
print("Started")

for i in range(10):
    start = time.time()
    url_list = url_lists[i]
    for url in url_list:
        shortened = appTester.put(peer1, url)
    end = time.time()
    put_times.append((end - start)/len(url_list))

    start = time.time()
    for url in url_list:
        shortenedUrl = "http://" + peer2 + "/" + shortened
        appTester.get(shortenedUrl)
    end = time.time()
    get_times.append((end - start)/len(url_list))

    print(len(url_list))

print(len(url_list))

print("PUT times: {}".format(put_times))
print("GET times: {}".format(get_times))
