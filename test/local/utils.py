import subprocess
import string
import os
import random
import time
import docker

client = docker.from_env()

def getRandomString(len: int):
    return ''.join(random.choice(string.ascii_uppercase + string.ascii_lowercase + string.digits) for _ in range(len))

def getRandomURL():
    return "https://www." + getRandomString(16) + ".com"

def getContainers():
    out = client.containers.list()
    out = filter(lambda c: c.name.startswith("url-shortener_leader") or c.name.startswith("url-shortener_peer"), out)
    return list(out)

def containerIsHealthy(container) -> bool:
    return container.attrs['State']['Health']['Status'] == 'healthy'

def waitUntilHealthy(container):
    while not containerIsHealthy(container):
        time.sleep(1)
        container.reload()
    print("Container " + container.name + " is healthy")

def spinUpCluster(numberPeers: int):
    scale = numberPeers-1
    print("Spin up cluster")
    assert(os.system(f"docker-compose -f docker-compose.test.e2e.yml up --build -d --scale peer={scale}") == 0)
    print("Detached from cluster")
    containers = getContainers()
    for container in containers:
        waitUntilHealthy(container)
    print("All containers healthy")
    return containers, client.containers.get('url-shortener_leader')

def killCluster():
    assert(os.system("docker-compose -f docker-compose.test.e2e.yml down --remove-orphans") == 0)

def getAddress(container):
    host = container.attrs["NetworkSettings"]["Ports"]["8001/tcp"][0]
    return "http://" + host["HostIp"] + ":" + host["HostPort"]

def getShortenedUrl(container, shortened: str) -> str:
    return getAddress(container) + "/" + shortened
