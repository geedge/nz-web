#/bin/bash

EXPORTER_INSTALL=`rpm -qa node-exporter`

if [ ! $EXPORTER_INSTALL ];then
  rpm -ivh /tmp/nezha/node-exporter-1.0.0-1.x86_64.rpm
fi

if [ $EXPORTER_INSTALL ];then
  rpm -e node-exporter && rpm -ivh /tmp/nezha/node-exporter-1.0.0-1.x86_64.rpm
fi

systemctl start node-exporter.service 

rm -rf /tmp/nezha
