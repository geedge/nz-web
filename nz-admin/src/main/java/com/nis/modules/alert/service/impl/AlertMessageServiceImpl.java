package com.nis.modules.alert.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.entity.EndpointState;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.endpoint.service.EndpointStateService;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;



@Service("alertMessageService")
public class AlertMessageServiceImpl extends ServiceImpl<AlertMessageDao, AlertMessageEntity> implements AlertMessageService {
	private Log logger= Log.get();

	@Autowired
	private AssetService assetService;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private AlertRuleService alertRuleService;
	@Autowired
	private EndpointService endpointService;
	@Autowired
	private IdcService idcService;
	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	@Autowired
	private AlertMessageDao alertMessageDao;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private EndpointStateService endpointStateService;

	@Autowired
    private EndpointDao endpointDao;

	/**
	 * 处理请求参数
	 *
	 * @param params
	 */
	private void handleQueryDateParam(Map<String, Object> params) {
		try {
			if (params.get("startAt") != null && !StringUtils.isBlank(params.get("startAt").toString())) {
				Date searchStartAt = DateUtils.parseDate(params.get("startAt").toString(), "yyyy-MM-dd HH:mm:ss");
				params.put("startAt", searchStartAt);
			}
			if (params.get("endAt") != null && !StringUtils.isBlank(params.get("endAt").toString())) {
				Date searchEndAt = DateUtils.parseDate(params.get("endAt").toString(), "yyyy-MM-dd HH:mm:ss");
				params.put("endAt", searchEndAt);
			}
		} catch (ParseException e) {
			logger.error("时间转换异常,参数是 {},异常信息：{}", params, e);
		}
	}

    @Override
    public PageUtils queryPage(Map<String, Object> params)  {

    	this.handleQueryDateParam(params);

	    IPage<AlertMessageEntity> page = new Query<AlertMessageEntity>().getPage(params);

	    List<AlertMessageEntity> alertMessages=this.baseMapper.list(page,params);

		// 处理返回结果
		this.handleResultData(alertMessages);

		page.setRecords(alertMessages);

        return new PageUtils(page);
    }


    private void handleResultData(List<AlertMessageEntity> alertMessages){

		if(CollectionUtils.isEmpty(alertMessages)){
			return;
		}

		List<Idc> idcList = idcService.list();
		Map<Integer,Idc> idcMap = new HashMap<>();
		for (Idc dc:idcList) {
			idcMap.put(dc.getId(), dc);
		}

		List<Endpoint> endpointList = endpointService.list();
		Map<Integer,Endpoint> endpointMap= new HashMap<>();
		for (Endpoint endpoint: endpointList) {
			endpointMap.put(endpoint.getId(),endpoint);
		}

	    Integer endpointId, dcId;
		for (AlertMessageEntity entity:alertMessages) {
			endpointId = entity.getEndpointId();
			if(endpointId != null){
				entity.setEndpoint(endpointMap.get(endpointId));
			}
			dcId = entity.getDcId();
			if(dcId != null){
				entity.setDc(idcMap.get(dcId));
			}
		}
	}

    @Override
    public synchronized void saveBatch(String receiveStr, HttpServletRequest request) {
	    logger.debug(request.getRemoteAddr() + " 推送过来的 promethues 告警消息是 {}", receiveStr);

	    // 接收告警的当前时间
	    Date currentTime = DateUtil.getUTCTimeByConfigTimeZone();

	    List<AlertMessageReceiveDTO> receiveData = JSONObject.parseArray(receiveStr, AlertMessageReceiveDTO.class);
	    if (CollectionUtils.isEmpty(receiveData)) return;

        String[] keys = {Constant.ENDPOINT_ALERT_PUSH, Constant.ENDPOINT_ALERT_RULE, Constant.SYSCONFIG_KEY_EMAIL_ENABLE};
        List<SysConfigEntity> paramValues = configService.list(new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, keys));
        Map<String, String> configMap = paramValues.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        // endpoint state
        this.handleEndpointStateByRule(configMap, receiveData);

	    // 全为endpoint state告警 后续操作不用继续 
	    if (CollectionUtils.isEmpty(receiveData)) return;
	    
	    AlertRuleEntity buildRule = alertRuleService.getOne(new QueryWrapper<AlertRuleEntity>().lambda().eq(AlertRuleEntity::getBuildIn, 1));
	    Integer buildRuleId = buildRule.getId();

