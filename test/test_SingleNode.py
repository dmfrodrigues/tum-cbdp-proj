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
        shortenedUrl = utils.getShortenedUrl(self.leader, "12345678")
        r = requests.get(shortenedUrl, allow_redirects=False)
        self.assertEqual(r.status_code, 404)

        # Test shortening URL
        url = "https://www.tum.de/"

        r = requests.put(utils.getAddress(self.leader), data=url)
        self.assertEqual(r.status_code, 200)
        shortened = r.text

        shortenedUrl = utils.getShortenedUrl(self.leader, shortened)
        r = requests.get(shortenedUrl, allow_redirects=False)
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
            r = requests.put(utils.getAddress(self.leader), data=urls[i])
            self.assertEqual(r.status_code, 200)
            shortened.append(r.text)

        print("PUT: ", time.time()-t)
        t = time.time()

        for i in range(N):
            shortenedUrl = utils.getShortenedUrl(self.leader, shortened[i])
            r = requests.get(shortenedUrl, allow_redirects=False)
            self.assertEqual(r.status_code, 301)
            urlGet = r.headers['Location']

            self.assertEqual(urls[i], urlGet)

        print("GET: ", time.time()-t)

if __name__ == '__main__':
    unittest.main()
