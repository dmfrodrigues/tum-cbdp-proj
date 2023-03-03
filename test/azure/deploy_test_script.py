#!/bin/python
import time
from azure_deployment import AzureDeployment
from sys import argv

# Setup network with a leader
# Execute the specified test script

if len(argv) != 2:
    print("Usage: {} <test_script> <test_args>".format(argv[0]))
    exit(1)

azure_deployment = AzureDeployment()
publish_app_image = azure_deployment.check_if_src_changed()
if publish_app_image:
    print("Source code changed, publishing images...")
    azure_deployment.upload_app_images()

nPeers = 2
print("Deploying network with {} peers...".format(nPeers))
azure_deployment.deploy(nPeers, create_network=publish_app_image)
print("Waiting for network to start...")
time.sleep(20)
publish_test_image = True
if publish_test_image:
    print("Test code changed, publishing images...")
    container_ips = azure_deployment.leader + \
        " " + " ".join(azure_deployment.peers)
    azure_deployment.upload_test_images(argv[1], container_ips)
print("Deploying script...")
azure_deployment.deploy_test(create_network=publish_test_image)
time.sleep(100)
print("Stopping network...")
azure_deployment.stop()
print("Done, Measurements should be done now.")
