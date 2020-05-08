#/bin/bash
tar -zxf target_path/node_exporter.tar.gz -C target_path
nohup target_path/node_exporter  > target_path/node_exporter.log 2>&1 &
rm -rf target_path/node_exporter.tar.gz
