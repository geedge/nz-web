package com.nis.modules.dashboard.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetModelImportEnum;
import com.nis.modules.dashboard.dao.VisualNotebookDao;
import com.nis.modules.dashboard.entity.VisualNotebook;
import com.nis.modules.dashboard.service.VisualNotebookService;
import com.nis.modules.dashboard.utils.DashboardConstant;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * visual notebook service implement
 *
 * @author shizhendong
 * @date 2023-08-16
 */
@Service
public class VisualNotebookServiceImpl extends ServiceImpl<VisualNotebookDao, VisualNotebook> implements VisualNotebookService {

    private static final Log log = Log.get();

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysConfService sysConfService;

    @Override
    public VisualNotebook queryNotebookInfo(Integer id) {
        VisualNotebook notebook = this.getById(id);
        ValidateUtils.is(notebook).notNull(RCode.NOTEBOOK_NOT_EXIST);

        SysUserEntity createUser = sysUserService.getById(notebook.getCid());
        if (Tool.ObjectUtil.isNotNull(createUser)) {
            notebook.setCreateUser(Tool.MapUtil.builder()
                    .put("id", createUser.getId())
                    .put("name", createUser.getName())
                    .build());
        }

        SysUserEntity updateUser = sysUserService.getById(notebook.getUid());
        if (Tool.ObjectUtil.isNotNull(updateUser)) {
            notebook.setUpdateUser(Tool.MapUtil.builder()
                    .put("id", updateUser.getId())
                    .put("name", updateUser.getName())
                    .build());
        }

        Object notebookParam = notebook.getParam();
        if (Tool.ObjectUtil.isNotEmpty(notebookParam)) {
            try {
                notebook.setParam(JSONUtil.toBean(StrUtil.utf8Str(notebookParam), Map.class));
            } catch (Exception e) {
                log.error(e, "[queryNotebookInfo] [param json format error] [id: {}]", id);
            }
        }

        Object notebookCharts = notebook.getCharts();
        if (Tool.ObjectUtil.isNotEmpty(notebookCharts)) {
            try {
                notebook.setCharts(JSONUtil.toList(StrUtil.utf8Str(notebookCharts), Map.class));
            } catch (Exception e) {
                log.error(e, "[queryNotebookInfo] [charts json format error] [id: {}]", id);
            }
        }
        return notebook;
    }

    @Override
    public PageUtils queryNotebookPage(Map<String, Object> params) {
        String ids = (String) params.get("ids");
        String name = (String) params.get("name");
        Integer cid = (Integer) params.get("cid");
        Integer uid = (Integer) params.get("uid");
        String type = (String) params.get("type");

        IPage page = new Query(VisualNotebook.class).getPage(params);
        LambdaQueryWrapper queryWrapper = new LambdaQueryWrapper<VisualNotebook>()
                .in(Tool.StrUtil.isNotEmpty(ids), VisualNotebook::getId, Tool.StrUtil.split(ids, ","))
                .like(Tool.StrUtil.isNotEmpty(name), VisualNotebook::getName, Tool.StrUtil.likeEscape(name))
                .eq(Tool.ObjectUtil.isNotEmpty(cid), VisualNotebook::getCid, cid)
                .eq(Tool.ObjectUtil.isNotEmpty(uid), VisualNotebook::getUid, uid)
                .eq(Tool.StrUtil.isNotEmpty(type), VisualNotebook::getType, type);

        IPage<VisualNotebook> result = this.page(page, queryWrapper);
        Map<Integer, SysUserEntity> userInfoCache = MapUtil.newHashMap();
        for (VisualNotebook notebook : result.getRecords()) {
            Object notebookParam = notebook.getParam();
            if (Tool.ObjectUtil.isNotEmpty(notebookParam)) {
                try {
                    notebook.setParam(JSONUtil.toBean(StrUtil.utf8Str(notebookParam), Map.class));
                } catch (Exception e) {
                    log.error(e, "[queryNotebookPage] [param json format error] [id: {}]", notebook.getId());
                }
            }

            Object notebookCharts = notebook.getCharts();
            if (Tool.ObjectUtil.isNotEmpty(notebookCharts)) {
                try {
                    notebook.setCharts(JSONUtil.toList(StrUtil.utf8Str(notebookCharts), Map.class));
                } catch (Exception e) {
                    log.error(e, "[queryNotebookPage] [charts json format error] [id: {}]", notebook.getId());
                }
            }

            SysUserEntity createUser = userInfoCache.get(notebook.getCid());
            if (Tool.ObjectUtil.isNull(createUser) && Tool.ObjectUtil.notEqual(-1, notebook.getCid())) {
                createUser = sysUserService.getById(notebook.getCid());
                userInfoCache.put(notebook.getCid(), createUser);
            }
            if (Tool.ObjectUtil.isNotNull(createUser)) {
                notebook.setCreateUser(Tool.MapUtil.builder()
                        .put("id", createUser.getId())
                        .put("name", createUser.getName())
                        .build());
            }

            SysUserEntity updateUser = userInfoCache.get(notebook.getUid());
            if (Tool.ObjectUtil.isNull(updateUser) && Tool.ObjectUtil.notEqual(-1, notebook.getUid())) {
                updateUser = sysUserService.getById(notebook.getUid());
                userInfoCache.put(notebook.getUid(), updateUser);
            }
            if (Tool.ObjectUtil.isNotNull(updateUser)) {
                notebook.setUpdateUser(Tool.MapUtil.builder()
                        .put("id", updateUser.getId())
                        .put("name", updateUser.getName())
                        .build());
            }
        }
        return new PageUtils(result);
    }