	    // 最终保存入库的告警信息
	    List<AlertMessageEntity> entities = new ArrayList<>();
	    // 过期的告警消息
	    List<AlertMessageEntity> expiredAlertList = new ArrayList<>();

	    // 当前激活状态的告警消息
	    List<AlertMessageEntity> pendingAlert = this.list(new QueryWrapper<AlertMessageEntity>().lambda().eq(AlertMessageEntity::getState, 1));
	    Map<String, AlertMessageEntity> alertHashKeyAndEntityMap = new HashMap<>();
	    for (AlertMessageEntity entity : pendingAlert) {
		    alertHashKeyAndEntityMap.put(entity.getHashKey(), entity);
	    }

	    List<Endpoint> endpointList = endpointService.list();
	    Map<Integer, Endpoint> endpointMap = new HashMap<>();
	    for (Endpoint endpoint : endpointList) {
		    endpointMap.put(endpoint.getId(), endpoint);
	    }

	    List<Module> moduleList = moduleService.list();
	    Map<Integer, Module> moduleIdAndEntityMap = new HashMap<>();
	    Map<String, Module> moduleNameAndEntityMap = new HashMap<>();
	    for (Module module : moduleList) {
		    moduleIdAndEntityMap.put(module.getId(), module);
		    moduleNameAndEntityMap.put(module.getName(), module);
	    }

	    List<Asset> assetList = assetService.list();
	    Map<Integer, Asset> assetIdAndEntityMap = new HashMap<>();
	    Map<String, Asset> assetHostAndEntityMap = new HashMap<>();
	    for (Asset asset : assetList) {
		    assetIdAndEntityMap.put(asset.getId(), asset);
		    assetHostAndEntityMap.put(asset.getHost(), asset);
	    }

	    List<Project> projectList = projectService.list();
	    Map<String, Integer> projectNameAndIdMap = projectList.stream().collect(Collectors.toMap(Project::getName, Project::getId));

	    List<Idc> idcList = idcService.list();
	    Map<String, Integer> idcNameAndIdMap = idcList.stream().collect(Collectors.toMap(Idc::getName, Idc::getId));

