#!/bin/bash

# Start containers
docker-compose -f environment/cluster-redis/docker-composep-redis-cluster.yml down
echo "[TipsGO]: Redis_sentinel server Stop..."