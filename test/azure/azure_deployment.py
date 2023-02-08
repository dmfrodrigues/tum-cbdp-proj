#!/bin/python

import subprocess

from os import path

import time

DEPLOY_SCRIPTS_DIR = path.join("deploy_azure")
TEST_SCRIPTS_DIR = path.join("test", "azure")  # TODO Specify azure folder here
SRC_DIR = path.join("app")


class AzureDeployment:
    def __init__(self):
        self.leader = ""
        self.peers = []

    def check_if_dir_changed(self, dir):
        return subprocess.run(["git", "diff-index", "--quiet", "HEAD", "--", dir], stdout=subprocess.PIPE).returncode == 1

    def check_if_src_changed(self):
        return self.check_if_dir_changed(SRC_DIR)

    def check_if_tests_changed(self):
        return self.check_if_dir_changed(TEST_SCRIPTS_DIR)

    def run_script(self, script_name, args=[]):
        script_path = path.join(DEPLOY_SCRIPTS_DIR, script_name)
        subprocess.run(["chmod", "+x", script_path])
        return subprocess.run([script_path] + args, stdout=subprocess.PIPE).stdout.decode('utf-8').strip()

    def upload_app_images(self):
        self.run_script("deploy_push_app_image.sh", [])

    def upload_test_images(self, test_name):
        self.run_script("deploy_push_test_image.sh", [
                        TEST_SCRIPTS_DIR, test_name])

    def deploy(self, n_peers, create_network=True):
        if create_network:
            print("Creating network...")
            self.run_script("deploy_create.sh", [str(n_peers)])
        else:
            print("Starting network...")
            self.run_script("deploy_start.sh", [str(n_peers)])

        self.leader = self.run_script("deploy_get_container.sh", ["leader"])
        for i in range(n_peers):
            self.peers.append(self.run_script(
                "deploy_get_container.sh", ["peer{}".format(i)]))

        print("Network deployed: {}".format(self))

    def deploy_test(self, create_network=True):
        if create_network:
            print("Creating test...")
            self.run_script("deploy_create_test.sh")
        else:
            print("Starting test...")
            self.run_script("deploy_start_test.sh")

    def stop_node(self, node):
        self.run_script("deploy_stop.sh", [node])

    def stop(self):
        self.stop_node("leader")
        for i in range(len(self.peers)):
            self.stop_node("peer{}".format(i))

    def __str__(self) -> str:
        return "Leader: {}\nPeers: {}".format(self.leader, self.peers)


if __name__ == "__main__":
    azure_deployment = AzureDeployment()
    publish_app_image = azure_deployment.check_if_src_changed()
    if publish_app_image:
        print("Source code changed, publishing images...")
        azure_deployment.upload_app_images()

    publish_test_image = azure_deployment.check_if_tests_changed()
    if publish_test_image:
        print("Test code changed, publishing images...")
        azure_deployment.upload_test_images("echo.py")

    # azure_deployment.deploy(2, create_network=publish_app_image)
    print("Waiting for network to start...")
    # time.sleep(10)
    azure_deployment.deploy_test(create_network=publish_test_image)
    print("Waiting for test to start...")
    time.sleep(10)
    print("Stopping network...")
    azure_deployment.stop()
