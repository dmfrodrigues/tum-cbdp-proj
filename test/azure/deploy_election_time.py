#!/bin/python
import time
from azure_deployment import AzureDeployment

# Setup network with a leader
# Crash the leader and check the time (manually) taken to elect a new leader

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
print("Crashing leader...")
azure_deployment.stop_node("leader")
time.sleep(30)
print("Stopping network...")
azure_deployment.stop()
print("Done, Measurements should be done now.")
