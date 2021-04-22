package com.nis.modules.panel.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.ExcelUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.panel.dao.VisualPanelDao;
import com.nis.modules.panel.entity.ChartElement;
import com.nis.modules.panel.entity.Node;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.panel.entity.VisualChartElement;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.NodeService;
import com.nis.modules.panel.service.VisualChartElementService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;


@Service("visualPanelService")
public class VisualPanelServiceImpl extends ServiceImpl<VisualPanelDao, VisualPanel> implements VisualPanelService {

	private Log log = Log.get();
	
    @Autowired
    private VisualChartService visualChartService;
    
    @Autowired
    private SysConfigService sysConfigService;
  
    @Autowired
    private VisualChartElementService chartElementService;
    
    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;
    
	@Autowired
    private TransactionDefinition transactionDefinition;
    
    @Override
    public VisualPanel queryInfo(Integer id) {
    	return this.baseMapper.queryInfo(id);
    }
    
    @Override
    public List<VisualPanel> queryList(Map<String, Object> params) {
    	String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("ids",listIds);
        }
        // 1. 查询时 type 为空则默认 dashboard 类型
        String type = (String) params.get("type");
        if (StringUtils.isEmpty(type)) {
            type = Constant.PanelType.DASHBOARD.getValue();
            params.put("type", type);
        }
        // 2. dashboard 类型 需要进行排序返回
        List<VisualPanel> panelList = new ArrayList<>();
        if (StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(),type)) {
            panelList = this.baseMapper.queryList(params);
            if (CollectionUtils.isEmpty(panelList) || panelList.size() == 1) {
                return panelList;
            }
            panelList = this.getVisualPanelTreeList(panelList, 0);
        } else {
            // 3. 其他类型不进行排序
            panelList = this.baseMapper.queryList(params);
        }
        
        // 查询当前panel下的chart数量并且进行赋值
        Map<Integer, Integer> chartNums = visualChartService.queryChartNum();
        for(VisualPanel visualPanel : panelList) {
        	Integer chartNum = ObjectUtil.isEmpty(chartNums.get(visualPanel.getId()))?0:chartNums.get(visualPanel.getId());
        	visualPanel.setChartNum(chartNum);
        }
        return panelList;
    }

    /**
     * 递归获取树结构
     *
     * @param panelList
     * @param parentId
     * @return
     */
    private List<VisualPanel> getVisualPanelTreeList(List<VisualPanel> panelList, Integer parentId) {
        List<VisualPanel> resultList = new ArrayList<>();
        for (VisualPanel panel : panelList) {
            Integer id = panel.getId();
            Integer pid = panel.getPid();
            if (parentId.equals(pid)) {
                panel.setChildren(this.getVisualPanelTreeList(panelList, id));
                resultList.add(panel);
            }
        }
        return resultList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPanelsAndChartInfoByPanelIds(Set<Integer> panelIds) {
        if (CollectionUtils.isNotEmpty(panelIds)) {
            // 删除 panel
            this.removeByIds(panelIds);

            // 删除 chart info
            Integer[] removePanelIds = panelIds.stream().toArray(n -> new Integer[n]);
//            visualChartService.delChartsByPanelId(removePanelIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPanels(Integer... ids) {
        if (ToolUtil.isEmpty(ids)) {
            return;
        }

        List<VisualPanel> panels = this.list();

        // 1. 校验内置panel不能被删除
        this.validateBuildInPanels(panels, ids);

        // 获取这些节点的所有子节点
        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        List<VisualPanel> panelList = this.list();
        Set<Integer> allPanelIds = new HashSet<>();
        for (Integer id : idList) {
            allPanelIds.add(id);
            allPanelIds.addAll(this.getChildrenIds(panelList, id));
        }

        // 2. 删除 panel ,如果包含子节点，也一并删除
        this.removeByIds(allPanelIds);

        // 3. 删除关联的 chart chartElement
        Integer[] removePanelIds = allPanelIds.stream().toArray(n -> new Integer[n]);
        visualChartService.delVisualChartsByPanelId(removePanelIds);
    }

    /**
     * 查询所有子节点 ID
     * @param panelList
     * @param parentId
     * @return
     */
    private List<Integer> getChildrenIds(List<VisualPanel> panelList, Integer parentId) {
        List<Integer> resultList = new ArrayList<>();
        for (VisualPanel panel : panelList) {
            Integer id = panel.getId();
            Integer pid = panel.getPid();
            if (parentId.equals(pid)) {
                resultList.add(id);
                resultList.addAll(this.getChildrenIds(panelList, id));
            }
        }
        return resultList;
    }

    /**
     * 处理删除后的位置
     * @param sequentialPanelList      删除之前有序的链表
     * @param deleteIds                删除的 IDS
     */
    @Override
    public void handlePositionAfterDelete(List<VisualPanel> sequentialPanelList, List<Integer> deleteIds) {
        // 1. 全删除了
        if (sequentialPanelList.size() == deleteIds.size()) {
            return;
        }

        // 2. 删除之后的剩余元素  有序
        List<VisualPanel> deletedPanelList = new ArrayList<>();
        for (VisualPanel obj : sequentialPanelList) {
            if (!deleteIds.contains(obj.getId())) {
                deletedPanelList.add(obj);
            }
        }

        // 开头 panel
        VisualPanel firstPanel = deletedPanelList.get(0);

        // 剩余一个
        if (deletedPanelList.size() == 1) {
            this.updateById(firstPanel);
        } else {
            // 剩余数量大于 1
            List<VisualPanel> updatePanels = new ArrayList<>();

            // 批量更新
            VisualPanel tempPanel = null;
            int size = deletedPanelList.size();
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    updatePanels.add(firstPanel);
                } else {
                    tempPanel = deletedPanelList.get(i);
                    updatePanels.add(tempPanel);
                }
            }
            this.saveOrUpdateBatch(updatePanels);
        }
    }

    /**
     * 校验内置 panel 不可删除
     * @param panels
     * @param removeIds
     */
    private void validateBuildInPanels(List<VisualPanel> panels, Integer... removeIds) {
        // 内置 panel 不能删除
        List<Integer> buildInPanels = panels.stream().filter(panel -> panel.getBuildIn() == 1).map(VisualPanel::getId).collect(Collectors.toList());
        List<Integer> removeIdList = Arrays.stream(removeIds).collect(Collectors.toList());
        removeIdList.retainAll(buildInPanels);
        if (CollectionUtils.isNotEmpty(removeIdList)) {
            List<String> panelNames = this.listByIds(removeIdList).stream().map(VisualPanel::getName).collect(Collectors.toList());
            throw new NZException("These panel is built-in and cannot be deleted, panels info: " + panelNames.toString(), RCode.PANEL_BUILDIN_CAN_NOT_REMOVE.getCode());
        }
    }

    /**
     * 校验 panel 相关参数是否正确
     *
     * @param panel
     */
    public void validatePanel(VisualPanel panel) {
        String name = panel.getName();
        ValidateUtils.is(name).notNull(RCode.PANEL_NAME_ISNULL);

        if (name.contains("/")) {
            throw new NZException(RCode.PANEL_NAME_FORMAT_ERROR);
        }

        Integer pid = panel.getPid();
        pid = (pid == null) ? 0 : pid;
        // 校验name是否重复
        List<VisualPanel> list = this.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getPid, pid).eq(VisualPanel::getName, panel.getName()));
        boolean flag = false;
        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(panel.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.PANEL_NAME_DUPLICATE);
        }

        // link 不为空时
        if (ToolUtil.isNotEmpty(panel.getLink())) {
            // 校验 type
            String type = panel.getType();
            ValidateUtils.is(type).notNull(RCode.PANEL_TYPE_ISNULL);
            List<Constant.PanelType> panelTypes = Arrays.asList(Constant.PanelType.values());
            List<String> types = panelTypes.stream().map(Constant.PanelType::getValue).collect(Collectors.toList());
            if (!types.contains(type)) {
                throw new NZException(RCode.PANEL_TYPE_INVALIDE);
            }
        }

        // pid 是否存在
        if (panel.getPid() != null && panel.getPid() != 0) {
        	VisualPanel entity = this.getById(panel.getPid());
            if (entity == null) {
                throw new NZException(RCode.PANEL_NOT_EXIST);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveVisualPanel(VisualPanel panel) {
        this.validatePanel(panel);

        Long userId = ShiroUtils.getUserId();
        panel.setCreateBy(userId.intValue());

        if (ToolUtil.isEmpty(panel.getLink())) {
            panel.setType(Constant.PanelType.DASHBOARD.getValue());
        }

        Integer pid = panel.getPid();
        pid = (pid == null) ? 0 : pid;
        // 查询该父级节点下是否存在 Panel 节点
        List<VisualPanel> list = this.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, panel.getType()).eq(VisualPanel::getPid, pid));
        if (CollectionUtils.isEmpty(list)) {
            // 父级节点下暂无其他节点，直接保存
            this.save(panel);
        } else {
            // 找到当前末尾的 panel ， 加到其后
        	VisualPanel endPanel = list.stream().max(Comparator.comparingInt(VisualPanel::getWeight)).get();
            Integer weight = endPanel.getWeight();
            panel.setWeight(++weight);
            this.save(panel);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisualPanel(VisualPanel panel) {
        this.validatePanel(panel);

        VisualPanel oldPanel = this.getById(panel.getId());

        if (oldPanel == null) {
            throw new NZException(RCode.PANEL_NOT_EXIST);
        }

        // 改变 pid 父级节点时，将该 panel 加入到该 pid 下末尾位置
        if (!oldPanel.getPid().equals(panel.getPid())) {
        	VisualPanel endPanel = this.getOne(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getPid, panel.getPid()).orderByDesc(VisualPanel::getWeight).last("limit 0,1"));
            // 该 pid 下存在节点
            if (endPanel != null) {
                Integer weight = endPanel.getWeight();
                panel.setWeight(++weight);
            } else {
                panel.setWeight(0);
            }
        }

        // 修改
        this.updateById(panel);
    }

    /**
     * 调整panel位置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void modify(VisualPanel panel) {
        String type = panel.getType();
        List<Constant.PanelType> panelTypes = Arrays.asList(Constant.PanelType.values());
        List<String> types = panelTypes.stream().map(Constant.PanelType::getValue).collect(Collectors.toList());
        if (!types.contains(type)) {
            throw new NZException(RCode.PANEL_TYPE_INVALIDE);
        }

        List<VisualPanel> panelList = this.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, type));

        // 当前类型没有panel
        if (CollectionUtils.isEmpty(panelList)) {
            throw new NZException(RCode.PANEL_NOTFOUND_BYTYPE);
        }

        Map<Integer, Node> idAndPanelMap = new HashMap<>();
        for (VisualPanel panel1 : panelList) {
//            idAndPanelMap.put(panel1.getId(), panel1);
        }

        // 当前类型下不存在此panel
        Node oldPanel = idAndPanelMap.get(panel.getId());
        if (oldPanel == null) {
            throw new NZException(RCode.PANEL_NOT_EXIST);
        }

        // 就只有一个图表 不让其调整位置
        if (panelList.size() == 1) {
            oldPanel.setPrev(Constant.START_PREV_VALUE);
            oldPanel.setNext(Constant.END_NEXT_VALUE);
//            this.updateById((VisualPanel) oldPanel);
            return;
        }

		/*        // 校验 prev next 参数是否合法
		if (nodeService.validatePrevAndNext(idAndPanelMap, panel)) {
		    return;
		}*/

		/*        List<Node> updatePanels = nodeService.modify(idAndPanelMap, panel, oldPanel);
		
		// 更新库中数据
		if (CollectionUtils.isNotEmpty(updatePanels)) {
		    Set<VisualPanel> panels = new HashSet<>();
		    for (Node updatePanel : updatePanels) {
		        panels.add((VisualPanel) updatePanel);
		    }
		    this.saveOrUpdateBatch(panels);
		}*/
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(List<VisualPanel> panelList) {
        if (CollectionUtils.isEmpty(panelList)) throw new NZException(RCode.PANEL_ID_ISNULL);
        for (VisualPanel panel : panelList) {
            if (panel.getId() == null) throw new NZException(RCode.PANEL_ID_ISNULL);
            if (panel.getPid() == null) throw new NZException(RCode.PANEL_PID_ISNULL);
            if (panel.getWeight() == null) throw new NZException(RCode.PANEL_WEIGHT_ISNULL);
        }

        this.getBaseMapper().updateWeightBatch(panelList);
    }

    @Override
    public List<VisualPanel> getSequentialPanelList(List<VisualPanel> panels) {
        if (CollectionUtils.isEmpty(panels)) {
            return new ArrayList<>();
        }
        List<VisualPanel> resultList = new ArrayList<>();

        // 按照prev升序排序
//        panels = panels.stream().sorted(Comparator.comparing(VisualPanel::getPrev)).collect(Collectors.toList());

        Map<Integer, VisualPanel> idAndPanelMap = new HashMap<>();
        for (VisualPanel panel : panels) {
            idAndPanelMap.put(panel.getId(), panel);
        }

        VisualPanel firstPanel = panels.get(0);

        VisualPanel tempPanel = firstPanel;
        resultList.add(firstPanel);
        VisualPanel nextPanel = null;

        while (true) {
//            nextPanel = idAndPanelMap.get(tempPanel.getNext());
            if (nextPanel != null) {
                resultList.add(nextPanel);
                tempPanel = nextPanel;
            } else {
                break;
            }
        }
        return resultList;
    }
    
    @Override
	public void getExportTemplateByLanguage(HttpServletResponse response, String language, String type) {
		// 按类型获取表头模板信息
        String configKey = this.getChartTemplateConfigKeyByType(type);
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, configKey));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        Map<String, String> exportHeaderMap;
        if(map.get(language) != null){
            exportHeaderMap = (Map) map.get(language);
        }else{
            throw new NZException(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ERROR);
        }
        SXSSFWorkbook workbook = null;
        try {
            workbook = ExcelUtils.createExcel07(exportHeaderMap, "Chart", true);
            String fileName = "PanelChartExportTemplate.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("图表模板生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("获取图表导入模板失败，表头语言是：" + language + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
	}
    
    @Override
    public Map importChartFromExcelFile(MultipartFile excelFile, String type, Integer linkId, String language) {
        if (!ExcelUtils.validateFileType(excelFile.getOriginalFilename())) {
            throw new NZException(RCode.PANEL_IMPORTFILE_TYPE);
        }

        Workbook work = null;
        try {
            work = ExcelUtils.getWork(excelFile.getOriginalFilename(), excelFile.getInputStream());
        } catch (IOException e) {
            log.error("解析导入图表文件格式有误"+e);
            throw new NZException(RCode.PANEL_IMPORTFILE_FAILED);
        } finally {
            if (work != null) {
                IOUtils.closeQuietly(work);
            }
        }

        Sheet sheet = work.getSheetAt(0);
        // 表头校验
        String configKey = this.getChartTemplateConfigKeyByType(type);
        SysConfigEntity config = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, configKey));
        HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
        boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
        if(!headerFlag){
            throw new NZException(RCode.PANEL_EXPORT_HEADER_TEMPLATE_ERROR);
        }

        Map<String, String> exportHeader = (Map) map.get(language);
        List<String> headerInfos = new ArrayList(exportHeader.keySet());

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();

        // 保存并返回成功条数
        type = StringUtils.isEmpty(type) ? Constant.PanelType.DASHBOARD.getValue() : type;

        SysConfigEntity i18nConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.I18N_MAPPING).eq(SysConfigEntity::getStatus, 1));
        Map i18nConfigMap = JSONObject.parseObject(i18nConfig.getParamValue(), Map.class);
        Map chartTypeMap = (Map) i18nConfigMap.get(Constant.DICT_CHART_TYPE);
        Map chartTypeMapByLanguage = (Map) chartTypeMap.get(language);
        if (ToolUtil.isEmpty(chartTypeMapByLanguage)) {
            throw new NZException(RCode.PANEL_EXPORT_HEADER_LANGUAGE_ERROR);
        }

        int successNum = 0;
        if(StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(),type)) {
            successNum = this.saveDashboardChartDataFromSheet(sheet, failDetails, seqUUID, chartTypeMapByLanguage, headerInfos);
        } else {
            successNum = this.saveAssetChartDataFromSheet(sheet, failDetails, seqUUID, type, linkId, chartTypeMapByLanguage,headerInfos);
        }
        int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));

        return resultMap;
    }
	
	@Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelImport(String seq) {
        List<VisualPanel> panelList = this.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, Constant.PanelType.DASHBOARD.getValue()));
        List<Integer> delPanelIds = panelList.stream().filter(panel -> seq.equals(panel.getSeq())).map(VisualPanel::getId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(delPanelIds)) {
            // 删除 panel
        	this.remove(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getSeq, seq));
        }

        List<VisualChart> delCharts = visualChartService.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getSeq, seq));
        if (CollectionUtils.isNotEmpty(delCharts)) {
            // 删除 charts
        	visualChartService.remove(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getSeq, seq));
        }
        chartElementService.remove(new QueryWrapper<VisualChartElement>().lambda().eq(VisualChartElement::getSeq, seq));
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) {
        // 导出查询与列表查询一致
        List<VisualChart> charts = this.exportDataByParams(params);
        String type = Constant.PanelType.DASHBOARD.getValue();
        if (CollectionUtils.isNotEmpty(charts)) {
        	VisualChart chart = charts.stream().findFirst().get();
            VisualPanel panel = this.getById(chart.getPanelId());
            if(!StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(),panel.getType())) {
                type = Constant.PanelType.ASSET.getValue();
            }
        }

        // 获取导出配置 key 名称
        String chartTemplateConfigKey = this.getChartTemplateConfigKeyByType(type);
        SysConfigEntity configEntity = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, chartTemplateConfigKey));
        Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
        // 添加标题行 默认语言 en
        String language = (String) params.get("language");
        language = ToolUtil.isEmpty(language) ? "en" : language;

        SXSSFWorkbook workbook = null;
        try {
            String sheetName = "Chart";
            workbook = ExcelUtils.createExcel07((Map) map.get(language), sheetName, false);
            SXSSFSheet sheet = workbook.getSheet(sheetName);

            SysConfigEntity i18nConfig = sysConfigService.getOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.I18N_MAPPING).eq(SysConfigEntity::getStatus, 1));
            Map i18nConfigMap = JSONObject.parseObject(i18nConfig.getParamValue(), Map.class);
            Map chartTypeMap = (Map) i18nConfigMap.get(Constant.DICT_CHART_TYPE);
            Map chartTypeMapByLanguage = (Map)  chartTypeMap.get(language);

            // 填充数据
            if(!StrUtil.equals(Constant.PanelType.DASHBOARD.getValue(),type)){
                this.fillAssetModelChartData(charts, 1, sheet, workbook, chartTypeMapByLanguage);
            }else{
                this.fillDashboardChartData(charts, 1, sheet, workbook, chartTypeMapByLanguage);
            }

            String fileName = "PanelChartExport.xlsx";
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            log.error("图表导出文件生成失败，错误信息是：" + e);
        } catch (RuntimeException e) {
            log.error("图表导出失败，参数是：" + params.toString() + " ，错误信息是：" + e);
        } finally {
            if (workbook != null) {
                IOUtils.closeQuietly(workbook);
            }
        }
    }
    

	private String getChartTemplateConfigKeyByType(String type) {
        // 默认 dashboard 类型
        type = StringUtils.isEmpty(type) ? Constant.PanelType.DASHBOARD.getValue() : type;
        String chartTemplateTyep;
        switch (type) {
            case "dashboard": {
                chartTemplateTyep = Constant.SYSCONFIG_KEY_CHART_EXPORT_HEADER;
                break;
            }
            case "model": {
                chartTemplateTyep = Constant.SYSCONFIG_KEY_MODEL_CHART_EXPORT_HEADER;
                break;
            }
            case "asset": {
                chartTemplateTyep = Constant.SYSCONFIG_KEY_ASSET_CHART_EXPORT_HEADER;
                break;
            }
            default:
                throw new NZException(RCode.PANEL_CHART_TEMPLATE_TYPE_INCORRECT);
        }
        return chartTemplateTyep;
    }

	
    
    private void fillAssetModelChartData(List<VisualChart> charts, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook, Map chartTypeMapByLanguage) {
        Row row;
        String type;
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (VisualChart chart : charts) {
            if (chart == null || Constant.CHART_ASSETINFO_TYPE.equalsIgnoreCase(chart.getType())) continue;
            row = sheet.createRow(rowNum++);
            type = chart.getType();
            row.createCell(0).setCellValue(chart.getName());
            row.createCell(1).setCellValue(this.getI18nChartTypeValue(chartTypeMapByLanguage, type));
            row.createCell(2).setCellValue(chart.getSpan());
            row.createCell(3).setCellValue(chart.getHeight());
            if(StrUtil.equals(Constant.CHART_TEXT_TYPE,type)) {
                row.createCell(4).setCellValue("");
            } else {
                row.createCell(4).setCellValue(this.getUnitDescrByVal(chart.getUnit()));
            }
            row.createCell(5).setCellValue(this.handleChartParam(chart.getParam()));
            row.createCell(6).setCellValue(this.handleChartElementForExport(chart.getElements()));

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 6);
        }
    }
    
    private String handleChartElementForExport(List<VisualChartElement> chartElements) {
        if (CollectionUtils.isEmpty(chartElements)) {
            return "";
        }
        List<Map> handleMaps = new ArrayList<>();
        Map tempMap;
        for (VisualChartElement element : chartElements) {
            tempMap = new LinkedHashMap(4);
            tempMap.put("expression", element.getExpression());
            if(ToolUtil.isNotEmpty(element.getLegend())){
                tempMap.put("legend", element.getLegend());
            }
            handleMaps.add(tempMap);
        }
        return JSONArray.toJSONString(handleMaps);
    }
    
    private String getI18nChartTypeValue(Map chartTypeMapByLanguage, String type) {
        Object value = chartTypeMapByLanguage.get(type);
        return value == null ? "" : value.toString();
    }
    
    private String getUnitDescrByVal(Integer unitVal) {
        String unitDescr = null;
        for (Map.Entry<String, Integer> entry : Constant.ALERT_RULE_UNIT_MAP.entrySet()) {
            if (entry.getValue().equals(unitVal)) {
                unitDescr = entry.getKey();
                break;
            }
        }
        return unitDescr;
    }
    
    private String handleChartParam(Object param) {
        if (ToolUtil.isEmpty(param)) {
            return "";
        }
        Map<String, Object> map = JSONObject.parseObject(param.toString(), Map.class);
        map.entrySet().removeIf(entry -> ToolUtil.isEmpty(entry.getValue()));
        return ToolUtil.isEmpty(map) ? "" : JSONObject.toJSONString(map);
    }
    
    public void getPanelNamePathAndIdMapFromTree(VisualPanel panel, String path, Map<String, Integer> nodeNamePathAndIdMap) {
        if (CollectionUtils.isEmpty(panel.getChildren())) {
            return;
        }
        for (VisualPanel cPanel : panel.getChildren()) {
            String cPath = "";
            cPath += path + "/" + cPanel.getName();
            nodeNamePathAndIdMap.put(cPath, cPanel.getId());
            this.getPanelNamePathAndIdMapFromTree(cPanel, cPath, nodeNamePathAndIdMap);
        }
    }
    
    private int saveDashboardChartDataFromSheet(Sheet sheet, List<Map> failDetails, String seqUUID, Map<String, String> chartTypeMapByLanguage, List<String> headerInfos) {
        List<VisualPanel> panelTreeList = this.queryList(new HashMap());
        Map<String, Integer> nodeNamePathAndIdMap = new HashMap<>();
        for (VisualPanel panel : panelTreeList) {
            nodeNamePathAndIdMap.put(panel.getName(), panel.getId());
            this.getPanelNamePathAndIdMapFromTree(panel, panel.getName(), nodeNamePathAndIdMap);
        }

        List<VisualPanel> dashboardTypePanelList = this.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.DASHBOARD.getValue()));
        Integer panelMaxWeight;
        if (CollectionUtils.isEmpty(dashboardTypePanelList)) {
            panelMaxWeight = 0;
        } else {
        	VisualPanel endPanel = dashboardTypePanelList.stream().max(Comparator.comparingInt(VisualPanel::getWeight)).get();
            panelMaxWeight = endPanel.getWeight();
        }

        Row row;
        int successNum = 0;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            boolean typeFlag = true;
            String panelName = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(panelName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.PANEL_NAME_ISNULL.getMsg());
            }
            String title = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(title)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.CHART_TITLE_ISNULL.getMsg());
            }
            String type = ExcelUtils.getCellValue(row.getCell(2));
            if (StringUtils.isEmpty(type)) {
                saveFlag = typeFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_TYPE_ISNULL.getMsg());
            } else {
                type = this.validateChartTypeForImport(type, chartTypeMapByLanguage);
                if (StringUtils.isEmpty(type)) {
                    saveFlag = typeFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_TYPE_INVALIDE.getMsg());
                }
            }
            String span = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isEmpty(span)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_SPAN_ISNULL.getMsg());
            }else{
                Integer width = null;
                try {
                    width = Double.valueOf(span).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_WIDTH_FORMAT.getMsg());
                }
                if(width != null && (width < 1 || width > 12)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_WIDTH_INVALIDE.getMsg());
                }
            }
            String height = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isEmpty(height)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_HEIGHT_ISNULL.getMsg());
            }else{
                try {
                    Double.valueOf(height).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_HEIGHT_FORMAT.getMsg());
                }
            }
            String unit = ExcelUtils.getCellValue(row.getCell(5));
            if (StringUtils.isNotEmpty(unit)) {
                if (ToolUtil.isEmpty(Constant.ALERT_RULE_UNIT_MAP.get(unit))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.CHART_UNIT_NOTFOUND.getMsg());
                }
            }
            String param = ExcelUtils.getCellValue(row.getCell(6));
            if(typeFlag){
                String msg = this.validateChartParamForImport(type, param, "dashboard");
                if(StringUtils.isNotEmpty(msg)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), msg);
                }
            }
            String elements = ExcelUtils.getCellValue(row.getCell(7));
            if(typeFlag){
                String msg = this.validateChartElementForImport(type, elements);
                if(StringUtils.isNotEmpty(msg)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(7), msg);
                }
            }
            if(saveFlag){
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                try {
                    Integer panelId = this.handlePanelNameForCellVal(nodeNamePathAndIdMap, panelName, seqUUID, ++panelMaxWeight);
                    VisualChart chart = new VisualChart();
                    chart.setSeq(seqUUID);
                    chart.setPanelId(panelId);
                    chart.setName(title);
                    chart.setType(type);
                    chart.setSpan(Double.valueOf(span).intValue());
                    chart.setHeight(Double.valueOf(height).intValue());
                    // unit 为空 设置默认值为 short
                    unit = StringUtils.isEmpty(unit) ? "short" : unit;
                    chart.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(unit));
                    chart.setParam(param.toString());
                    chart.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
                    // 查询当前 panel 下的 charts
                    List<VisualChart> chartList = visualChartService.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPanelId, chart.getPanelId()));
                    if (CollectionUtils.isEmpty(chartList)) {
                    	visualChartService.save(chart);
                    } else {
                    	VisualChart endChart = visualChartService.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, chart.getPanelId()).orderByDesc(VisualChart::getWeight).last("limit 0,1"));
                        Integer weight = endChart.getWeight();
                        chart.setWeight(++weight);
                        visualChartService.save(chart);
                    }
                    List<VisualChartElement> chartElements = JSONArray.parseArray(elements, VisualChartElement.class);
                    if (CollectionUtils.isNotEmpty(chartElements)) {
                        chartElements.forEach(chartElement -> {
                            chartElement.setChartId(chart.getId());
                            chartElement.setSeq(seqUUID);
                            chartElement.setType(Constant.CHART_ELEMENT_EXPERT_TYPE);
                        });
                        chartElementService.saveBatch(chartElements);
                    }
                    dataSourceTransactionManager.commit(transactionStatus);
                    // panelNameAndIdMap.put(panelName, panelId);
                    successNum++;
                } catch (RuntimeException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage() + "");
                    log.error(String.format("保存 panel chart chartelement失败,回滚事务,错误信息:%s", e));
                }
            }
        }
        return successNum;
    }
    
    private String validateChartElementForImport(String type, String elements) {
        // elemrnt 只有 alert  和 url , text 可以没有，其他必须有
        if(!StrUtil.equals(Constant.CHART_ALERTLIST_TYPE,type) && !StrUtil.equals(Constant.CHART_URL_TYPE,type) && !StrUtil.equals(Constant.CHART_TEXT_TYPE,type)){
            if(StringUtils.isEmpty(elements)){
                return RCode.CHART_ELEMENT_ISNULL.getMsg();
            }
            List<ChartElement> elementsList = null;
            try {
                elementsList = JSONArray.parseArray(elements, ChartElement.class);
            } catch (JSONException e) {
                return RCode.CHART_ELEMENTS_FORMAT.getMsg();
            }

            if(CollectionUtils.isEmpty(elementsList)){
                return RCode.CHART_ELEMENT_ISNULL.getMsg();
            }

            for (ChartElement element : elementsList) {
                if(ToolUtil.isEmpty(element.getExpression())){
                    return RCode.CHARTELEMENT_EXPRESSION_ISNULL.getMsg();
                }
            }
        }
        return "";
    }
    
    /**
     * @param panelName 格式： 使用 / 作为字符分隔，表示 父子节点关系
     * @param seqUUID   导入序列号
     */
    private Integer handlePanelNameForCellVal(Map<String, Integer> nodeNamePathAndIdMap, String panelName, String seqUUID, Integer panelWeight) {
        String savePanelNames = panelName;
        Integer panelId;
        while (true) {
            panelId = nodeNamePathAndIdMap.get(panelName);
            if (panelId != null) {
                break;
            }
            // 仍然包含 ‘/’ 证明还是有父子关系
            if (panelName.lastIndexOf("/") != -1) {
                panelName = panelName.substring(0, panelName.lastIndexOf("/"));
            } else {
                // 最后的 panel 也不存在，赋值 为空 ，下面会进行过滤添加
                panelName = "";
                break;
            }
        }
        savePanelNames = savePanelNames.replace(panelName, "");

        // 如果存在需要保存的 panel
        if (StringUtils.isNotEmpty(savePanelNames)) {
            if (savePanelNames.startsWith("/")) {
                savePanelNames = savePanelNames.substring(savePanelNames.indexOf("/") + 1);
            }

            VisualPanel panel = new VisualPanel();
            panel.setType(Constant.PanelType.DASHBOARD.getValue());
            panel.setCreateBy(ShiroUtils.getUserId().intValue());
            panel.setSeq(seqUUID);
            panel.setWeight(panelWeight);

            List<String> saveList = Arrays.asList(savePanelNames.split("/"));
            int size = saveList.size();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < size; i++) {
                String name = saveList.get(i);
                panel.setName(name);
                if (panel.getId() == null) {
                    panel.setPid(panelId);
                } else {
                    panel.setPid(panel.getId());
                }
                this.save(panel);
                sb.append(name + "/");
                String temp = panelName + "/" + sb.substring(0, sb.lastIndexOf("/"));
                // 因为用户输入根节点没有 ‘/’，所以不保存根节点 '/' 路径作为对比
                if (temp.startsWith("/")) {
                    temp = temp.substring(temp.indexOf("/") + 1);
                }
                nodeNamePathAndIdMap.put(temp, panel.getId());
            }
            return panel.getId();
        }
        return panelId;
    }

    
    private String validateChartParamForImport(String type, String param, String panelType) {
        switch (type){
            // url
            case Constant.CHART_URL_TYPE: {
                if (StringUtils.isEmpty(param)) {
                    return RCode.CHART_URLPARAM_ISNULL.getMsg();
                }
                Map map = null;
                try {
                    map = JSONObject.parseObject(param, Map.class);
                } catch (JSONException e) {
                    return RCode.CHART_PARAM_FORMAT.getMsg();
                }
                Object url = map.get(Constant.CHART_URL_TYPE);
                if (ToolUtil.isEmpty(url)) {
                    return RCode.CHART_PARAMURL_ISNULL.getMsg();
                }

                if(StrUtil.equals(Constant.PanelType.MODEL.getValue(),panelType)) {
                    // 替换全部变量 替换为 a（任意字符） 不影响原来的数据格式 然后进行校验
                    String urlStr = url.toString();
                    urlStr = urlStr.replaceAll("\\{\\{asset\\..*?\\}\\}", "a");

                    boolean b = CommonUtils.checkUrlInModel(urlStr);
                    boolean b1 = CommonUtils.checkUrl(urlStr);

                    if (!b && !b1) {
                        return RCode.CHART_PARAMURL_FORMAT.getMsg();
                    }
                } else {
                    if (!CommonUtils.checkUrl(url.toString())) {
                        return RCode.CHART_PARAMURL_FORMAT.getMsg();
                    }
                }
                break;
            }
            // singleStat
            case Constant.CHART_SINGLESTAT_TYPE: {
                if (StringUtils.isEmpty(param)) {
                    return RCode.CHART_SINGLESTATPARAM_ISNULL.getMsg();
                }
                Map map = null;
                try {
                    map = JSONObject.parseObject(param, Map.class);
                } catch (JSONException e) {
                    return RCode.CHART_PARAM_FORMAT.getMsg();
                }
                Object statistics = map.get(Constant.CHART_SINGLESTAT_PARAMkEY);
                if (ToolUtil.isEmpty(statistics)) {
                    return RCode.CHART_PARAMSINGLESTAT_ISNULL.getMsg();
                }
                if (!Constant.CHART_SINGLESTAT_STATISTICS.contains(statistics.toString())) {
                    return RCode.CHART_PARAMSINGLESTAT_INVALIDE.getMsg();
                }
                break;
            }
            case Constant.CHART_TEXT_TYPE: {
                if (StringUtils.isEmpty(param)) {
                    return RCode.CHART_TEXTPARAM_ISNULL.getMsg();
                }
                Map map = null;
                try {
                    map = JSONObject.parseObject(param, Map.class);
                } catch (JSONException e) {
                    return RCode.CHART_PARAM_FORMAT.getMsg();
                }
                Object text = map.get(Constant.CHART_TEXT_TYPE);
                if (ToolUtil.isEmpty(text)) {
                    return RCode.CHART_PARAMTEXT_ISNULL.getMsg();
                }
                break;
            }
            default:{
                if(StringUtils.isNotEmpty(param)){
                    try {
                        JSONObject.parseObject(param, Map.class);
                    } catch (JSONException e) {
                        return RCode.CHART_PARAM_FORMAT.getMsg();
                    }
                }
            }
        }
        return "";
     }
    
    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
    }

    private String validateChartTypeForImport(String type, Map<String,String> chartTypeMapByLanguage) {
        for (Map.Entry<String, String> entry : chartTypeMapByLanguage.entrySet()) {
            if(StrUtil.equals(type,entry.getValue())){
                return entry.getKey();
            }
        }
        return null;
    }
    
    private int saveAssetChartDataFromSheet(Sheet sheet, List<Map> failDetails, String seqUUID, String panelType, Integer linkId, Map chartTypeMapByLanguage, List<String> headerInfos) {
        if (linkId == null) throw new NZException(RCode.PANEL_CHART_IMPORT_LINKID_ISNULL);

        VisualPanel panel = this.getOne(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, panelType).eq(VisualPanel::getLink, linkId));
        if (panel == null) throw new NZException(RCode.PANEL_NOT_EXIST);

        Row row;
        int successNum = 0;
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row == null) continue;
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            boolean typeFlag = true;
            String title = ExcelUtils.getCellValue(row.getCell(0));
            if (StringUtils.isEmpty(title)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.CHART_TITLE_ISNULL.getMsg());
            }
            String type = ExcelUtils.getCellValue(row.getCell(1));
            if (StringUtils.isEmpty(type)) {
                saveFlag = typeFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.CHART_TYPE_ISNULL.getMsg());
            } else {
                type = this.validateChartTypeForImport(type, chartTypeMapByLanguage);
                if (StringUtils.isEmpty(type)) {
                    saveFlag = typeFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.CHART_TYPE_INVALIDE.getMsg());
                }
            }
            String span = ExcelUtils.getCellValue(row.getCell(2));
            if (StringUtils.isEmpty(span)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_SPAN_ISNULL.getMsg());
            } else {
                Integer width = null;
                try {
                    width = Double.valueOf(span).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_WIDTH_FORMAT.getMsg());
                }
                if (width != null && (width < 1 || width > 12)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.CHART_WIDTH_INVALIDE.getMsg());
                }
            }
            String height = ExcelUtils.getCellValue(row.getCell(3));
            if (StringUtils.isEmpty(height)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_HEIGHT_ISNULL.getMsg());
            } else {
                try {
                    Double.valueOf(height).intValue();
                } catch (NumberFormatException e) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.CHART_HEIGHT_FORMAT.getMsg());
                }
            }
            String unit = ExcelUtils.getCellValue(row.getCell(4));
            if (StringUtils.isNotEmpty(unit)) {
                if (ToolUtil.isEmpty(Constant.ALERT_RULE_UNIT_MAP.get(unit))) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.CHART_UNIT_NOTFOUND.getMsg());
                }
            }
            String param = ExcelUtils.getCellValue(row.getCell(5));
            if (typeFlag) {
                String msg = this.validateChartParamForImport(type, param, type);
                if (StringUtils.isNotEmpty(msg)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), msg);
                }
            }
            String elements = ExcelUtils.getCellValue(row.getCell(6));
            if (typeFlag) {
                String msg = this.validateChartElementForImport(type, elements);
                if (StringUtils.isNotEmpty(msg)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), msg);
                }
            }
            if (saveFlag) {
                TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                try {
                	VisualChart chart = new VisualChart();
                    chart.setSeq(seqUUID);
                    chart.setPanelId(panel.getId());
                    chart.setName(title);
                    chart.setType(type);
                    chart.setSpan(Double.valueOf(span).intValue());
                    chart.setHeight(Double.valueOf(height).intValue());
                    // unit 为空 设置默认值为 short
                    unit = StringUtils.isEmpty(unit) ? "short" : unit;
                    chart.setUnit(Constant.ALERT_RULE_UNIT_MAP.get(unit));
                    chart.setParam(param.toString());
                    chart.setUpdateAt(DateUtil.getUTCTimeByConfigTimeZone());
                    // 查询当前 panel 下的 charts
                    List<VisualChart> chartList = visualChartService.list(new QueryWrapper<VisualChart>().lambda().eq(VisualChart::getPanelId, chart.getPanelId()));
                    if (CollectionUtils.isEmpty(chartList)) {
                    	visualChartService.save(chart);
                    } else {
                    	VisualChart endChart = visualChartService.getOne(new LambdaQueryWrapper<VisualChart>().eq(VisualChart::getPanelId, chart.getPanelId()).orderByDesc(VisualChart::getWeight).last("limit 0,1"));
                        Integer weight = endChart.getWeight();
                        chart.setWeight(++weight);
                        visualChartService.save(chart);
                    }
                    List<VisualChartElement> chartElements = JSONArray.parseArray(elements, VisualChartElement.class);
                    if (CollectionUtils.isNotEmpty(chartElements)) {
                        chartElements.forEach(chartElement -> {
                            chartElement.setChartId(chart.getId());
                            chartElement.setSeq(seqUUID);
                            chartElement.setType(Constant.CHART_ELEMENT_EXPERT_TYPE);
                        });
                        chartElementService.saveBatch(chartElements);
                    }
                    dataSourceTransactionManager.commit(transactionStatus);
                    successNum++;
                } catch (RuntimeException e) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage() + "");
                    log.error(String.format("保存 panel chart chartelement失败,回滚事务,错误信息:%s", e));
                }
            }
        }
        return successNum;
    }
    
    private List<VisualChart> exportDataByParams(Map<String, Object> params) {
        Integer panelId = null;
        if (ToolUtil.isNotEmpty(params.get("panelId"))) {
            panelId = Integer.valueOf(params.get("panelId").toString());
        }
        Integer chartId = null;
        if (ToolUtil.isNotEmpty(params.get("chartId"))) {
            chartId = Integer.valueOf(params.get("chartId").toString());
        }
        List<VisualChart> chartList = new ArrayList<VisualChart>();
        if (panelId == null) {
            // 导出所有 dashboard panel
            List<VisualPanel> panelList = this.list(new QueryWrapper<VisualPanel>().lambda().eq(VisualPanel::getType, Constant.PanelType.DASHBOARD.getValue()).orderByAsc(VisualPanel::getWeight, VisualPanel::getId));
           // panelList = panelService.getSequentialPanelList(panelList);
            for (VisualPanel panel : panelList) {
                chartList.addAll(this.queryChartsByPanelId(panel.getId(),null));
            }
        } else {
            if (chartId != null) {
                // 只导出当前 chart
                chartList.addAll(this.queryChartsByPanelId(panelId, chartId));
            } else {
                // 导出 panel 下所有chart
                chartList.addAll(this.queryChartsByPanelId(panelId, null));
            }
        }
        return chartList;
    }
    
    private void fillDashboardChartData(List<VisualChart> charts, int rowNum, SXSSFSheet sheet, SXSSFWorkbook workbook, Map chartTypeMapByLanguage) {
        List<VisualPanel> panelTreeList = this.queryList(new HashMap());
        Map<Integer, String> nodeIdAndNamePathMap = new HashMap<>();
        for (VisualPanel panel : panelTreeList) {
            nodeIdAndNamePathMap.put(panel.getId(), panel.getName());
            this.getPanelIdAndNamePathMapFromTree(panel, panel.getName(), nodeIdAndNamePathMap);
        }

        Row row;
        String type;
        CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
        for (VisualChart chart : charts) {
            if(chart == null) continue;
            row = sheet.createRow(rowNum++);
             type = chart.getType();
            row.createCell(0).setCellValue(nodeIdAndNamePathMap.get(chart.getPanelId()));
            row.createCell(1).setCellValue(chart.getName());
            row.createCell(2).setCellValue(this.getI18nChartTypeValue(chartTypeMapByLanguage, type));
            row.createCell(3).setCellValue(chart.getSpan());
            row.createCell(4).setCellValue(chart.getHeight());
            if(StrUtil.equals(Constant.CHART_TEXT_TYPE,type)) {
                row.createCell(5).setCellValue("");
            } else {
                row.createCell(5).setCellValue(this.getUnitDescrByVal(chart.getUnit()));
            }
            row.createCell(6).setCellValue(this.handleChartParam(chart.getParam()));
            row.createCell(7).setCellValue(this.handleChartElementForExport(chart.getElements()));

            // 设置单元格样式
            ExcelUtils.setDataCellStyle(dataStyle, row, 7);
        }
    }
    
    public void getPanelIdAndNamePathMapFromTree(VisualPanel panel, String path, Map<Integer, String> nodeIdAndNamePathMap) {
        if (CollectionUtils.isEmpty(panel.getChildren())) {
            return;
        }
        for (VisualPanel cPanel : panel.getChildren()) {
            String cPath = "";
            cPath += path + "/" + cPanel.getName();
            nodeIdAndNamePathMap.put(cPanel.getId(), cPath);
            this.getPanelIdAndNamePathMapFromTree(cPanel, cPath, nodeIdAndNamePathMap);
        }
    }
    
    public List<VisualChart> queryChartsByPanelId(Integer panelId,Integer chartId) {
    	Map<String,Object> params =new HashMap<String,Object>();
    	params.put("panelId", panelId);
    	params.put("ids", chartId);
        PageUtils queryPage = visualChartService.queryPage(params);
        List<VisualChart> result = (List<VisualChart>) queryPage.getList();
        return result;
    }

}
