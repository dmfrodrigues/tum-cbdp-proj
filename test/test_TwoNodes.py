import unittest
import requests

import utils

class TestTwoNodes(unittest.TestCase):
    def setUp(self):
        self.peers, self.leader = utils.spinUpCluster(2)

    def tearDown(self):
        utils.killCluster()

    def testPutGet(self):
        # Test not found
        for peer in self.peers:
            shortenedUrl = utils.getShortenedUrl(peer, "12345678")
            r = requests.get(shortenedUrl, allow_redirects=False)
            self.assertEqual(r.status_code, 404)

        # Test shortening URL
        url = "https://www.tum.de/"

        r = requests.put(utils.getAddress(self.leader), data=url)
        self.assertEqual(r.status_code, 200)
        shortened = r.text
        print("shortened=" + shortened)

        for peer in self.peers:
            shortenedUrl = utils.getShortenedUrl(peer, shortened)
            print("shortenedUrl=" + shortenedUrl)
            r = requests.get(shortenedUrl, allow_redirects=False)
            self.assertEqual(r.status_code, 301)
            urlGet = r.headers['Location']

            self.assertEqual(url, urlGet)

if __name__ == '__main__':
    unittest.main()
