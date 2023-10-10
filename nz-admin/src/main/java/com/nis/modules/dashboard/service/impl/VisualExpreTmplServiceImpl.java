package com.nis.modules.dashboard.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.dashboard.dao.VisualExpreTmplDao;
import com.nis.modules.dashboard.entity.ExprTmplImportEnum;
import com.nis.modules.dashboard.entity.VisualExpressionTmpl;
import com.nis.modules.dashboard.service.VisualExpreTmplService;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 表达式模板 service 实现类
 *
 * @author shizhendong
 * @date 2021-03-11
 */
@Service
public class VisualExpreTmplServiceImpl extends ServiceImpl<VisualExpreTmplDao, VisualExpressionTmpl> implements VisualExpreTmplService {

    private static Log logger = Log.get();

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private DcService dcService;

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private MonitorModuleService moduleService;

    @Autowired
    private MonitorProjectService projectService;

    @Autowired
    private MonitorEndpointService endpointService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

    @Override
    public VisualExpressionTmpl queryTmplInfoById(Integer id) {
    	VisualExpressionTmpl tmpl = this.getById(id);
        if (ToolUtil.isEmpty(tmpl)) {
            throw new NZException(RCode.EXPRETMPL_NOT_FOUND);
        }
        String[] vars = this.parseExpre(tmpl.getExpression());
        if (Tool.ObjectUtil.isNotEmpty(vars)) {
            List<String> list = Arrays.stream(vars).distinct().collect(Collectors.toList());
            vars = list.toArray(new String[list.size()]);
            tmpl.setVars(vars);
        }
        return tmpl;
    }