    /**
     * validate notebook
     *
     * @param notebook
     */
    private void validateNotebook(VisualNotebook notebook) {
        // type
        String type = notebook.getType();
        type = StrUtil.emptyToDefault(type, DashboardConstant.NotebookType.NONE.getValue());

        DashboardConstant.NotebookType instance = DashboardConstant.NotebookType.getInstanceByValue(type);
        ValidateUtils.is(instance).notNull(RCode.NOTEBOOK_TYPE_INVALID);

        // name
        String name = notebook.getName();
        VisualNotebook tempNotebookForValidateNameDuplicate = this.getOne(new LambdaQueryWrapper<VisualNotebook>()
                .eq(VisualNotebook::getName, name)
                .eq(VisualNotebook::getType, type)
                .ne(ObjectUtil.isNotNull(notebook.getId()), VisualNotebook::getId, notebook.getId())
                .last("limit 1"));

        if (ToolUtil.isNotEmpty(tempNotebookForValidateNameDuplicate)) {
            throw new NZException(RCode.NOTEBOOK_NAME_DUPLICATE);
        }

        // param
        Object param = notebook.getParam();
        if (ObjectUtil.isNotEmpty(param)) {
            boolean typeJSONObject = JSONUtil.isTypeJSONObject(StrUtil.utf8Str(param));
            if (BooleanUtil.negate(typeJSONObject)) {
                throw new NZException(RCode.NOTEBOOK_PARAM_FORMAT);
            }
        }

        // charts
        Object charts = notebook.getCharts();
        if (ObjectUtil.isNotEmpty(charts)) {
            boolean typeJSONArray = JSONUtil.isTypeJSONArray(StrUtil.utf8Str(charts));
            if (BooleanUtil.negate(typeJSONArray)) {
                throw new NZException(RCode.NOTEBOOK_CHARTS_FORMAT);
            }
        }
    }

    @Override
    public Integer saveOrUpdateNotebook(VisualNotebook notebook) {
        // validate notebook
        this.validateNotebook(notebook);

        // set default value
        notebook.setType(ObjectUtil.defaultIfNull(notebook.getType(), DashboardConstant.NotebookType.NONE.getValue()));

        // param
        if (ObjectUtil.isNotEmpty(notebook.getParam())) {
            notebook.setParam(JSONUtil.toJsonStr(notebook.getParam()));
        } else {
            notebook.setParam(StrUtil.EMPTY_JSON);
        }

        // charts
        if (ObjectUtil.isNotEmpty(notebook.getCharts())) {
            notebook.setCharts(JSONUtil.toJsonStr(notebook.getCharts()));
        } else {
            notebook.setCharts(JSONUtil.toJsonStr(Tool.ListUtil.empty()));
        }


        // cid&cts
        Integer id = notebook.getId();
        if (ObjectUtil.isNull(id)) {
            notebook.setCid(ShiroUtils.getUserId().intValue());
            notebook.setCts(System.currentTimeMillis());
        } else {
            VisualNotebook tempNotebookForJudge = this.getById(id);
            if (ObjectUtil.isNull(tempNotebookForJudge)) {
                notebook.setCid(ShiroUtils.getUserId().intValue());
                notebook.setCts(System.currentTimeMillis());
            }
        }

        // uid&uts
        notebook.setUid(ShiroUtils.getUserId().intValue());
        notebook.setUts(System.currentTimeMillis());

        // save or update
        this.saveOrUpdate(notebook);
        return notebook.getId();
    }

