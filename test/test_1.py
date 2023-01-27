import os
import random
import string
import subprocess
import time
import unittest
import requests

def getRandomString(len: int):
    return ''.join(random.choice(string.ascii_uppercase + string.ascii_lowercase + string.digits) for _ in range(len))

def getRandomURL():
    return "https://www." + getRandomString(16) + ".com"

def getContainerNames():
    cmd = ["docker", "ps", "--format", "'{{.Names}}'"]
    out = subprocess.check_output(cmd)
    out = out.decode('utf-8').split('\n')
    out = map(lambda s: s[1:-1], out)
    out = filter(lambda s: s.startswith("url-shortener_"), out)
    return list(out)

def waitUntilContainerHealthy(containerName: str):
    cmd = ["docker", "inspect", "-f", "{{.State.Health.Status}}", containerName]
    while subprocess.check_output(cmd) != b'healthy\n':
        time.sleep(1)
    print("Container " + containerName + " is healthy")

def spinUpCluster(numberPeers: int):
    print("Spin up cluster")
    assert(os.system("docker-compose up --build -d") == 0)
    print("Detached from cluster")
    waitUntilContainerHealthy("url-shortener_leader")
    print("All containers healthy")
    peers = getContainerNames()
    return peers, 'url-shortener_leader'

def killCluster():
    assert(os.system("docker-compose down") == 0)

def getContainerAddress(containerName: str):
    port = subprocess.check_output(["docker", "port", containerName]).decode('utf-8').split('\n')[0].split('/')[0]
    return "http://localhost:" + port

class TestClass(unittest.TestCase):
    def setUp(self):
        self.peers, self.leader = spinUpCluster(1)

    def tearDown(self):
        killCluster()

    def testPutGet(self):
        # Test not found
        r = requests.get(getContainerAddress(self.leader) + "/12345678", allow_redirects=False)
        self.assertEqual(r.status_code, 404)

        # Test shortening URL
        url = "https://www.tum.de/"

        r = requests.put(getContainerAddress(self.leader), data=url)
        self.assertEqual(r.status_code, 200)
        shortened = r.text

        r = requests.get(shortened, allow_redirects=False)
        self.assertEqual(r.status_code, 301)
        urlGet = r.headers['Location']

        self.assertEqual(url, urlGet)
    
    def testMassiveSeparate(self):
        N = 100
        t = time.time()
        
        urls = []
        for i in range(N):
            urls.append(getRandomURL())

        t = time.time()
        
        shortened = []
        for i in range(N):
            r = requests.put(getContainerAddress(self.leader), data=urls[i])
            self.assertEqual(r.status_code, 200)
            shortened.append(r.text)

        print("PUT: ", time.time()-t)
        t = time.time()

        for i in range(N):
            r = requests.get(shortened[i], allow_redirects=False)
            self.assertEqual(r.status_code, 301)
            urlGet = r.headers['Location']

            self.assertEqual(urls[i], urlGet)

        print("GET: ", time.time()-t)

if __name__ == '__main__':
    unittest.main()