    private String[] parseExpre(String expre) {
        Pattern pattern = Pattern.compile("(\\{\\{)(.+?)(\\}\\})");
        Matcher matcher = pattern.matcher(expre);

        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            group = group.replaceAll("(\\{\\{)|(\\}\\})", "");
            list.add(group);
        }
        String[] vars = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            vars[i] = list.get(i);
        }
        return vars;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
    	String ids = (String) params.get("ids");
    	List<String> asList =null;
    	if(StrUtil.isNotEmpty(ids)) {
    		asList = Arrays.asList(ids.split(","));
    	}
        LambdaQueryWrapper<VisualExpressionTmpl> queryWrapper = new LambdaQueryWrapper<VisualExpressionTmpl>()
                .in(ToolUtil.isNotEmpty(params.get("ids")), VisualExpressionTmpl::getId, asList)
                .like(ToolUtil.isNotEmpty(params.get("name")), VisualExpressionTmpl::getName, Tool.StrUtil.likeEscape(Tool.MapUtil.getStr(params,"name")))
                .like(ToolUtil.isNotEmpty(params.get("gname")), VisualExpressionTmpl::getGname,Tool.StrUtil.likeEscape(Tool.MapUtil.getStr(params,"gname")));

        IPage page = this.page(new Query(VisualExpressionTmpl.class).getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    @Override
    public List<String> queryTmplsByGname(String gname) {
        List<VisualExpressionTmpl> list = this.list(new LambdaQueryWrapper<VisualExpressionTmpl>().like(ToolUtil.isNotEmpty(gname), VisualExpressionTmpl::getGname, Tool.StrUtil.likeEscape(gname)).orderByAsc(VisualExpressionTmpl::getGname));
        List<String> gnameList = list.stream().map(VisualExpressionTmpl::getGname).distinct().collect(Collectors.toList());
        return gnameList;
    }

    private void validateExpressionTmplInfo(VisualExpressionTmpl tmpl) {
        // 1. 名称重复
        List<VisualExpressionTmpl> list = this.list(new LambdaQueryWrapper<VisualExpressionTmpl>().eq(VisualExpressionTmpl::getGname, ToolUtil.isEmpty(tmpl.getGname()) ? "default" : tmpl.getGname()).eq(VisualExpressionTmpl::getName, tmpl.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(tmpl.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.EXPRETMPL_NAME_DUPLICATE);
        }

        // 2. 校验 build in 值
        List<Integer> switchList = Arrays.asList(new Integer[]{1, 0});
        if (ToolUtil.isNotEmpty(tmpl.getBuildIn()) && !switchList.contains(tmpl.getBuildIn())) {
            throw new NZException(RCode.EXPRETMPL_BUILDIN_INCORRECT);
        }
    }

    @Override
    public Integer saveTmpl(VisualExpressionTmpl tmpl) {
        // 1. 校验
        tmpl.setGname(StrUtil.trim(tmpl.getGname()));
        tmpl.setName(StrUtil.trim(tmpl.getName()));
        this.validateExpressionTmplInfo(tmpl);

        if (StringUtils.isEmpty(tmpl.getGname())) {
            tmpl.setGname("default");
        }

        // 保存
        this.save(tmpl);
        return tmpl.getId();
    }

    @Override
    public Integer updateTmpl(VisualExpressionTmpl tmpl) {
        // 1. 校验
        tmpl.setGname(StrUtil.trim(tmpl.getGname()));
        tmpl.setName(StrUtil.trim(tmpl.getName()));
        this.validateExpressionTmplInfo(tmpl);

        if (StringUtils.isEmpty(tmpl.getGname())) {
            tmpl.setGname("default");
        }

        // 保存
        this.updateById(tmpl);
        return tmpl.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer... ids) {
        List<VisualExpressionTmpl> list = this.list();

        // 1. 内置的 配置不允许删除
        List<Integer> buildInIdList = list.stream().filter(assetTypeConf -> assetTypeConf.getBuildIn().equals(1)).map(VisualExpressionTmpl::getId).collect(Collectors.toList());
        buildInIdList.retainAll(Arrays.asList(ids));
        if (CollectionUtils.isNotEmpty(buildInIdList)) {
            throw new NZException("These expression templates are built-in and cannot be deleted. Details: " + buildInIdList.toString(), RCode.EXPRETMPL_BUILDIN_CAN_NOT_REMOVE.getCode());
        }

        // 删除
        this.removeByIds(Arrays.asList(ids));
    }

    /**
     * 表达式渲染
     * @param tmpl ExpressionTmpl 对象 ，
     *             expression为表达式内容
     *             varsVal 是替换内容，其中key asset.datacenter project module endpoint ，value 为 id
     * @return
     */
    @Override
    public String renderExpression(VisualExpressionTmpl tmpl) {
        String expression = tmpl.getExpression();
        Map varsVal = tmpl.getVarsVal();

        String[] vars = this.parseExpre(expression);
        for (String var : vars) {
            Integer assetId = var.startsWith("asset.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer dcId = var.startsWith("datacenter.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer projectId = var.startsWith("project.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer moduleId = var.startsWith("module.") ? Integer.parseInt(varsVal.get(var).toString()) : null;
            Integer endpointId = var.startsWith("endpoint.") ? Integer.parseInt(varsVal.get(var).toString()) : null;

            AssetAsset asset = ToolUtil.isNotEmpty(assetId) ? assetService.getById(assetId) : null;
            Dc dc = ToolUtil.isNotEmpty(dcId) ? dcService.getById(dcId) : null;
            MonitorProject project = ToolUtil.isNotEmpty(projectId) ? projectService.getById(projectId) : null;
            MonitorModule module = ToolUtil.isNotEmpty(moduleId) ? moduleService.getById(moduleId) : null;
            MonitorEndpoint endpoint = ToolUtil.isNotEmpty(endpointId) ? endpointService.getById(endpointId) : null;

            Object renderValue = null;
            boolean flag = true;
            if (var.startsWith("asset.") || var.startsWith("datacenter.") || var.startsWith("project.") || var.startsWith("module.") || var.startsWith("endpoint.")) {
                flag = false;
            }
            renderValue = (var.startsWith("asset.") && ToolUtil.isNotEmpty(asset) && renderValue == null) ? BeanUtil.getProperty(asset, var.replaceAll("asset.", "")) : renderValue;
            renderValue = (var.startsWith("datacenter.") && ToolUtil.isNotEmpty(dc) && renderValue == null) ? BeanUtil.getProperty(dc, var.replaceAll("datacenter.", "")) : renderValue;
            renderValue = (var.startsWith("project.") && ToolUtil.isNotEmpty(project) && renderValue == null) ? BeanUtil.getProperty(project, var.replaceAll("project.", "")) : renderValue;
            renderValue = (var.startsWith("module.") && ToolUtil.isNotEmpty(module) && renderValue == null) ? BeanUtil.getProperty(module, var.replaceAll("module\\.", "")) : renderValue;
            renderValue = (var.startsWith("endpoint.") && ToolUtil.isNotEmpty(endpoint) && renderValue == null) ? BeanUtil.getProperty(endpoint, var.replaceAll("endpoint.", "")) : renderValue;
            renderValue = (renderValue == null && flag) ? varsVal.get(var) : renderValue;
            if (renderValue == null && flag) {
                continue;
            }
            expression = expression.replaceAll("\\{\\{" + var + "\\}\\}", renderValue == null ? "" : renderValue.toString());
        }
        return expression;
    }

    /**
     * 渲染表达式
     * @param expression  表达式
     * @param varsVal     表达式内容 直接进行 key value 渲染
     * @return
     */
    @Override
    public String renderExpression(String expression, Map varsVal) {
        expression = this.generateExpression(expression, varsVal);
        return expression;
    }

    private String generateExpression(String expression, Map varsVal) {
        String[] vars = this.parseExpre(expression);
        for (int i = 0; i < vars.length; i++) {
            Object o = varsVal.get(vars[i]);
            if (ToolUtil.isEmpty(o)) {
                continue;
            }
            expression = expression.replaceAll("\\{\\{" + vars[i] + "\\}\\}", o.toString());
        }
        return expression;
    }

    @Override
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER);
        String fileName = "ExpressionTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
    }

    @Override
    public Map importTmpls(MultipartFile multipartFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        String fileExtension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        params.put("fileExtension", fileExtension);

        // 返回统计信息
        Map statMap = this.saveTmplsFromImportDataList(importDataList, headerInfos, params);
        return statMap;
    }

    private Map saveTmplsFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) {
        String seq = StrUtil.uuid();
        params.put("seq", seq);
        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

        // 创建导入校验对象
        ImportValidateUtil validateUtil = ImportValidateUtil.builder()
                .existed(existed).headerInfos(headerInfos)
                .build();
        Integer addNum = 0, updateNum = 0;
        // 记录导入失败内容
        List<Map<Integer, String>> failContentList = ListUtil.list(true);
        for (int i = 0; i < importDataList.size(); i++) {
            int lineNum = i + 1;
            Map<Integer, String> dataMap = importDataList.get(i);

            String name = dataMap.get(ExprTmplImportEnum.NAME.getIndex());
            validateUtil.validateRequired(name, lineNum, ExprTmplImportEnum.NAME.getIndex());

            String gname = dataMap.get(1);
            gname = StrUtil.emptyToDefault(gname, "default");

            if (StrUtil.isNotEmpty(name)) {
                List<VisualExpressionTmpl> tList = this.list(new LambdaQueryWrapper<VisualExpressionTmpl>()
                        .eq(VisualExpressionTmpl::getGname, gname)
                        .eq(VisualExpressionTmpl::getName, name));
                if (Tool.CollUtil.isNotEmpty(tList)) {
                    if (StrUtil.equals("0", existed)) {
                        validateUtil.recordImportAttrErr(lineNum, RCode.IMPORT_FIELD_DUPLICATE.setParam(ExprTmplImportEnum.NAME.getIndex()));
                    } else if (StrUtil.equals("2", existed)) {
                        validateUtil.ignoreThisRow(lineNum, ExprTmplImportEnum.NAME.getIndex());
                        failContentList.add(dataMap);
                        continue;
                    }
                }
            }

            String expression = dataMap.get(ExprTmplImportEnum.EXPRESSION.getIndex());
            validateUtil.validateRequired(expression, lineNum, ExprTmplImportEnum.EXPRESSION.getIndex());

            String remark = dataMap.get(ExprTmplImportEnum.REMARK.getIndex());

            // 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
            String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");

            List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
            if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
                // 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
                for (int j = i; j < importDataList.size(); j++) {
                    validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
                    failContentList.add(importDataList.get(j));
                }
                break;
            }

            if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
                VisualExpressionTmpl tmpl = new VisualExpressionTmpl();
                tmpl.setName(name);
                tmpl.setGname(gname);
                tmpl.setExpression(expression);
                tmpl.setRemark(remark);
                try {
                    this.saveOrUpdateRowData(tmpl, params);
                    // save or update
                    Object saveFlag = params.get("save");
                    if (ObjectUtil.isNotNull(saveFlag)) {
                        addNum++;
                    } else {
                        updateNum++;
                    }
                } catch (Exception e) {
                    failContentList.add(dataMap);
                    validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
                    logger.error("Visual Expression Tmpl import roll back the transaction failure", e);
                }
            } else {
                failContentList.add(dataMap);
            }
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER);
        String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, MapUtil.getStr(params, "fileExtension"));
        //  返回统计信息
        Map<Object, Object> statMap = Tool.MapUtil.builder()
                .put("seq", seq)
                .put("totalNum", importDataList.size())
                .put("successNum", addNum + updateNum)
                .put("failNum", failContentList.size())
                .put("addNum", addNum)
                .put("updateNum", updateNum)
                .put("failDetail", validateUtil.getImportErrMsg())
                .put("failContent", failedContentFileBase64Str)
                .build();
        return statMap;
    }

    private void saveOrUpdateRowData(VisualExpressionTmpl tmpl, Map<String, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        String seq = params.get("seq").toString();
        tmpl.setSeq(seq);

        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore   在此只需要处理更新即可
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");
        VisualExpressionTmpl oldTmpl = this.getOne(new LambdaQueryWrapper<VisualExpressionTmpl>()
                .eq(VisualExpressionTmpl::getGname, tmpl.getGname())
                .eq(VisualExpressionTmpl::getName, tmpl.getName()));
        if (StrUtil.equals("1", existed) && oldTmpl != null) {
            params.put("update", true);
            // update
            tmpl.setId(oldTmpl.getId());

            LambdaUpdateWrapper<VisualExpressionTmpl> updateWrapper = new LambdaUpdateWrapper<VisualExpressionTmpl>()
                    .eq(VisualExpressionTmpl::getId, tmpl.getId())
                    .set(VisualExpressionTmpl::getRemark, tmpl.getRemark());

            this.update(tmpl, updateWrapper);
        } else {
            params.put("save", true);
            // insert
            this.save(tmpl);
        }
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
        String maxLine = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
        long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
        // 导出最大条数限制
        Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
        Long pageSize = Long.parseLong(limit.toString());
        if (pageSize == -1) {
            // 导出所有直接限制 pageSize
            params.put(Constant.PAGE, "1");
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        } else if (pageSize > exportMaxLine) {
            // 分页超过最大限制 则设置最大限制为 系统配置值，和 pageSize == -1 判断区别在于 pageNo
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        }
        PageUtils page = this.queryPage(params);
        List<VisualExpressionTmpl> list = (List<VisualExpressionTmpl>) page.getList();

        List<List<String>> exportDataList = this.handleExportDataList(list);

        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPRETMPL_EXPORT_HEADER);
        String fileName = "ExpressionTemplates";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    private List<List<String>> handleExportDataList(List<VisualExpressionTmpl> list) {
        List<List<String>> exportDataList = new ArrayList<>();
        for (VisualExpressionTmpl tmpl : list) {
            List<String> tempList = new ArrayList<>();
            tempList.add(tmpl.getName());
            tempList.add(tmpl.getGname());
            tempList.add(tmpl.getExpression());
            tempList.add(tmpl.getRemark());
            exportDataList.add(tempList);
        }
        return exportDataList;
    }

    @Override
    public void cancelImport(String seq) {
        this.remove(new LambdaQueryWrapper<VisualExpressionTmpl>().eq(VisualExpressionTmpl::getSeq, seq));
    }

}