    @Override
    public void duplicateNotebook(Integer id, String name) {
        VisualNotebook notebook = this.getById(id);
        ValidateUtils.is(notebook).notNull(RCode.NOTEBOOK_NOT_EXIST);

        // validate name duplicate
        String duplicateNotebookName = StrUtil.emptyToDefault(name, "copy from " + notebook.getName());
        duplicateNotebookName = StrUtil.trim(duplicateNotebookName);

        VisualNotebook tempNotebookForValidateNameDuplicate = this.getOne(new LambdaQueryWrapper<VisualNotebook>()
                .eq(VisualNotebook::getName, duplicateNotebookName)
                .eq(VisualNotebook::getType, notebook.getType())
                .last("limit 1"));

        if (ToolUtil.isNotEmpty(tempNotebookForValidateNameDuplicate)) {
            throw new NZException(RCode.NOTEBOOK_NAME_DUPLICATE);
        }

        notebook.setId(null);
        notebook.setName(duplicateNotebookName);

        notebook.setCid(ShiroUtils.getUserId().intValue());
        notebook.setCts(System.currentTimeMillis());

        notebook.setUid(ShiroUtils.getUserId().intValue());
        notebook.setUts(System.currentTimeMillis());

        notebook.setBuildIn(0);
        this.save(notebook);
    }

    @Override
    public void deleteNotebook(List<Integer> idList) {
        List<VisualNotebook> delNotebookList = this.list(new LambdaQueryWrapper<VisualNotebook>().in(VisualNotebook::getId, idList));

        // validate build-in data
        VisualNotebook findFirstBuildInNotebook = delNotebookList.stream().filter(t -> ObjectUtil.equal(t.getBuildIn(), 1)).findFirst().orElse(null);
        if (ObjectUtil.isNotNull(findFirstBuildInNotebook)) {
            throw new NZException(RCode.NOTEBOOK_BUILTIN_CANNOT_DELETED);
        }

        // remove
        this.removeByIds(idList);
    }

    @Override
    public Map<Object, Object> importNotebook(MultipartFile jsonFile, Integer existed, Integer ignoreError) {
        List<VisualNotebook> importNotebookList = ListUtil.list(true);
        try {
            String readFileContext = IoUtil.readUtf8(jsonFile.getInputStream());
            importNotebookList = JSONUtil.toList(readFileContext, VisualNotebook.class);
        } catch (Exception e) {
            log.error(e, "[importNotebook] [Import file parsing failed] [file: {}]", jsonFile.getOriginalFilename());
            throw new NZException(RCode.IMPORT_FILE_PARSE_ERROR);
        }

        List<String> headerInfoList = Stream.of(DashboardConstant.NotebookImportEnum.values())
                .sorted(Comparator.comparing(DashboardConstant.NotebookImportEnum::getIndex))
                .map(DashboardConstant.NotebookImportEnum::getName)
                .collect(Collectors.toList());

        Map<Object, Object> params = MapUtil.builder()
                .put("seq", StrUtil.uuid())
                .put("existed", StrUtil.toString(existed))
                .put("ignoreError", StrUtil.toString(ignoreError))
                .build();

        Map<Object, Object> statMap = null;
        try {
            statMap = this.saveImportNotebookList(importNotebookList, headerInfoList, params);
        } catch (Exception e) {
            log.error(e, "[importNotebook] [error] [data: {}] [existed: {}] [ignoreError: {}]", JSONUtil.toJsonStr(importNotebookList), existed, ignoreError);
            throw new NZException(RCode.ERROR);
        }
        return statMap;
    }

