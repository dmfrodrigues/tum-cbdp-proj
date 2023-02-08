#!/bin/env python

from sys import argv
import requests

if len(argv) != 2:
    print("Usage: {} <leader_ip>".format(argv[0]))
    exit(1)


# Send delete request to leader
leader_ip = argv[1]
url = "http://{}".format(leader_ip)
print("Sending delete request to leader at {}".format(url))
r = requests.delete(url)
print("Response: {}".format(r.text))
