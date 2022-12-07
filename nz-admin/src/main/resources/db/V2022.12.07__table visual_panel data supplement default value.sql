/**
        1. supplement var_type default value
 */

update visual_panel set var_type = 1 where type = 'asset';
update visual_panel set var_type = 2 where type = 'endpoint';
