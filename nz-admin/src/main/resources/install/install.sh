#!/bin/bash
{# this ensures the entire script is downloaded #

#auto replace parameter start

WEB_TOKEN=${webToken}

DATACENTER_ID=${datacenterID}

TYPE=${type}

REMOTE_IP=${remoteIP}

ROOT_PATH=${rootPath}

AGENT_TOKEN=$(cut -d '-' -f 1 /proc/sys/kernel/random/uuid)

wget -q --content-disposition --header="Authorization:"$WEB_TOKEN http://$REMOTE_IP/agent/download

}# this ensures the entire script is downloaded #