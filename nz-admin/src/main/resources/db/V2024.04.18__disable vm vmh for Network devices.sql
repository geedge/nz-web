
/*
1. Disable vm vmh for Network devices
 */

UPDATE `asset_type_conf` SET `vm` = 0, `vmh` = 0 WHERE `name` = "Network devices";
