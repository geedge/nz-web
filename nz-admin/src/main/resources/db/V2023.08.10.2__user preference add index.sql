/**
 *
 *    user preference add index
 *
 **/

CREATE INDEX IF NOT EXISTS idx_uid ON sys_user_preference(`uid`) USING BTREE;
CREATE INDEX IF NOT EXISTS idx_pref_key ON sys_user_preference(`pref_key`) USING BTREE;