#/bin/bash
tar -zxf /opt/nezha/node_exporter/node_exporter.tar.gz -C /opt/nezha/node_exporter
nohup /opt/nezha/node_exporter/node_exporter  > /opt/nezha/node_exporter/node_exporter.log 2>&1 &
rm -rf /opt/nezha/node_exporter/node_exporter.tar.gz
