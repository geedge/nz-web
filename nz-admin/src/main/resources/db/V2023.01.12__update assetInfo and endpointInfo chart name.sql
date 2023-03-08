
/**
        1. update assetInfo and endpointInfo chart name
 */

UPDATE visual_chart SET name = "Asset information" WHERE type = "assetInfo";
UPDATE visual_chart SET name = "Endpoint information" WHERE type = "endpointInfo";

