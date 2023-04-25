/**
    1. add index to severity_id column in tables alert_message_active and alert_message_history
 */


CREATE INDEX
IF NOT EXISTS idx_severity_id ON alert_message_active ( `severity_id` ) USING BTREE;

CREATE INDEX
IF NOT EXISTS idx_severity_id ON alert_message_history ( `severity_id` ) USING BTREE;
