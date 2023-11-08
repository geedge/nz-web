package com.nis.modules.tool.controller;

import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.service.AlertHandlerService;
import com.nis.modules.tool.service.FullTextSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 高级搜索
 * FullTextSearchController
 *
 * @author shizhendong
 * @date 2023-05-24 17:31:44.352
 */
@RestController
@RequestMapping
public class FullTextSearchController {

    private final static Log log = Log.get();

    @Autowired
    private AlertHandlerService handlerService;

    @Autowired
    private FullTextSearchService fullTextSearchService;


    @GetMapping("/fulltext/metadata/{type}")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
    public R queryMetadataConf(@PathVariable(value = "type", required = false) String type) {
        ValidateUtils.is(type).notNull(RCode.TOOL_FULLTEXT_METADATA_TYPE_ISNULL);

        return R.ok().putData("list", fullTextSearchService.queryMetadata(type));
    }


    @GetMapping("/fulltext/keyword")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
    public R queryKeywordList(@RequestParam Map<String, Object> requestParam) {
        requestParam = Tool.MapUtil.defaultIfEmpty(requestParam, Tool.MapUtil.empty());

        String metadataType = (String) requestParam.get("name");
        ValidateUtils.is(metadataType).notNull(RCode.TOOL_FULLTEXT_METADATA_TYPE_ISNULL);

        String text = (String) requestParam.get("text");

        String pageNo = (String) requestParam.get("pageNo");
        pageNo = Tool.StrUtil.emptyToDefault(pageNo, "1");

        String pageSize = (String) requestParam.get("pageSize");
        pageSize = Tool.StrUtil.emptyToDefault(pageSize, "10");

        PageUtils page = fullTextSearchService.queryKeywordList(metadataType, text, pageNo, pageSize);
        return R.ok(page);
    }


    @GetMapping("/alert/message/query")
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.ALERT_MESSAGE)
    public R alertMessageFullTextSearchQuery(@RequestParam Map<String, Object> params) {
        PageUtils page = fullTextSearchService.queryAlertMessageFullTextPage(params);
        return R.ok(page);
    }

}
