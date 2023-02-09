#!/bin/bash

import requests


class AppTester:

    def put(self, dest, url):
        dest = "http://" + dest
        print("PUT: (" + dest + ", " + url + ")")
        return requests.put(dest, data=url).text

    def get(self, shortenedUrl):
        print("GET: " + shortenedUrl)
        r = requests.get(shortenedUrl, allow_redirects=False)

        if r.status_code == 301:
            return True
        return False
