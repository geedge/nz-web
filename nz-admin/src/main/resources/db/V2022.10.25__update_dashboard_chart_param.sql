/**
 * 更新 chart link 跳转链接
 */
UPDATE visual_chart
SET param = JSON_SET(
	param,
	'$.link',
REPLACE ( JSON_UNQUOTE( JSON_EXTRACT( param, "$.link" )), "/#/", "/ui/" )) 
WHERE
	param IS NOT NULL 
	AND JSON_VALID( param ) = 1 
	AND JSON_UNQUOTE(
	JSON_EXTRACT( param, '$.link' )) IS NOT NULL 
	AND LOCATE(
		"/#/",
	JSON_UNQUOTE(
	JSON_EXTRACT( param, '$.link' )));