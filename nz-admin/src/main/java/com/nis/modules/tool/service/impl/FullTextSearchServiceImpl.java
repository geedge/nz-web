package com.nis.modules.tool.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertHandlerDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.sys.dao.BasicMapper;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.tool.service.FullTextSearchService;
import com.nis.modules.tool.utils.ToolConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 高级搜索
 * FullTextSearchServiceImpl
 *
 * @author shizhendong
 * @date 2023-05-24 17:31:44.352
 */
@Service
public class FullTextSearchServiceImpl implements FullTextSearchService {

    private static final Log log = Log.get();

    @Autowired
    private BasicMapper basicMapper;

    @Autowired
    private AlertHandlerDao msgHandlerDao;

    @Autowired
    private SysConfService sysConfService;

    @Override
    public Object queryMetadata(String type) {
        String fulltextMetadataConf = sysConfService.getValueOrDefault("fulltext_metadata_conf", StrUtil.EMPTY_JSON);
        JSONObject jsonObject = Tool.JSONUtil.parseObj(fulltextMetadataConf);
        return Tool.ObjectUtil.defaultIfNull(jsonObject.get(type), Collections.emptyList());
    }

    @Override
    public PageUtils queryKeywordList(String metadataType, String text, String pageNo, String pageSize) {
        ToolConstant.KeywordTypeTableEnum instanceByType = ToolConstant.KeywordTypeTableEnum.getInstanceByType(metadataType);
        ValidateUtils.is(instanceByType).notNull(RCode.TOOL_FULLTEXT_KEYWORD_TYPE_INVALID);

        String tableName = instanceByType.getTableName();
        Map<String, Object> params = MapUtil.ofEntries(
                MapUtil.entry(Constant.PAGE, pageNo),
                MapUtil.entry(Constant.LIMIT, pageSize),
                MapUtil.entry("tableName", tableName),
                MapUtil.entry("text", text)
        );
        IPage page = new Query(Object.class).getPage(params);

        List<String> nameList = basicMapper.selectKeywordsByTableName(page, params);
        page.setRecords(nameList);
        return new PageUtils(page);
    }

    @Override
    public PageUtils queryAlertMessageFullTextPage(Map<String, Object> params) {
        String queryBodyJsonStr = (String) params.get("body");
        queryBodyJsonStr = URLDecoder.decode(queryBodyJsonStr, Charset.forName("UTF-8"));

        Map queryBody = JSONUtil.toBean(queryBodyJsonStr, HashMap.class);

        // 根据状态判断对应的表
        if (ToolUtil.isNotEmpty(queryBody.get("state"))) {
            String tableName = Constant.ALERT_MESSAGE_STATE_TABLE_MAPPING.get(Integer.valueOf(queryBody.get("state").toString()));
            queryBody.put("tableName", tableName);
        }

        // preprocess Sql Param
        queryBody = this.preprocessSqlParam("alertmessage", queryBody);

        String fulltextMode = sysConfService.getValueOrDefault("fulltext_mode", "1");
        if (StrUtil.equals(fulltextMode, "1")) {
            // MYSQL
            IPage page = new Query(AlertMessageEntity.class).getPage(params);
            List<AlertMessageEntity> messageEntityList = msgHandlerDao.queryAlertMessageFullText(page, queryBody);
            page.setRecords(messageEntityList);
            return new PageUtils(page);
        } else {

            // TODO REDIS
            return new PageUtils(null);
        }
    }


    /**
     * preprocess Sql Param
     *
     * @param metadataType
     * @param queryBody
     * @return
     */
    private Map<String, Object> preprocessSqlParam(String metadataType, Map<String, Object> queryBody) {
        // metadata config
        String fulltextMetadataConf = sysConfService.getValueOrDefault("fulltext_metadata_conf", StrUtil.EMPTY_JSON);
        JSONObject jsonObject = Tool.JSONUtil.parseObj(fulltextMetadataConf);
        JSONArray alertmessageConf = (JSONArray) jsonObject.get(metadataType);

        // number
        List<String> numberTypeProperties = alertmessageConf.stream().filter(obj -> StrUtil.equals(((JSONObject) obj).get("type").toString(), "number"))
                .map(obj -> ((JSONObject) obj).get("name").toString())
                .collect(Collectors.toList());

        for (Map.Entry<String, Object> entry : queryBody.entrySet()) {
            String key = entry.getKey();
            // number. get operator and value
            // num: ["11", "-12", ">15", "<18", ">=19", "<=20"]
            // num = 11 and num != 12 and num >15 and num < 18
            if (numberTypeProperties.contains(key)) {
                List<Map> tList = ListUtil.list(true);
                JSONArray values = (JSONArray) entry.getValue();
                for (Object value : values) {
                    String group = ReUtil.getGroup0(ToolConstant.NUMBER_OPERATION_PATTERN, StrUtil.toString(value));

                    String operator = StrUtil.nullToDefault(group, "=");
                    operator = StrUtil.equals("-", operator) ? "!=" : operator;

                    tList.add(MapUtil.builder()
                            .put("operator", operator)
                            .put("value", StrUtil.toString(value).replaceAll("-|<|>|=", ""))
                            .map());
                }
                entry.setValue(tList);
            }
        }
        return queryBody;
    }

}
