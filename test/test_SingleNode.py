import time
import unittest
import requests

import utils

class TestSingleNode(unittest.TestCase):
    def setUp(self):
        self.peers, self.leader = utils.spinUpCluster(1)

    def tearDown(self):
        utils.killCluster()

    def testPutGet(self):
        # Test not found
        r = requests.get(utils.getContainerAddress(self.leader) + "/12345678", allow_redirects=False)
        self.assertEqual(r.status_code, 404)

        # Test shortening URL
        url = "https://www.tum.de/"

        r = requests.put(utils.getContainerAddress(self.leader), data=url)
        self.assertEqual(r.status_code, 200)
        shortened = r.text

        r = requests.get(utils.getContainerAddress(self.leader) + "/" + shortened, allow_redirects=False)
        self.assertEqual(r.status_code, 301)
        urlGet = r.headers['Location']

        self.assertEqual(url, urlGet)
    
    def testMassiveSeparate(self):
        N = 100
        t = time.time()
        
        urls = []
        for i in range(N):
            urls.append(utils.getRandomURL())

        t = time.time()
        
        shortened = []
        for i in range(N):
            r = requests.put(utils.getContainerAddress(self.leader), data=urls[i])
            self.assertEqual(r.status_code, 200)
            shortened.append(r.text)

        print("PUT: ", time.time()-t)
        t = time.time()

        for i in range(N):
            r = requests.get(utils.getContainerAddress(self.leader) + "/" + shortened[i], allow_redirects=False)
            self.assertEqual(r.status_code, 301)
            urlGet = r.headers['Location']

            self.assertEqual(urls[i], urlGet)

        print("GET: ", time.time()-t)

if __name__ == '__main__':
    unittest.main()
