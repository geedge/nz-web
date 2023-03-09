
/**
        1. modify dashboard names for asset and endpoint types
 */

UPDATE visual_panel panel
LEFT JOIN asset_asset asset ON panel.link = asset.id
SET panel.name = asset.name
WHERE
	panel.type = "asset"
	AND asset.name IS NOT NULL;


UPDATE visual_panel panel
LEFT JOIN monitor_endpoint endpoint ON panel.link = endpoint.id
SET panel.name = endpoint.name
WHERE
	panel.type = "endpoint"
	AND endpoint.name IS NOT NULL;

