import time
import unittest
import requests

import utils

class TestTwoNodes(unittest.TestCase):
    def setUp(self):
        self.peers, self.leader = utils.spinUpCluster(2)

    # def tearDown(self):
    #     utils.killCluster()

    def testPutGet(self):
        leaderAddress = utils.getContainerAddress(self.leader)

        # Test not found
        for peer in self.peers:
            peerAddress = utils.getContainerAddress(peer) + "/12345678"
            print(peer, peerAddress)
            r = requests.get(peerAddress, allow_redirects=False)
            self.assertEqual(r.status_code, 404)

        # Test shortening URL
        url = "https://www.tum.de/"

        r = requests.put(leaderAddress, data=url)
        self.assertEqual(r.status_code, 200)
        shortened = r.text
        print("shortened=" + shortened)

        for peer in self.peers:
            peerAddress = utils.getContainerAddress(peer)
            r = requests.get(utils.getContainerAddress(self.leader) + "/" + shortened, allow_redirects=False)
            self.assertEqual(r.status_code, 301)
            urlGet = r.headers['Location']

            self.assertEqual(url, urlGet)

if __name__ == '__main__':
    unittest.main()
