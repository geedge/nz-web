package com.nis.common.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.*;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysI18nService;
import com.sun.jmx.snmp.SnmpOid;
import lombok.Builder;
import lombok.Data;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目导入校验工具类、
 * 可根据需要自行补充校验方法
 */
@Data
@Builder
public class ImportValidateUtil {

    private static SysI18nService sysI18nService;

    private static BasicImportAndExportServices basicImportAndExportServices;

    static {
        sysI18nService = SpringContextUtils.getBean(SysI18nService.class);
        basicImportAndExportServices = SpringContextUtils.getBean(BasicImportAndExportServices.class);
    }

    // 错误提示表头信息
    private List<String> headerInfos;

    /**
     * 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
     * 校验工具类只对 1：覆盖 override 进行处理
     */
    private String existed;

    // 存储导入错误信息
    private final ThreadLocal<List<Map<String, String>>> threadLocal = new ThreadLocal();

    /**
     * 此行忽略并重置错误信息，只提示忽略本行
     *
     * @param lineNum
     * @param headerIndex
     */
    public void ignoreThisRow(Integer lineNum, int headerIndex) {
        // 删除之前的错误信息
        List<Map<String, String>> errMsgList = threadLocal.get();
        errMsgList = Tool.CollUtil.isEmpty(errMsgList) ? Tool.ListUtil.list(true) : errMsgList;

        errMsgList = errMsgList.stream().filter(map -> {
            String lineNo = map.get("lineNo");
            if (!Tool.StrUtil.equals(Tool.StrUtil.toString(lineNum), lineNo)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        // 先删
        threadLocal.remove();
        if (Tool.CollUtil.isNotEmpty(errMsgList)) {
            // 后增
            threadLocal.set(errMsgList);
        }

        // 提示忽略
        String header = headerInfos.get(headerIndex);
        this.recordImportAttrErr(lineNum, RCode.IMPORT_IGNORE.setParam(header));
    }

    /**
     * 校验 value 必填
     * @param value
     * @param lineNum
     * @param headerIndex
     */
    public void validateRequired(Object value, int lineNum, int headerIndex) {
        if (ObjectUtil.isEmpty(value)) {
            String header = headerInfos.get(headerIndex);
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_REQUIRED.setParam(header));
        }
    }

    /**
     * 校验索引列 name 是否重复
     *
     * @return
     */
    public boolean validateIndexDuplicate(String tableName, String colName, String value, int lineNum, int headerIndex) {
        List list = basicImportAndExportServices.queryList(tableName, colName, value);

        if (Tool.CollectionUtil.isNotEmpty(list) && !StrUtil.equals("1", existed)) {
            String header = headerInfos.get(headerIndex);
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(header));
            return true;
        }
        return false;
    }


    /**
     * 校验其他属性是否重复
     * 根据 index & isUpdate 修改时校验是否重复
     */
    public void validateDuplicate(String tableName, String colName, String value, String index, int lineNum, int headerIndex) {
        List<Map<String, Object>> list = basicImportAndExportServices.queryList(tableName, colName, value);

        if (StrUtil.equals("1", existed)) {
            // 更新的情况下
            boolean flag = false;
            // 当只有一个结果并且这个结果是自身时，或没有结果时，说明结果不重复
            if (Tool.CollectionUtil.isEmpty(list) || (list != null && list.size() == 1 && StrUtil.equals(list.get(0).get("name").toString(), index))) {
                flag = true;
            }

            if (!flag){
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(header));
            }
        } else {
            if (Tool.CollectionUtil.isNotEmpty(list)){
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(header));
            }
        }
    }

    /**
     * 根据正则校验内容是否匹配
     */
    public void validateRegex(String value, String regex, int lineNum, int headerIndex) {
        if (!ReUtil.isMatch(regex, value)) {
            String header = headerInfos.get(headerIndex);
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
        }
    }


    /**
     * 校验是否是数字格式
     */
    public Integer validateInteger(String value, int lineNum, int headerIndex) {
        boolean isInt = Tool.NumberUtil.isInteger(value);
        if (!isInt) {
            String header = headerInfos.get(headerIndex);
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
            return null;
        } else {
            return Integer.parseInt(value);
        }
    }

    /**
     * 校验是否是double格式
     */
    public Double validateDouble(String value, int lineNum, int headerIndex) {
        try {
            Double aDouble = Double.valueOf(value);
            return aDouble;
        } catch (NumberFormatException e) {
            String header = headerInfos.get(headerIndex);
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
        }
        return null;
    }

    /**
     * 校验时间数据是否符合指定格式
     */
    public Date validateDateFormat(String value, String pattern, int lineNum, int headerIndex) {
        if (StrUtil.isNotEmpty(value)) {
            try {
                Date date = DateUtils.parseDate(value, new String[]{pattern});
                return date;
            } catch (ParseException e) {
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
            }
        }
        return null;
    }


    /**
     * 校验端口是否符合规范
     */
    public void validatePort(String port, int lineNum, int headerIndex) {
        if (StrUtil.isNotEmpty(port)) {
            if (!CommonUtils.checkPort(port)) {
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
            }
        }
    }

    /**
     * 校验端口是否符合规范
     */
    public void validateIp(String ip, int lineNum, int headerIndex) {
        if (StrUtil.isNotEmpty(ip)) {
            if (!CommonUtils.checkIp(ip)) {
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
            }
        }
    }

    /**
     *
     * 校验 SNMP OID 是否符合规范
     */
    public SnmpOid validateSnmpOID(String oid, int lineNum, int headerIndex) {
        try {
            SnmpOid snmpOid = new SnmpOid(oid);
            return snmpOid;
        } catch (IllegalArgumentException e) {
            String header = headerInfos.get(headerIndex);
            this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
        }
        return null;
    }

    /**
     * 校验字符串是否为 jsonarray 格式
     */
    public boolean validateJsonArray(String jsonStr, Integer lineNum, int headerIndex) {
        if (StrUtil.isNotEmpty(jsonStr)) {
            try {
                JSONObject.parseArray(jsonStr);
                return true;
            } catch (JSONException e) {
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_FORMAT_ERROR.setParam(header));
                return false;
            }
        }
        return true;
    }

    /**
     * 校验内容是否在 枚举数组内
     */
    public void validateEnumValArr(String value, List<String> enumList, int lineNum, int headerIndex) {
        if (BooleanUtil.and(StrUtil.isNotEmpty(value), CollectionUtil.isNotEmpty(enumList))) {
            if (!enumList.contains(value)) {
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_ENUM_VALUE_ERROR.setParam(header));
            }
        }
    }

    /**
     * 校验关联对象是否存在 单表
     *
     * @allowAutoCreate 自动添加时为true: 不存在也不记录错误信息
     */
    public Boolean validateRelObjExits(String tableName, String colName, String value, Boolean allowAutoCreate, int lineNum, int headerIndex) {
        List list = basicImportAndExportServices.queryList(tableName, colName, value);

        // 允许自动创建,只返回是否存在标识，不记录错误信息
        if (allowAutoCreate) {
            // 不存在
            if (Tool.CollectionUtil.isEmpty(list)) return false;
        } else {
            if (Tool.CollectionUtil.isEmpty(list)) {
                // 不存在
                String header = headerInfos.get(headerIndex);
                this.recordImportAttrErr(lineNum, RCode.IMPORT_RELATED_OBJ_NOT_EXIST.setParam(header));
                return false;
            }
        }
        return true;
    }

    /**
     * 记录错误信息
     * rCode.param 支持填写 导入属性下标，自动转换为可读表头信息
     */
    public void recordImportAttrErr(int lineNum, RCode rCode) {
        Object[] param = rCode.getParam();
        List rCodeParamList = new ArrayList();
        if (Tool.ObjectUtil.isNotEmpty(param)) {
            for (Object o : param) {
                boolean isNumber = Tool.NumberUtil.isNumber(o.toString());
                if (isNumber) {
                    String s = headerInfos.get(Integer.valueOf(o.toString()));
                    rCodeParamList.add(s);
                } else {
                    rCodeParamList.add(o);
                }
            }
        }
        String msg = sysI18nService.queryValue(Tool.StrUtil.toString(rCode), rCodeParamList.toArray());

        if (Tool.StrUtil.isEmpty(msg)) {
            msg = sysI18nService.queryValueByName(Tool.StrUtil.toString(rCode));
        }

        if (Tool.StrUtil.isEmpty(msg)) {
            msg = rCode.getMsg();
        }

        Map failDetail = Tool.MapUtil.builder()
                .put("lineNo", StrUtil.toString(lineNum))
                .put("errorMsg", msg).build();

        List<Map<String, String>> errMsgList = threadLocal.get();
        errMsgList = Tool.CollUtil.isEmpty(errMsgList) ? Tool.ListUtil.list(true) : errMsgList;
        errMsgList.add(failDetail);
        errMsgList = errMsgList.stream().distinct().collect(Collectors.toList());

        threadLocal.set(errMsgList);
    }


    /**
     * 获取导入错误信息
     *
     * @return
     */
    public List<Map<String, String>> getImportErrMsg() {
        List<Map<String, String>> errMsgList = threadLocal.get();
        errMsgList = Tool.CollUtil.isEmpty(errMsgList) ? Tool.ListUtil.list(true) : errMsgList;

        threadLocal.remove();
        return errMsgList;
    }

    /**
     * 指定行号，返回该行错误信息
     *
     * @param lineNum
     * @return
     */
    public List<Map<String, String>> getImportErrMsgForThisLine(Integer lineNum) {
        List<Map<String, String>> errMsgList = threadLocal.get();
        errMsgList = Tool.CollUtil.isEmpty(errMsgList) ? Tool.ListUtil.list(true) : errMsgList;

        List<Map<String, String>> list = errMsgList.stream().filter(map -> {
            String lineNo = map.get("lineNo");
            if (Tool.StrUtil.equals(Tool.StrUtil.toString(lineNum), lineNo)) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        return list;
    }
}
