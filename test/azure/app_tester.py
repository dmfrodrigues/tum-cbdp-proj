#!/bin/bash

import requests


class AppTester:

    def put(self, dest, url):
        print("PUT: " + url)
        requests.put(dest, data=url)

    def get(self, dest, hash):
        shortenedUrl = dest + "/" + hash
        print("GET: " + shortenedUrl)
        r = requests.get(shortenedUrl, allow_redirects=False)

        if r.status_code == 301:
            print("Redirected to: " + r.headers['Location'])
            return True
        return False