	    AlertMessageEntity entity, expiredAlert;
	    for (AlertMessageReceiveDTO dto : receiveData) {
		    try {
			    getReceiveLabels(dto, endpointMap, assetIdAndEntityMap, assetHostAndEntityMap, projectNameAndIdMap, idcNameAndIdMap, moduleIdAndEntityMap, moduleNameAndEntityMap);
			    entity = AlertMessageReceiveDTO.toAlertMessageEntity(dto, buildRuleId);

			    // 如果是内置告警直接入库
			    if (buildRuleId.equals(entity.getRuleId())) {
				    entities.add(entity);
			    } else {
				    Date endAt = entity.getEndAt();
				    // 当前时间比结束时间早 告警激活入库
				    if (currentTime.before(endAt)) {
					    // 已存在告警则不保存入库
					    if (alertHashKeyAndEntityMap.get(entity.getHashKey()) != null) continue;
					    entity.setState(1);
					    entity.setEndAt(null);
					    entities.add(entity);
				    } else {
					    // 当前时间比结束时间晚 告警失效
					    expiredAlert = alertHashKeyAndEntityMap.get(entity.getHashKey());
					    // 消息为空或者消息已经过期则不进行操作
					    if(expiredAlert == null || expiredAlert.getState().equals(2)) continue;

					    expiredAlert.setState(2);
					    expiredAlert.setEndAt(entity.getEndAt());
					    expiredAlertList.add(expiredAlert);
				    }
			    }
		    } catch (Exception e) {
			    logger.error("解析告警信息失败，参数是：" + JSONObject.toJSONString(dto) + " 错误信息：" ,e);
			    throw new NZException(RCode.ALERTMSG_SAVE_ERROR);
		    }
	    }
	    // 处理转换之后的数据
	    this.handleConvertedMsg(entities, expiredAlertList, configMap.get(Constant.SYSCONFIG_KEY_EMAIL_ENABLE));
    }

	/**
	 * 从 告警中获取 endpoint state 状态
	 * @param configMap
	 * @param receiveData
	 */
	private void handleEndpointStateByRule(Map<String, String> configMap, List<AlertMessageReceiveDTO> receiveData) {
		String endpointAlertPush = configMap.get(Constant.ENDPOINT_ALERT_PUSH);
		String endpointAlertRule = configMap.get(Constant.ENDPOINT_ALERT_RULE);
		Integer ednpointAlertName = 0;
		if (ToolUtil.isNotEmpty(endpointAlertRule)) {
			Map endpointAlertRuleMap = (Map) JSON.parse(endpointAlertRule);
			ednpointAlertName = Integer.parseInt((String) endpointAlertRuleMap.get("alertName"));
		}
		if ("1".equals(endpointAlertPush)) {
			Set<EndpointState> endpointStates = new HashSet<>();
			// 用于判断 endpoint id 是否重复
			Set<Integer> tempEndpointIds = new HashSet<>();
			EndpointState endpointState;
			for (int i = receiveData.size() - 1; i >= 0; i--) {
				Map receiveLabels = receiveData.get(i).getLabels();
				Map receiveAnnotations = receiveData.get(i).getAnnotations();
				int receiveAlertName = Integer.parseInt((String) receiveLabels.get("alertname"));
				if (receiveAlertName == ednpointAlertName && ToolUtil.isNotEmpty(receiveLabels.get("endpoint"))) {
					Integer endpointId = Integer.valueOf(receiveLabels.get("endpoint").toString());
					if (!tempEndpointIds.contains(endpointId)) {
						endpointState = new EndpointState();
						endpointState.setId(endpointId);
						endpointState.setEndpointId(endpointId);
						endpointState.setCreateTime(DateUtil.getUTCTimeByConfigTimeZone());
						endpointState.setState(Integer.parseInt((String) receiveAnnotations.get("summary")));
						endpointStates.add(endpointState);

						tempEndpointIds.add(endpointId);
					}
					receiveData.remove(i);
				}
			}
			// 将产生告警的id信息存入endpoint_state表中
			if (ToolUtil.isNotEmpty(endpointStates)) {
				//告警一次不会推送全部告警情况 不再清表 根据endpoint_id执行批量的新增或者修改
				endpointStateService.saveOrUpdateBatch(endpointStates);
			}

			List<Integer> suspendedEndpointIds = endpointDao.getSuspendedEndpointIdsByAssetState();
			// 停用的 endpoint ids
			List<Integer> disabledEndpointIds = endpointDao.getDisabledEndpointIds();
			suspendedEndpointIds.addAll(disabledEndpointIds);
			suspendedEndpointIds = suspendedEndpointIds.stream().distinct().collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(suspendedEndpointIds)) {
				endpointStateService.remove(new LambdaQueryWrapper<EndpointState>().eq(EndpointState::getState, 2));
				endpointStates = new HashSet<>();
				Date now = DateUtil.getUTCTimeByConfigTimeZone();
				for (Integer endpointId : suspendedEndpointIds) {
					endpointState = new EndpointState(endpointId, endpointId, now, 2);
					endpointStates.add(endpointState);
				}
				endpointStateService.saveBatch(endpointStates);
			}
		} else {
			// 当endpoint state开关关闭时 防止相关告警存入alert_message表
			for (int i = receiveData.size() - 1; i >= 0; i--) {
				Map receiveLabels = receiveData.get(i).getLabels();
				int receiveAlertName = Integer.parseInt((String) receiveLabels.get("alertname"));
				if (receiveAlertName == ednpointAlertName) {
					receiveData.remove(i);
				}
			}
		}
	}

	/**
	 * 入库、过期、告警通知
	 * @param entities
	 * @param expiredAlertList
	 * @param emailEnable
	 */
	private void handleConvertedMsg(List<AlertMessageEntity> entities, List<AlertMessageEntity> expiredAlertList, String emailEnable) {
		logger.debug("状态变化的告警消息信息为 : {}", JSONObject.toJSONString(expiredAlertList));
		if (ToolUtil.isNotEmpty(expiredAlertList)) {
			this.updateBatchById(expiredAlertList);
		}

		logger.debug("告警消息处理完成，入库消息为 : {}", JSONObject.toJSONString(entities));
		if (CollectionUtils.isNotEmpty(entities)) {
			this.saveBatch(entities);
		}

		// 邮件通知
		if(StrUtil.equals(Constant.SWITCH_OPEN,emailEnable) && (CollectionUtils.isNotEmpty(entities) || CollectionUtils.isNotEmpty(expiredAlertList))) {
			// 邮件通知 告警 pending 与 expired
			entities.addAll(expiredAlertList);
			String jsonString = JSONObject.toJSONString(entities);
			logger.debug("向redi放入邮件消息数据: {}", jsonString);
			redisTemplate.opsForList().leftPush(Constant.REDIS_KEY_EMAIL_ALERT_MESSAGE, jsonString);
		}
	}

	private AlertMessageReceiveDTO getReceiveLabels(AlertMessageReceiveDTO dto, Map<Integer, Endpoint> endpointMap, Map<Integer, Asset> assetIdAndEntityMap, Map<String, Asset> assetHostAndEntityMap, Map<String, Integer> projectNameAndIdMap, Map<String, Integer> idcNameAndIdMap, Map<Integer, Module> moduleIdAndEntityMap, Map<String, Module> moduleNameAndEntityMap) {
		Map receiveLabels = dto.getLabels();
		Map labelIds = new HashMap();
		Integer assertId = null, moduleId = null, projectId = null, endpointId = null, dcId = null;
		// 有endpointId ,即可查出所有
		if (ToolUtil.isNotEmpty(receiveLabels.get("endpoint"))) {
			endpointId = Integer.valueOf(receiveLabels.get("endpoint").toString());
			Endpoint endpoint = endpointMap.get(endpointId);
			assertId = endpoint.getAssetId();
			moduleId = endpoint.getModuleId();
			Module module = moduleIdAndEntityMap.get(moduleId);
			projectId = module.getProjectId();
			Asset asset = assetIdAndEntityMap.get(assertId);
			dcId = asset.getIdcId();
		} else {
			if (ToolUtil.isNotEmpty(receiveLabels.get("module"))) {
				Module module = moduleNameAndEntityMap.get(receiveLabels.get("module").toString());
				moduleId = module.getId();
				projectId = module.getProjectId();
			}
			if (ToolUtil.isNotEmpty(receiveLabels.get("project")) && ToolUtil.isEmpty(receiveLabels.get("module"))) {
				projectId = projectNameAndIdMap.get(receiveLabels.get("project").toString());
			}
			if (ToolUtil.isNotEmpty(receiveLabels.get("asset"))) {
				Asset asset = assetHostAndEntityMap.get(receiveLabels.get("asset").toString());
				assertId = asset.getId();
				dcId = asset.getIdcId();
			}
			if (ToolUtil.isNotEmpty(receiveLabels.get("datacenter")) && ToolUtil.isEmpty(receiveLabels.get("asset"))) {
				dcId = idcNameAndIdMap.get(receiveLabels.get("datacenter").toString());
			}
		}
		labelIds.put("endpoint", endpointId);
		labelIds.put("project", projectId);
		labelIds.put("assetId", assertId);
		labelIds.put("module", moduleId);
		labelIds.put("datacenter", dcId);
		dto.setLabelIds(labelIds);

		return dto;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeByAssetIds(List<Integer> idList) {
		alertMessageDao.deleteByAssetIds(idList);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeByModuleIds(List ids) {
		alertMessageDao.deleteByModuleIds(ids);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeByProjectIds(List ids) {
		alertMessageDao.deleteByProjectIds(ids);
	}

	@Override
    public void export(HttpServletResponse response, Map<String, Object> params) {
        // 从 sys_config 获取 表头信息    每次加载条数  最大条数限制
        String[] keys = {"alert_export_header", "export_pagesize", "export_max_line"};
        List<SysConfigEntity> paramValues = this.configService.list(new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey, keys));
        Map<String, String> configMap = paramValues.stream().collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        // 导出最大条数限制
        int exportMaxLine = Integer.parseInt(configMap.get("export_max_line"));
        int exportRealTotal = 0;

        //Excel2003版最大行数是65536，Excel2007最大行数是1048576
        //Excel2003版最大列数是256，Excel2007最大列数是16384
        //使用SXSSFWorkbook避免Excel数据超出65536行时报错
        SXSSFWorkbook workbook = null;
        try {
            workbook = new SXSSFWorkbook();
            Sheet sheet = workbook.createSheet("AlertMessages");
            sheet.setDefaultColumnWidth(25);
            int rowNum = 0;
            Row row = sheet.createRow(rowNum);

            // 得到表头
            String language = ToolUtil.isEmpty(params.get("language")) ? "en" : params.get("language").toString();
            Map map = (Map) (JSON.parseObject(configMap.get("alert_export_header"), Feature.OrderedField).get(language));
            Collection values = map.values();
            List<String> valueList = new ArrayList<>(values.size());
            values.stream().forEach(value -> valueList.add(value.toString()));
            // 筛选表头并添加表头至excel
            CellStyle colStyle = ExcelUtils.createHeaderStyle(workbook);
            for (int i = 0; i < valueList.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(valueList.get(i));
                cell.setCellStyle(colStyle);
            }

            PageUtils page = this.queryPage(params);
            List<AlertMessageEntity> exportData = (List<AlertMessageEntity>) page.getList();
            exportRealTotal = exportData.size();

            // 判断导出条数是否超过配置最大导出量
            if (exportData.size() > exportMaxLine) {
                throw new NZException(RCode.SYS_CONFIG_EXPORT_MAX_LINE);
            }

            // 单元格样式
            CellStyle cellStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);
            fill(exportData, rowNum, sheet, cellStyle);

            // 响应为流
            String fileName = "AlertMessagesExport.xlsx"; // 默认名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            try {
                String errMsg;
                if (e instanceof NZException) {
                    errMsg = String.format("Configure the maximum number of export: %s, query the total number of export: %s", exportMaxLine, exportRealTotal);
                } else {
                    errMsg = "Export error";
                }
	            response.sendError(413, errMsg);
            } catch (IOException ex) {
                logger.error(ex);
            }
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

	@Override
	public void updateAlertStateByIds(Map<String, Object> params) {
		// 当前时间
		Date date = DateUtil.getUTCTimeByConfigTimeZone();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formatDate = df.format(date);
		// 当前用户
		SysUserEntity user = (SysUserEntity) SecurityUtils.getSubject().getPrincipal();

		String remark = (String) params.get("remark");
		//remark = formatDate + "  " + user.getUsername() + "  " + remark;
		remark = user.getUsername() + "  " + remark;
		params.put("remark", remark);

		String ids = (String) params.get("ids");
		List<String> idList = Arrays.asList(ids.split(","));

		Integer state = (Integer) params.get("state");
		// 如果状态设置为Expired 设置结束时间
		if (state.equals(2)) {
			params.put("endAt", formatDate);
		}

		alertMessageDao.updateAlertStateByIds(idList, params);

		List<AlertMessageEntity> list = this.list(new QueryWrapper<AlertMessageEntity>().lambda().in(AlertMessageEntity::getId, idList));
		List<String> hashKeys = list.stream().map(AlertMessageEntity::getHashKey).distinct().collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(hashKeys)) {
			HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
			opsForHash.delete(Constant.REDIS_KEY_ALERTMESSAGE, hashKeys.toArray());
		}
	}

    @Override
    public List<MetricsDto> queryAlertMessageStatisticsInfoByRule() {
        return alertMessageDao.queryAlertMessageStatisticsInfoByRule();
    }

    /**
	 * 填充表格数据
	 */
    private static void fill(List<AlertMessageEntity> list, int rowNum, Sheet sheet, CellStyle dataStyle) {
        Row r;
        for (AlertMessageEntity entity : list) {
            r = sheet.createRow(++rowNum);
            AlertRuleEntity alertRule = entity.getAlertRule();
            String ruleName = alertRule == null ? "" : alertRule.getAlertName();
            Idc dc = entity.getDc();
            String dcName = dc == null ? "" : dc.getName();
            Project project = entity.getProject();
            String projectName = project == null ? "" : project.getName();
            Module module = entity.getModule();
            String moduleName = module == null ? "" : module.getName();
            Endpoint endpoint = entity.getEndpoint();
	        String endpointId = endpoint == null ? "" : endpoint.getId() + "";
            Asset asset = entity.getAsset();
            String assetHost = asset == null ? "" : asset.getHost();
	        String startAt = ToolUtil.isNotEmpty(entity.getStartAt()) ? DateUtil.formatDate(entity.getStartAt(), "yyyy-MM-dd HH:mm:ss") : "";
	        String endAt = ToolUtil.isNotEmpty(entity.getEndAt()) ? DateUtil.formatDate(entity.getEndAt(), "yyyy-MM-dd HH:mm:ss") : "";

            r.createCell(0).setCellValue(entity.getId());
            r.createCell(1).setCellValue(ruleName);
            r.createCell(2).setCellValue(dcName);
            r.createCell(3).setCellValue(projectName);
            r.createCell(4).setCellValue(moduleName);
            r.createCell(5).setCellValue(endpointId);
            r.createCell(6).setCellValue(assetHost);
            r.createCell(7).setCellValue(entity.getLabels());
            r.createCell(8).setCellValue(entity.getSeverity());
            r.createCell(9).setCellValue(entity.getSummary());
            r.createCell(10).setCellValue(entity.getDescription());
            r.createCell(11).setCellValue(startAt);
            r.createCell(12).setCellValue(endAt);
            r.createCell(13).setCellValue(entity.getState() == 1 ? "pending" : "expired");

            ExcelUtils.setDataCellStyle(dataStyle, r, 13);
        }
    }

	@Override
	public void saveBatchToCache(String receiveStr, HttpServletRequest request) throws Exception {
		logger.debug(request.getRemoteAddr() + " 推送过来的 promethues 告警消息是 {}", receiveStr);

	    // 接收告警的当前时间
	    Date currentTime = DateUtil.getUTCTimeByConfigTimeZone();

	    List<AlertMessageReceiveDTO> receiveData = JSONObject.parseArray(receiveStr, AlertMessageReceiveDTO.class);
	    if (CollectionUtils.isEmpty(receiveData)) return;
    
	    AlertRuleEntity buildRule = alertRuleService.getOne(new QueryWrapper<AlertRuleEntity>().lambda().eq(AlertRuleEntity::getBuildIn, 1));
	    Integer buildRuleId = buildRule.getId();

	    // 当前激活状态的告警消息
	    List<AlertMessageEntity> pendingAlert = this.list(new QueryWrapper<AlertMessageEntity>().lambda().eq(AlertMessageEntity::getState, 1));
	    Map<String, AlertMessageEntity> alertHashKeyAndEntityMap = new HashMap<>();
	    for (AlertMessageEntity entity : pendingAlert) {
		    alertHashKeyAndEntityMap.put(entity.getHashKey(), entity);
	    }

	    List<Endpoint> endpointList = endpointService.list();
	    Map<Integer, Endpoint> endpointMap = new HashMap<>();
	    for (Endpoint endpoint : endpointList) {
		    endpointMap.put(endpoint.getId(), endpoint);
	    }

	    List<Module> moduleList = moduleService.list();
	    Map<Integer, Module> moduleIdAndEntityMap = new HashMap<>();
	    Map<String, Module> moduleNameAndEntityMap = new HashMap<>();
	    for (Module module : moduleList) {
		    moduleIdAndEntityMap.put(module.getId(), module);
		    moduleNameAndEntityMap.put(module.getName(), module);
	    }

	    List<Asset> assetList = assetService.list();
	    Map<Integer, Asset> assetIdAndEntityMap = new HashMap<>();
	    Map<String, Asset> assetHostAndEntityMap = new HashMap<>();
	    for (Asset asset : assetList) {
		    assetIdAndEntityMap.put(asset.getId(), asset);
		    assetHostAndEntityMap.put(asset.getHost(), asset);
	    }

	    List<Project> projectList = projectService.list();
	    Map<String, Integer> projectNameAndIdMap = projectList.stream().collect(Collectors.toMap(Project::getName, Project::getId));

	    List<Idc> idcList = idcService.list();
	    Map<String, Integer> idcNameAndIdMap = idcList.stream().collect(Collectors.toMap(Idc::getName, Idc::getId));

	    AlertMessageEntity entity;
	    List<AlertMessageEntity> entities =new ArrayList<AlertMessageEntity>();
	    ListOperations<String, Object> opsForList = redisTemplate.opsForList();
	    
	    for (AlertMessageReceiveDTO dto : receiveData) {
		    try {
			    getReceiveLabels(dto, endpointMap, assetIdAndEntityMap, assetHostAndEntityMap, projectNameAndIdMap, idcNameAndIdMap, moduleIdAndEntityMap, moduleNameAndEntityMap);
			    entity = AlertMessageReceiveDTO.toAlertMessageEntity(dto, buildRuleId);
			    entities.add(entity);
			    
		    } catch (Exception e) {
			    logger.error("解析告警信息失败，参数是：" + JSONObject.toJSONString(dto) + " 错误信息：" ,e);
			    throw new NZException(RCode.ALERTMSG_SAVE_ERROR);
		    }
	    }
	    // 存入缓存
	    opsForList.leftPush(Constant.ALERT_MESSAGE_RECV, JSON.toJSONString(entities));
	}
}