    /**
     * save import notebook list
     *
     * @param notebookList
     * @param headerInfoList
     * @param params
     * @return
     */
    private Map<Object, Object> saveImportNotebookList(List<VisualNotebook> notebookList, List<String> headerInfoList, Map<Object, Object> params) {
        String existed = MapUtil.getStr(params, "existed");
        String ignoreError = MapUtil.getStr(params, "ignoreError");

        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfoList)
                .build();
        Integer addNum = 0, updateNum = 0;
        List<VisualNotebook> failContentList = ListUtil.list(true);
        for (int i = 0; i < notebookList.size(); i++) {
            Integer lineNum = i + 1;
            VisualNotebook notebook = notebookList.get(i);
            validateUtil.validateRequired(notebook.getName(), lineNum, DashboardConstant.NotebookImportEnum.NAME.getIndex());

            String type = notebook.getType();
            type = StrUtil.emptyToDefault(type, DashboardConstant.NotebookType.NONE.getValue());

            List<String> typeEnumValueList = Stream.of(DashboardConstant.NotebookType.values()).map(DashboardConstant.NotebookType::getValue).collect(Collectors.toList());
            validateUtil.validateEnumValArr(type, typeEnumValueList, lineNum, DashboardConstant.NotebookImportEnum.TYPE.getIndex());
            // name duplicate
            VisualNotebook tempNotebookForValidateNameDuplicate = this.getOne(new LambdaQueryWrapper<VisualNotebook>()
                    .eq(VisualNotebook::getType, type)
                    .eq(VisualNotebook::getName, notebook.getName())
                    .last("limit 1"));
            if (Tool.ObjectUtil.isNotEmpty(tempNotebookForValidateNameDuplicate)) {
                if (StrUtil.equals("0", existed)) {
                    validateUtil.recordImportAttrErr(lineNum, RCode.NOTEBOOK_NAME_DUPLICATE);
                } else if (StrUtil.equals("2", existed)) {
                    validateUtil.ignoreThisRow(lineNum, AssetModelImportEnum.NAME.getIndex());
                    failContentList.add(notebook);
                    continue;
                }
            }

            Object param = notebook.getParam();
            if (ObjectUtil.isNotEmpty(param)) {
                boolean typeJSONObject = JSONUtil.isTypeJSONObject(StrUtil.utf8Str(param));
                if (BooleanUtil.negate(typeJSONObject))
                    validateUtil.recordImportAttrErr(lineNum, RCode.NOTEBOOK_PARAM_FORMAT);
            }

            Object charts = notebook.getCharts();
            if (ObjectUtil.isNotEmpty(charts)) {
                boolean typeJSONArray = JSONUtil.isTypeJSONArray(StrUtil.utf8Str(charts));
                if (BooleanUtil.negate(typeJSONArray))
                    validateUtil.recordImportAttrErr(lineNum, RCode.NOTEBOOK_CHARTS_FORMAT);
            }

            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                for (int j = i; j < notebookList.size(); j++) {
                    validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
                    failContentList.add(notebookList.get(j));
                }
                break;
            }

            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                try {
                    this.saveOrUpdateRowData(notebook, params);
                    // save or update
                    if (ObjectUtil.isNotNull(params.get("save"))) {
                        addNum++;
                    } else {
                        updateNum++;
                    }
                } catch (Exception e) {
                    log.error(e, "[saveImportNotebookList] [error] [save info: {}]", JSONUtil.toJsonStr(notebook));
                    failContentList.add(notebook);
                    validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                }
            } else {
                failContentList.add(notebook);
            }
        }

        for (VisualNotebook notebook : failContentList) {
            if (Tool.ObjectUtil.isNotEmpty(notebook.getParam()))
                notebook.setParam(JSONUtil.toBean(StrUtil.utf8Str(notebook.getParam()), Map.class));
            if (Tool.ObjectUtil.isNotEmpty(notebook.getCharts()))
                notebook.setCharts(JSONUtil.toList(StrUtil.utf8Str(notebook.getCharts()), Map.class));
        }

        Map<Object, Object> statMap = Tool.MapUtil.builder()
                .put("seq", MapUtil.getStr(params, "seq"))
                .put("totalNum", notebookList.size())
                .put("successNum", addNum + updateNum)
                .put("failNum", failContentList.size())
                .put("addNum", addNum)
                .put("updateNum", updateNum)
                .put("failDetail", validateUtil.getImportErrMsg())
                .put("failContent", Base64.encode(StrUtil.bytes(JSONObject.toJSONString(failContentList, SerializerFeature.PrettyFormat, SerializerFeature.SortField))))
                .build();
        return statMap;
    }

    /**
     * save or update row data
     *
     * @param notebook
     * @param params
     */
    private void saveOrUpdateRowData(VisualNotebook notebook, Map<Object, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        String seq = MapUtil.getStr(params, "seq");

        // set field value
        notebook.setId(null);
        notebook.setType(ObjectUtil.defaultIfNull(notebook.getType(), DashboardConstant.NotebookType.NONE.getValue()));
        notebook.setSeq(seq);

        if (ObjectUtil.isNotEmpty(notebook.getParam())) {
            notebook.setParam(JSONUtil.toJsonStr(notebook.getParam()));
        } else {
            notebook.setParam(StrUtil.EMPTY_JSON);
        }

        if (ObjectUtil.isNotEmpty(notebook.getCharts())) {
            notebook.setCharts(JSONUtil.toJsonStr(notebook.getCharts()));
        } else {
            notebook.setCharts(JSONUtil.toJsonStr(Tool.ListUtil.empty()));
        }

        // save or update
        String existed = MapUtil.getStr(params, "existed");
        VisualNotebook oldNotebook = this.getOne(new LambdaQueryWrapper<VisualNotebook>()
                .eq(VisualNotebook::getName, notebook.getName())
                .eq(VisualNotebook::getType, notebook.getType()));
        if (StrUtil.equals("1", existed) && oldNotebook != null) {
            params.put("update", true);
            // update
            notebook.setId(oldNotebook.getId());

            // uid&uts
            notebook.setUid(ShiroUtils.getUserId().intValue());
            notebook.setUts(System.currentTimeMillis());
            this.updateById(notebook);
        } else {
            params.put("save", true);

            notebook.setCid(ShiroUtils.getUserId().intValue());
            notebook.setCts(System.currentTimeMillis());
            notebook.setUid(ShiroUtils.getUserId().intValue());
            notebook.setUts(System.currentTimeMillis());

            this.save(notebook);
        }
    }

    @Override
    public void exportNotebook(HttpServletResponse response, Map<String, Object> params) {
        try {
            String maxLine = sysConfService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
            long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));

            Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
            Long pageSize = Long.parseLong(limit.toString());
            if (pageSize == -1) {
                params.put(Constant.PAGE, "1");
                params.put(Constant.LIMIT, Long.toString(exportMaxLine));
            } else if (pageSize > exportMaxLine) {
                params.put(Constant.LIMIT, Long.toString(exportMaxLine));
            }

            String ids = (String) params.get("ids");
            String name = (String) params.get("name");
            IPage<VisualNotebook> result = this.page(
                    new Query(VisualNotebook.class).getPage(params),
                    new LambdaQueryWrapper<VisualNotebook>()
                            .in(Tool.StrUtil.isNotEmpty(ids), VisualNotebook::getId, Tool.StrUtil.split(ids, ","))
                            .like(Tool.StrUtil.isNotEmpty(name), VisualNotebook::getName, Tool.StrUtil.likeEscape(name)));
            List<VisualNotebook> records = result.getRecords();

            List<Map<String, Object>> exportDataList = ListUtil.list(true);
            for (VisualNotebook notebook : records) {
                Map<String, Object> tempMap = MapUtil.newHashMap(true);
                tempMap.put(DashboardConstant.NotebookImportEnum.NAME.getName(), notebook.getName());
                tempMap.put(DashboardConstant.NotebookImportEnum.TYPE.getName(), notebook.getType());
                tempMap.put(DashboardConstant.NotebookImportEnum.REMARK.getName(), notebook.getRemark());
                tempMap.put(DashboardConstant.NotebookImportEnum.PARAM.getName(), JSONUtil.toBean(StrUtil.utf8Str(notebook.getParam()), Map.class));
                tempMap.put(DashboardConstant.NotebookImportEnum.CHARTS.getName(), JSONUtil.toList(StrUtil.utf8Str(notebook.getCharts()), Map.class));
                exportDataList.add(tempMap);
            }

            String fileName = "Notebook.json";
            byte[] fileContentByteArr = StrUtil.bytes(JSONUtil.toJsonPrettyStr(exportDataList));
            Tool.ResponseUtil.downloadFile(response, fileName, fileContentByteArr);
        } catch (Exception e) {
            log.error(e, "[exportNotebook] [error] [param: {}]", JSONUtil.toJsonStr(params));
            try {
                response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error");
            } catch (IOException ex) {
                log.error(ex);
            }
        }
    }

}
