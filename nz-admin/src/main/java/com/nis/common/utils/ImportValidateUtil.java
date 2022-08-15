package com.nis.common.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysI18nService;
import lombok.Builder;
import lombok.Data;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目导入校验工具类、
 * 可根据需要自行补充校验方法
 * 校验后将错误信息保存至 redis list 中
 */
@Data
@Builder
@Lazy
public class ImportValidateUtil {

    private static RedisTemplate redisTemplate;

    private static SysI18nService sysI18nService;

    private static BasicImportAndExportServices basicImportAndExportServices;

    static {
        redisTemplate = (RedisTemplate) SpringContextUtils.getBean("redisTemplate");
        sysI18nService = SpringContextUtils.getBean(SysI18nService.class);
        basicImportAndExportServices = SpringContextUtils.getBean(BasicImportAndExportServices.class);
    }

    private String errMsgRedisKey;

    /**
     * 此行忽略并重置错误信息，只提示忽略本行
     *
     * @param lineNum
     * @param header
     */
    public void ignoreThisRow(Integer lineNum, String header) {
        // 删除之前的错误信息
        List<String> errMsgList = redisTemplate.opsForList().range(errMsgRedisKey, 0, -1);

        errMsgList = errMsgList.stream().filter(str -> {
            Map map = JSONObject.parseObject(str, Map.class);
            if (!lineNum.equals((Integer) map.get("lineNo"))) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        // 先删
        redisTemplate.delete(errMsgRedisKey);

        if (Tool.CollUtil.isNotEmpty(errMsgList)) {
            // 后增
            redisTemplate.opsForList().rightPushAll(errMsgRedisKey, errMsgList);
        }

        // 提示忽略
        this.recordImportAttrErr(lineNum, RCode.IMPORT_IGNORE.setParam(header));
    }

    /**
     * 校验 value 必填
     * @param value
     * @param lineNum
     * @param header
     */
    public void validateRequired(Object value, int lineNum, String header) {
        if (ObjectUtil.isEmpty(value)) {
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(header));
        }
    }

    /**
     * 校验索引列 name 是否重复
     *
     * @return
     */
    public boolean validateIndexDuplicate(String tableName, String colName, String value, boolean isUpdate, int lineNum, String header) {
        List list = basicImportAndExportServices.queryList(tableName, colName, value);

        if (Tool.CollectionUtil.isNotEmpty(list) && !isUpdate) {
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(header));
            return true;
        }
        return false;
    }


    /**
     * 校验其他属性是否重复
     * 根据 index & isUpdate 修改时校验是否重复
     */
    public void validateDuplicate(String tableName, String colName, String value, String index, boolean isUpdate, int lineNum, String header) {
        List<Map<String, Object>> list = basicImportAndExportServices.queryList(tableName, colName, value);

        if (isUpdate) {
            // 更新的情况下
            boolean flag = false;
            // 当只有一个结果并且这个结果是自身时，或没有结果时，说明结果不重复
            if (Tool.CollectionUtil.isEmpty(list) || (list != null && list.size() == 1 && StrUtil.equals(list.get(0).get("name").toString(), index))) {
                flag = true;
            }

            if (!flag)
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(header));
        } else {
            if (Tool.CollectionUtil.isNotEmpty(list))
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(header));
        }
    }

    /**
     * 根据正则校验内容是否匹配
     */
    public void validateRegex(String value, String regex, int lineNum, String header) {
        if (!ReUtil.isMatch(regex, value)) {
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
        }
    }


    /**
     * 校验是否是数字格式
     */
    public Integer validateInteger(String value, int lineNum, String header) {
        boolean isInt = Tool.NumberUtil.isInteger(value);
        if (!isInt) {
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
            return null;
        } else {
            return Integer.parseInt(value);
        }
    }


    /**
     * 校验时间数据是否符合指定格式
     */
    public void validateDateFormat(String value, String pattern, int lineNum, String header) {
        if (StrUtil.isNotEmpty(value)) {
            try {
                Date date = DateUtils.parseDate(value, new String[]{pattern});
            } catch (ParseException e) {
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
            }
        }
    }


    /**
     * 校验端口是否符合规范
     */
    public void validatePort(String port, int lineNum, String header) {
        if (StrUtil.isNotEmpty(port)) {
            if (!CommonUtils.checkPort(port)) {
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
            }
        }
    }

    /**
     * 校验内容是否在 枚举数组内
     */
    public void validateEnumValArr(String value, List<String> enumList, int lineNum, String header) {
        if (BooleanUtil.and(StrUtil.isNotEmpty(value), CollectionUtil.isNotEmpty(enumList))) {
            if (!enumList.contains(value)) {
                this.recordImportAttrErr(lineNum, RCode.IMPORT_ENUM_VALUE_ERROR.setParam(header));
            }
        }
    }

    /**
     * 校验关联对象是否存在 单表
     *
     * @allowAutoCreate 自动添加时为true: 不存在也不记录错误信息
     */
    public Boolean validateRelObjExits(String tableName, String colName, String value, Boolean allowAutoCreate, int lineNum, String header) {
        List list = basicImportAndExportServices.queryList(tableName, colName, value);

        // 允许自动创建,只返回是否存在标识，不记录错误信息
        if (allowAutoCreate) {
            // 不存在
            if (Tool.CollectionUtil.isEmpty(list)) return false;
        } else {
            if (Tool.CollectionUtil.isEmpty(list)) {
                // 不存在
                this.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(header));
                return false;
            }
        }
        return true;
    }

    /**
     * 记录错误信息
     */
    public void recordImportAttrErr(int lineNum, RCode rCode) {
        String msg = sysI18nService.queryValue(Tool.StrUtil.toString(rCode), rCode.getParam());

        if (Tool.StrUtil.isEmpty(msg)) {
            msg = sysI18nService.queryValueByName(Tool.StrUtil.toString(rCode));
        }

        if (Tool.StrUtil.isEmpty(msg)) {
            msg = rCode.getMsg();
        }

        Map failDetail = Tool.MapUtil.newHashMap(4);
        failDetail.put("lineNo", lineNum);
        failDetail.put("errorMsg", msg);

        redisTemplate.opsForList().rightPush(errMsgRedisKey, Tool.JSONUtil.toJsonStr(failDetail));
    }

}
