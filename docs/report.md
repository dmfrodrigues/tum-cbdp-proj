## Describe the design of your system

Our system implements Raft for log duplication. In our case, a log entry is a key-value pair, where the value is the original URL and the key is the shortened key. It is implemented in Java, and makes use of the RMI API for RPC.

The shortened URL is determined by finding the next available key, and then encoding with Base64.

When a new log entry is added, the leader send an appendEntries request immediately, to get the new log entry to all peers as soon as possible. Also, at every heartbeat the leader also checks if the answer was false (in which case, the peer log needs to be healed), and the leader starts working on healing the faulty peer log.

Raft mostly considers the cluster membership to not change. The original paper does consider the possibility of using a two-phase configuration migration to guarantee safety. However, we considered this algorithm to be too complex for the timespan available to implement it.

So instead, we implemented a simple membership algorithm with a gossip protocol to heal the members list. This protocol works as follows:
1. A new peer may only join through the leader. The new peer sends a join request to the leader. The leader adds the new peer to its members list, and informs all the old members (that were already part of the cluster) that a new peer has joined. Then the leader replies to the new peer with the list of all members the leader knows.
2. Every 1000ms, each peer picks a random peer, and they exchange membership information through a gossip protocol. This guarantees that member lists are healed if step 1 somehow fails to cover some edge-case.
3. Every time a peer fails to connect to another, it immediately assumes it is down, and unilaterally removes it from the members list. This procedure is not used to detect leader failures; it is only used to update the members list.

The external interface of each node is an HTTP interface with two endpoints:
1. `PUT /`: which requests a new URL to be shortened. The URL to be shortened is in the body of the request. The reply body contains the shortened key (not the whole shortened URL; because this makes it easier to use the same key to ask different nodes).
2. `GET /{shortenedKey}`: asks to redirect to the full version of the shortened key `shortenedKey`. If successful, the response has code `301 Moved Permanently` and the `Location` is set to the proper full URL. If it fails, the server replies with `404 Not Found`

All requests can be made to all nodes; `GET` requests are served locally by each node, but `PUT` requests are redirected to the leader in a transparent way.

A few notable improvements we implemented are:
- **When commitIndex is modified, an extraordinary heartbeat is sent to all peers**, so that the may commit more log entries. This does not make Raft linearizable, because Raft does not wait for a log entry to be committed in all peers to return to a `PUT` request (although it waits until a writing quorum of half the peers plus one has gotten the new log entry before replying to the `PUT`). It does however help expedite commits in non-leader peers, since the leader does not wait until the next scheduled periodic heartbeat to inform peers of commitIndex; the leader informs them immediately.

## How does your cluster handle leader crashes?

The heartbeat period is $500\text{ms}$. Each peer picks a random election timeout $T$ between $T_{min} = 1000\text{ms}$ and $T_{max} = 2000\text{ms}$. When a peer detects a leader crash by noticing that the last appendEntries (or requestVote) were more than $T$ time ago, it becomes a candidate and starts an election. The election process follows the Raft protocol.

### How long does it take to elect a new leader?

Communication between peers in local deployment is quite fast (around 1ms-10ms). We also only tested clusters with up to 6 nodes. Therefore, elections rarely fail, and they often take 500ms-700ms between the instant the leader dies and the instant a new leader is elected (this is because the leader may fail at any random moment between time instant $0$ and $T_{min}$, with average $T/2 = 500\text{ms}$). This makes it so that a cluster with higher number nodes usually takes less time to elect a new leader, because it is more likely for a node to choose a lower election timeout and leader failure detection happens more frequently.


<> avg election time = 40ms + 37ms + 61ms + 43ms = 181ms / 4 = 45.25ms
In practice, the average election time since a node detects a leader failure for a network with 2 followers and 1 leader failing is 45ms.

### Measure the impact of election timeouts. Investigate what happens when it gets too short / too long.

Measurements show that a timeout between $T_{min} = 100\text{ms}$ and $T_{max} = 4000\text{ms}$ is has an average election time of 60ms.
A timeout between $T_{min} = 50\text{ms}$ and $T_{max} = 200\text{ms}$ has an average election time of 10ms. And a timeout between $T_{min} = 3000\text{ms}$ and $T_{max} = 4000\text{ms}$ has an average election time of 70ms.

## Analyze the load of your nodes:

### How much resources do your nodes use?

TODO

### Where do inserts create most load?

TODO

Inserts create most load in the leader, since the leader has to communicate with each peer.

### Do lookups distribute the load evenly among replicas?

TODO

Presumably yes, if the load balancer on the test-driver is decent enough.

## How many nodes should you use for this system? What are their roles?

We should use at least three nodes at every time. This is because, if we were using only two nodes, there was a chance one of the node fails, so only one node is left alive and alone in the network. Thus, it is a good idea to make sure there are at least three nodes, so that if one fails, there are still two nodes in the network.

TODO: is this really the answer they want?

## Measure the latency to generate a new short URL

TODO

### Analyze where your system spends time during this operation

TODO

## Measure the lookup latency to get the URL from a short id

TODO

## How does your system scale?

### Measure the latency with increased data inserted, e.g., in 10% increments of inserted short URLs

TODO

### Measure the system performance with more nodes

Due to Azure limitations, we could only 
