#!/bin/python

import subprocess


class AzureDeployment:
    def __init__(self):
        self.leader = ""
        self.peers = []

    def upload_images(self):
        subprocess.run("./deploy_push_images.sh")

    def deploy(self, n_peers):
        subprocess.run(["./deploy_create.sh", str(n_peers)])

        self.leader = subprocess.run(
            ["./deploy_get_container.sh", "leader"], stdout=subprocess.PIPE).stdout.decode('utf-8').strip()
        for i in range(n_peers):
            self.peers.append(subprocess.run(
                ["./deploy_get_container.sh", "peer{}".format(i)], stdout=subprocess.PIPE).stdout.decode('utf-8').strip())

    def stop(self):
        subprocess.run(["./deploy_stop.sh", "leader"])
        for i in range(len(self.peers)):
            subprocess.run(["./deploy_stop.sh", "peer{}".format(i)])

    def __str__(self) -> str:
        return "Leader: {}\nPeers: {}".format(self.leader, self.peers)


if __name__ == "__main__":
    azure_deployment = AzureDeployment()
    azure_deployment.leader = "10.0.0.4"
    azure_deployment.peers = ["10.0.0.5", "10.0.0.6"]
    # azure_deployment.upload_images()
    # azure_deployment.deploy(2)
    # print(azure_deployment)
    azure_deployment.stop()
