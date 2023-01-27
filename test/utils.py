import subprocess
import string
import os
import random
import time

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
