package com.nis.modules.endpoint.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.entity.EndpointState;
import com.nis.modules.endpoint.entity.StateInfo;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.endpoint.service.EndpointStateService;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Service("endpointService")
public class EndpointServiceImpl  extends ServiceImpl<EndpointDao, Endpoint> implements EndpointService {
	
	private static Log logger = Log.get();
	@Autowired
	private EndpointDao endpointDao;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private AssetService assetService;
	@Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;
	@Autowired
	private TransactionDefinition transactionDefinition;
	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private AlertMessageDao alertMessageDao;

	@Autowired
	private SysConfigDao sysConfigDao;

	@Autowired
	private EndpointStateService endpointStateService;
	
	@Value("${nezha.endpointStateTime}")
	private String endpointStateTime; 
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		logger.info("queryPage param is : {}",JSON.toJSON(params));
		IPage<Endpoint> page = new Query<Endpoint>().getPage(params);
		List<Endpoint> datas = endpointDao.queryPage(page, params);

		SysConfigEntity config = sysConfigDao.selectOne(new LambdaQueryWrapper<SysConfigEntity>().eq(SysConfigEntity::getParamKey, Constant.ENDPOINT_ALERT_PUSH));
		String endpointStateAlertSwitch = config.getParamValue();
		//获取当前时间
		Date nowTime = DateUtil.getUTCTimeByConfigTimeZone();
		long time = nowTime.getTime();
		//获取配置文件里的配置时间信息
		Integer alertTime=60000;
		if(ToolUtil.isNotEmpty(endpointStateTime)) {
			alertTime=Integer.valueOf(endpointStateTime);
		}
		for(Endpoint data : datas) {
			StateInfo stateInfo =new StateInfo();
			EndpointState endpointState = data.getEndpointState();
			if(ToolUtil.isNotEmpty(endpointState)) {
				long createTime = endpointState.getCreateTime().getTime();
				// 判断当数据的创建时间大于两个周期的时间时 prometheus down更新数据状态
				if (!"2".equals(endpointStateAlertSwitch) && time - createTime > alertTime) {
					data.setState(0);
					stateInfo.setCode(RCode.ENDPOINT_TIMEOUT_UPDATE.getCode());
					stateInfo.setMsg(RCode.ENDPOINT_TIMEOUT_UPDATE.getMsg());
				}else {
					Integer state = endpointState.getState();
					if(state.equals(0)) {
						data.setState(state);
						stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_UNUSE.getCode());
						stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_UNUSE.getMsg());
					}else {
						data.setState(state);
					}
				}
			}else {
				//数据库中无数据 则状态为down
				data.setState(0);
				stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_UNUSE.getCode());
				stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_UNUSE.getMsg());
			}
			data.setStateInfo(stateInfo);
		}
		//获取相关lastUpdate以及state数据信息
		/**try {
			if(ToolUtil.isNotEmpty(datas)) {
				Map resultMap=new HashMap();
				Map map =new HashMap();
				map.put("query", "up");
				JSONObject promeResult = promserverProxyService.query(map);
				JSONObject promeObject = (JSONObject) promeResult.get("data");
				JSONArray str = (JSONArray) promeObject.get("result");
				if(ToolUtil.isNotEmpty(str)) {
					for(int i=0;i<str.size();i++) {
						Map resultPromeMap = (Map) str.get(i);
						JSONObject metric = (JSONObject) resultPromeMap.get("metric");
						String jobName = (String) metric.get("endpoint");
						resultMap.put(jobName, resultPromeMap.get("value"));
					}
				}
				for(Endpoint data: datas) {
					 // 从redis中根据endpointId获取promserver id
			        Object promeId = redisTemplate.opsForHash().get(Constant.REDIS_KEY_ENDPOINT_PROM, data.getId().toString());
			        Promserver promserver =null;
			        StateInfo stateInfo =new StateInfo();
			        if (promeId != null) {
			            promserver = promserverService.getById(promeId.toString());
			            if(promserver!=null) {
							String jn=data.getId()+"";
							JSONArray valueList = (JSONArray) resultMap.get(jn);
							if(valueList!=null) {
								BigDecimal time = (BigDecimal) valueList.get(0);
								long timeStamp = time.longValue();
								data.setLastUpdate(timeStamp);
								String stateValue=(String)valueList.get(1);
								Integer state = Integer.valueOf(stateValue);
								data.setState(state);
								if(state.equals(0)) {
									// endpoint 连接失败
									stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_UNUSE.getCode());
									stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_UNUSE.getMsg());
								}else {
									stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_ON_USE.getCode());
									stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_ON_USE.getMsg());
								}
							}
			            }
			        }
			        if(data.getState()==null){
						data.setState(0);
						stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_UNAVAILABLE.getCode());
						stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_UNAVAILABLE.getMsg());
					}
					data.setStateInfo(stateInfo);
				}
			}
		} catch (Exception e) {
			logger.error(e);
			// 处理prometheus不可用时 出现异常 无状态显示问题
			for(Endpoint data: datas) {
				StateInfo stateInfo =new StateInfo();
				if(data.getState()==null){
					data.setState(0);
					stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_UNAVAILABLE.getCode());
					stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_UNAVAILABLE.getMsg());
				}
				data.setStateInfo(stateInfo);
			}
		}**/

		// endpoint 告警数量
		this.handleEndpointsAlertNum(datas);

        page.setRecords(datas);
        return new PageUtils(page);
	}

	private void handleEndpointsAlertNum(List<Endpoint> endpointList) {
		if (CollectionUtils.isEmpty(endpointList)) return;

		List<AlertMessageEntity> alerts = alertMessageDao.getAlertsStatColumn();
		alerts = alerts.stream().filter(alert -> ToolUtil.isNotEmpty(alert.getEndpointId())).collect(Collectors.toList());
		Map<Integer, List<AlertMessageEntity>> endpointIdAndAlersMap = alerts.stream().collect(Collectors.groupingBy(AlertMessageEntity::getEndpointId));

		List<AlertMessageEntity> tempAlert;
		for (Endpoint endpoint : endpointList) {
			tempAlert = endpointIdAndAlersMap.get(endpoint.getId());
			if (CollectionUtils.isEmpty(tempAlert)) {
				endpoint.setAlertNum(0);
			} else {
				endpoint.setAlertNum(tempAlert.size());
			}
		}
	}

	public void validateParam(List<Endpoint> endpointList) {
		List<Module> moduleList = moduleService.list();
		List<Integer> moduleIdList = moduleList.stream().map(Module::getId).collect(Collectors.toList());

		List<Asset> assetList = assetService.list();
		List<Integer> assetIdList = assetList.stream().map(Asset::getId).collect(Collectors.toList());

		String labels;
		Integer moduleId,assetId,enabled;
		for (Endpoint endpoint : endpointList) {
			moduleId = endpoint.getModuleId();
			assetId = endpoint.getAssetId();

			ValidateUtils.is(moduleId).notNull(RCode.ENDPOINT_MODULEID_ISNULL)
					.and(assetId).notNull(RCode.ENDPOINT_ASSETID_ISNULL)
					.and(endpoint.getPort()).notNull(RCode.ENDPOINT_PORT_ISNULL);

			if(!moduleIdList.contains(moduleId)){
				throw new NZException(RCode.MODULE_NOTFOUND);
			}

			if(!assetIdList.contains(assetId)){
				throw new NZException(RCode.ASSET_NOT_EXIST);
			}

			labels = endpoint.getLabels();
			if (StringUtils.isNotEmpty(labels)) {
				Map labelMap;
				try {
					labelMap = JSONObject.parseObject(labels, Map.class);
				} catch (Exception e) {
					throw new NZException(RCode.ENDPOINT_LABELS_FORMAT);
				}

				labelMap.keySet().stream().forEach(key -> {
					if (!Constant.PROMETHEUS_LABELSKEY_PATTERN.matcher(key.toString()).matches()) {
						throw new NZException("labels's key " + key + " format is incorrect", RCode.ENDPOINT_LABELS_KEY_FORMAT.getCode());
					}
				});
			}

			enabled = endpoint.getEnabled();
			if (enabled != null) {
				if (enabled != 0 && enabled != 1) {
					throw new NZException(RCode.ENDPOINT_ENABLED_INVALIDE);
				}
			}
		}
	}

	@Override
	public List<MetricsDto> queryEndpointStatisticsInfoByModule() {
		return endpointDao.selectEndpointStatisticsInfoByModule();
	}

	@Override
	public Map importEndpoint(MultipartFile excelFile, String language) {
		String seqUUID = UUID.randomUUID().toString();
		logger.info(String.format("导入的文件是： %s, 本次导入的 uuid是：%s", excelFile.getOriginalFilename(), seqUUID));

		String originalFilename = excelFile.getOriginalFilename();
		if (!ExcelUtils.validateFileType(originalFilename)) {
			throw new NZException(RCode.ENDPOINT_IMPORTFILE_TYPE);
		}

		Workbook work = null;
		try {
			work = ExcelUtils.getWork(originalFilename, excelFile.getInputStream());
		} catch (IOException e) {
			logger.error("解析endpoint导入文件格式有误",e);
			throw new NZException(RCode.ENDPOINT_IMPORTFILE_FAILED);
		} finally {
			if (work != null) {
				IOUtils.closeQuietly(work);
			}
		}

		Sheet sheet = work.getSheetAt(0);
		// 表头校验
		SysConfigEntity config = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ENDPOINT_EXPORT_HEADER));
		HashMap<String, Object> map = JSONObject.parseObject(config.getParamValue(), HashMap.class, Feature.OrderedField);
		boolean headerFlag = ExcelUtils.validateExcelHeader(sheet, sheet.getFirstRowNum(), map.values());
		if (!headerFlag) {
			throw new NZException(RCode.ENDPOINT_EXPORT_HEADER_TEMPLATE_ERROR);
		}

		Map<String, String> exportHeader = (Map) map.get(language);
		List<String> headerInfos = new ArrayList(exportHeader.keySet());

		List<Map> failDetails = new ArrayList<>();
		// 保存并返回成功条数
		int successNum = this.saveEndpointDataFromSheet(sheet, failDetails, headerInfos, seqUUID);
		int totalNum = sheet.getLastRowNum() - sheet.getFirstRowNum();

		Map resultMap = new HashMap(8);

		resultMap.put("seq", seqUUID);
		resultMap.put("totalNum", totalNum);
		resultMap.put("successNum", successNum);
		resultMap.put("failNum", totalNum - successNum);
		resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));

		return resultMap;
	}

	private int saveEndpointDataFromSheet(Sheet sheet, List<Map> failDetails, List<String> headerInfos, String seqUUID) {
		List<Project> projects = projectService.list();
		List<Module> modules = moduleService.list();
		List<Asset> assets = assetService.list();
		Map<String, Integer> projectNameAndIdMap = projects.stream().collect(Collectors.toMap(Project::getName, Project::getId));
		Map<String, Integer> assetSnAndIdMap = assets.stream().collect(Collectors.toMap(Asset::getSn, Asset::getId));
		Map<String, Integer> assetHostAndIdMap = assets.stream().collect(Collectors.toMap(Asset::getHost, Asset::getId));
		Map<Integer, String> assetIdAndHostMap = assets.stream().collect(Collectors.toMap(Asset::getId, Asset::getHost));
		Map<String, Module> moduleNameAndModuleMap = new HashMap<>();
		for (Module module : modules) {
			moduleNameAndModuleMap.put(module.getName(), module);
		}
		Row row;
		int successNum = 0;
		for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
			row = sheet.getRow(i);
			if (row == null) continue;

			boolean saveFlag = true;
			int dataFromExcelRowNum = i + 1;
			String projectName = ExcelUtils.getCellValue(row.getCell(0));
			if (StringUtils.isEmpty(projectName)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.PROJECT_NAME_ISNULL.getMsg());
			}
			String moduleName = ExcelUtils.getCellValue(row.getCell(1));
			if (StringUtils.isEmpty(moduleName)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NAME_ISNULL.getMsg());
			}
			String type = ExcelUtils.getCellValue(row.getCell(2));
			if (StringUtils.isEmpty(type)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.MODULE_TYPE_ISNULL.getMsg());
			} else {
				type = type.toLowerCase();
				if(!StrUtil.equals(Constant.MODULE_TYPE_HTTP,type) && !StrUtil.equals(Constant.MODULE_TYPE_SNMP,type)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.MODULE_TYPE_INVALID.getMsg());
				}
				// type 类型下必须存在 module
				if(StrUtil.equals(Constant.MODULE_TYPE_SNMP,type) && ToolUtil.isEmpty(moduleNameAndModuleMap.get(moduleName))) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_SNMP_NOTEXIST.getMsg());
				}
			}
			// module 存在 校验相关内容
			if (ToolUtil.isNotEmpty(moduleNameAndModuleMap.get(moduleName)) && saveFlag) {
				Integer projectId = projectNameAndIdMap.get(projectName);
				Module module = moduleNameAndModuleMap.get(moduleName);
				// moduel 存在 project 不存在 ， module 名称重复
				if (projectId == null) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NAME_DUPLICATE.getMsg());
				} else {
					// project 存在 校验 module 是否存在 这个 project 中
					if (!module.getProjectId().equals(projectId)) {
						saveFlag = false;
						this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NOTEXIS_PROJECT.getMsg());
					}
				}
				// 校验 module type 与输入得是否匹配
				if(!StrUtil.equals(module.getType (),type.toLowerCase())) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.MODULE_TYPE_INCONSISTENT.getMsg());
				}
			}
			String sn = ExcelUtils.getCellValue(row.getCell(3));
			String host = ExcelUtils.getCellValue(row.getCell(8));
			// sn host 不可同时为空
			if (StringUtils.isEmpty(sn) && StringUtils.isEmpty(host)) {
				saveFlag = false;
				this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ENDPOINT_SN_HOST_ISNULL.getMsg());
			} else if (StringUtils.isNotEmpty(sn) && StringUtils.isNotEmpty(host)) {
				// 都不为空 校验 sn 是否可以找到 asset , host 格式是否正确
				if (!CommonUtils.checkIp(host)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_HOST_FORMAT.getMsg());
				}
				if (ToolUtil.isEmpty(assetSnAndIdMap.get(sn))) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_NOT_EXIST.getMsg());
				}
			} else if (StringUtils.isNotEmpty(sn) && StringUtils.isEmpty(host)) {
				if (ToolUtil.isEmpty(assetSnAndIdMap.get(sn))) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.ASSET_NOT_EXIST.getMsg());
				}
			} else if (StringUtils.isEmpty(sn) && StringUtils.isNotEmpty(host)) {
				if (!CommonUtils.checkIp(host)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_HOST_FORMAT.getMsg());
				}
				if (ToolUtil.isEmpty(assetHostAndIdMap.get(host))) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(8), RCode.ASSET_NOT_EXIST.getMsg());
				}
			}

			String port = ExcelUtils.getCellValue(row.getCell(4));
			if (StringUtils.isNotEmpty(port)) {
				try {
					Double aDouble = Double.valueOf(port);
					if (!CommonUtils.checkPort(String.valueOf(aDouble.intValue()))) {
						saveFlag = false;
						this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.MODULE_PORT_ERROR.getMsg());
					}
				} catch (NumberFormatException e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.MODULE_PORT_ERROR.getMsg());
				}
			}

			String param = ExcelUtils.getCellValue(row.getCell(5));
			if (StringUtils.isNotEmpty(param)) {
				try {
					JSONObject.parseObject(param);
				} catch (Exception e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(5), RCode.ENDPOINT_PARAM_FORMAT_ERROR.getMsg());
				}
			}

			String labels = ExcelUtils.getCellValue(row.getCell(6));
			if (StringUtils.isNotEmpty(labels)) {
				Map labelMap = null;
				try {
					labelMap = JSONObject.parseObject(labels, Map.class);
				} catch (Exception e) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ENDPOINT_LABELS_FORMAT.getMsg());
				}

				if (ToolUtil.isNotEmpty(labelMap)) {
					for (Object key : labelMap.keySet()) {
						if (!Constant.PROMETHEUS_LABELSKEY_PATTERN.matcher(key.toString()).matches()) {
							saveFlag = false;
							this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(6), RCode.ENDPOINT_LABELS_KEY_FORMAT.getMsg());
						}
					}
				}
			}

			String path = ExcelUtils.getCellValue(row.getCell(7));

			String enabled = ExcelUtils.getCellValue(row.getCell(9));
			if (StringUtils.isNotEmpty(enabled)) {
				if (!"1".equals(enabled) && !"0".equals(enabled)) {
					saveFlag = false;
					this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(9), RCode.ENDPOINT_ENABLED_INVALIDE.getMsg());
				}
			}

			// 前面没有报错
			if (saveFlag) {
				TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
				Integer projectId = projectNameAndIdMap.get(projectName);
				Module module = moduleNameAndModuleMap.get(moduleName);
				Integer moduleId = ToolUtil.isEmpty(module) ? null : module.getId();
				// project 为空 module 一定为空
				try {
					if (projectId == null) {
						Project project = new Project();
						project.setName(projectName);
						project.setBuildIn(0);
						project.setSeq(seqUUID);
						projectService.save(project);
						projectId = project.getId();
						// 先校验 module name 是否重复
						if (ToolUtil.isNotEmpty(module)) {
							this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.MODULE_NAME_DUPLICATE.getMsg());
							continue;
						}
						// 由于 type 为 snmp 类型时 module必须存在 所以此处保存的为 http 类型 module
						module = new Module();
						module.setProjectId(projectId);
						module.setName(moduleName);
						module.setType(Constant.MODULE_TYPE_HTTP);
						module.setPort(9100);
						module.setPath(Constant.MODULE_HTTP_DEFAULT_PATH);
						module.setBuildIn(0);
						module.setSeq(seqUUID);
						moduleService.save(module);
						moduleId = module.getId();
					} else if (module == null) {
						module = new Module();
						module.setProjectId(projectId);
						module.setName(moduleName);
						module.setType(Constant.MODULE_TYPE_HTTP);
						module.setPort(9100);
						module.setPath(Constant.MODULE_HTTP_DEFAULT_PATH);
						module.setBuildIn(0);
						module.setSeq(seqUUID);
						moduleService.save(module);
						moduleId = module.getId();
					}
					Integer assetIdBySn = assetSnAndIdMap.get(sn);
					Integer assetIdByHost = assetHostAndIdMap.get(host);
					Endpoint endpoint = new Endpoint();

					// endpoint asset_id endpoint_host
					if (StringUtils.isNotEmpty(sn) && StringUtils.isNotEmpty(host)) {
						endpoint.setAssetId(assetIdBySn);
						endpoint.setHost(host);
					} else if (StringUtils.isNotEmpty(sn) && StringUtils.isEmpty(host)) {
						endpoint.setAssetId(assetIdBySn);
						endpoint.setHost(assetIdAndHostMap.get(assetIdBySn));
					} else if (StringUtils.isEmpty(sn) && StringUtils.isNotEmpty(host)) {
						endpoint.setAssetId(assetIdByHost);
						endpoint.setHost(assetIdAndHostMap.get(assetIdByHost));
					}

					endpoint.setModuleId(moduleId);
					endpoint.setPort(ToolUtil.isEmpty(port) ? module.getPort() : Double.valueOf(port).intValue());
					endpoint.setParam(ToolUtil.isEmpty(param) ? module.getParam() : param);
					endpoint.setLabels(ToolUtil.isEmpty(labels) ? module.getLabels() : labels);
					endpoint.setPath(ToolUtil.isEmpty(path) ? module.getPath() : path);
					endpoint.setEnabled(ToolUtil.isEmpty(enabled) ? 1 : Double.valueOf(enabled).intValue());
					endpoint.setSeq(seqUUID);
					this.save(endpoint);

					// 保存 endpoint state
					Integer endpointState = 0;
					// endpoint 停用状态
					if (endpoint.getEnabled() == 0) endpointState = 2;
					EndpointState state = new EndpointState(endpoint.getId(), endpoint.getId(), DateUtil.getUTCTimeByConfigTimeZone(), endpointState);
					endpointStateService.save(state);

					dataSourceTransactionManager.commit(transactionStatus);
					projectNameAndIdMap.put(projectName, projectId);
					moduleNameAndModuleMap.put(moduleName, module);
					successNum++;
				} catch (Exception e) {
					dataSourceTransactionManager.rollback(transactionStatus);
					this.addFailDetail(dataFromExcelRowNum, failDetails, "System", e.getMessage() + "");
					logger.error(String.format("保存 project module endpoint 失败,回滚事务,错误信息:%s", e));
				}
			}
		}
		return successNum;
	}

	private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
		Map failDetail = new HashMap(4);
		failDetail.put("lineNo", i);
		failDetail.put("errorMsg", headerInfo + ": " + msg);
		failDetails.add(failDetail);
	}
	
	@Override
	public void cancelImport(String seq) {
		logger.info("endpoint cancelImport start seq info is {}",seq);
		// endpoint
        List<Endpoint> endpoints = this.list(new QueryWrapper<Endpoint>().lambda().eq(Endpoint::getSeq, seq));
        if (CollectionUtils.isNotEmpty(endpoints)) {
            List<Integer> endpointIds = endpoints.stream().map(Endpoint::getId).collect(Collectors.toList());
            this.deleteEndpointByIds(endpointIds);
        }
        // module
        moduleService.remove(new QueryWrapper<Module>().lambda().eq(Module::getSeq, seq));
        // project
        projectService.remove(new QueryWrapper<Project>().lambda().eq(Project::getSeq, seq));
        
        logger.info("endpoint cancelImport success seq info is {}",seq);
	}

	@Override
	public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
		SysConfigEntity configEntity = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ENDPOINT_EXPORT_HEADER));
		Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);
		Map<String, String> exportHeaderMap;
		/*switch (language) {
			case "en": {
				exportHeaderMap = (Map) map.get("en");
				break;
			}
			case "cn": {
				exportHeaderMap = (Map) map.get("cn");
				break;
			}
			case "ru": {
				exportHeaderMap = (Map) map.get("ru");
				break;
			}
			default: {
				throw new NZException(RCode.ENDPOINT_EXPORT_HEADER_LANGUAGE_ERROR);
			}
		}*/
		if(map.get(language) != null){
			exportHeaderMap = (Map) map.get(language);
		}else{
			throw new NZException(RCode.ENDPOINT_EXPORT_HEADER_LANGUAGE_ERROR);
		}
		SXSSFWorkbook workbook = null;
		try {
			workbook = ExcelUtils.createExcel07(exportHeaderMap, "Endpoint", true);
			String fileName = "EndpointExportTemplate.xlsx";
			response.setContentType("application/excel");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
			workbook.write(response.getOutputStream());
		} catch (IOException e) {
			logger.error("endpoint 模板生成失败，错误信息是：" + e);
		} catch (Exception e) {
			logger.error("获取 endpoint 导入模板失败，表头语言是：" + language + " ，错误信息是：" +e);
		} finally {
			if (workbook != null) {
				IOUtils.closeQuietly(workbook);
			}
		}
	}

	@Override
	public void export(HttpServletResponse response, Map<String, Object> params) {
		IPage<Endpoint> page = new Query<Endpoint>().getPage(params);
		List<Endpoint> list = endpointDao.queryPage(page, params);

		SXSSFWorkbook workbook = null;
		try {
			int rowNum = 0;
			SysConfigEntity configEntity = sysConfigDao.selectOne(new QueryWrapper<SysConfigEntity>().lambda().eq(SysConfigEntity::getParamKey, Constant.SYSCONFIG_KEY_ENDPOINT_EXPORT_HEADER));
			Map map = JSONObject.parseObject(configEntity.getParamValue(), Map.class, Feature.OrderedField);

			// 添加标题行 默认语言 en
			String language = (String) params.get("language");
			language = ToolUtil.isEmpty(language) ? "en" : language;
			Map<String, String> exportHeader = (Map) map.get(language);

			String sheetName = "Endpoints";
			workbook = ExcelUtils.createExcel07(exportHeader, sheetName, false);
			SXSSFSheet sheet = workbook.getSheet(sheetName);
			CellStyle dataStyle = ExcelUtils.createCellStyle(workbook, (short) 13, false);

			// 添加数据
			Row row1;
			for (Endpoint endpoint : list) {
				row1 = sheet.createRow(++rowNum);

				Project project = endpoint.getProject();
				Module module = endpoint.getModule();
				Asset asset = endpoint.getAsset();

				// project name
				row1.createCell(0).setCellValue(project.getName());
				// module name
				row1.createCell(1).setCellValue(module.getName());
				// type
				row1.createCell(2).setCellValue(module.getType().toUpperCase());
				// sn
				row1.createCell(3).setCellValue(asset.getSn());
				// port
				row1.createCell(4).setCellValue(endpoint.getPort());
				// param
				row1.createCell(5).setCellValue(endpoint.getParam());
				// labels
				row1.createCell(6).setCellValue(endpoint.getLabels());
				// path
				row1.createCell(7).setCellValue(endpoint.getPath());
				// host
				row1.createCell(8).setCellValue(asset.getHost());
				// enabled
				row1.createCell(9).setCellValue(endpoint.getEnabled());

				// 设置单元格样式
				ExcelUtils.setDataCellStyle(dataStyle, row1, 9);
			}

			// 响应为流
			String fileName = "EndpointsExport.xlsx"; // 默认名称
			response.setContentType("application/excel");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
			workbook.write(response.getOutputStream());
		} catch (IOException e) {
			logger.error(e);
		} catch (Exception e) {
			logger.error(e);
		} finally {
			if (workbook != null) {
				IOUtils.closeQuietly(workbook);
			}
		}
	}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEndpointByIds(List<Integer> endpointIdList) {
	    // 删除 endpoint
        this.removeByIds(endpointIdList);

        // 删除 endpoint state
        endpointStateService.removeByIds(endpointIdList);

        // 删除 alert
	    alertMessageDao.deleteByEndpointIds(endpointIdList);
        try {
            // 同时删除 redis  endpoint_prom 中 endpointId 对应的内容
            String[] strArray = endpointIdList.stream().map(s -> String.valueOf(s)).toArray(String[]::new);
            redisTemplate.opsForHash().delete(Constant.REDIS_KEY_ENDPOINT_PROM, strArray);
        } catch (Exception e) {
            logger.error(String.format("endpoint删除时，同步删除redis信息失败，key:endpoint_prom，失败参数：%s,错误信息是:%s", endpointIdList, e));
        }
    }

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveOrUpdateBatchEndpointList(List<Endpoint> endpointList) {
		// 校验
		this.validateParam(endpointList);

		List<Module> moduleList = moduleService.list();
		Map<Integer, Module> moduleIdAndModuleInfoMap = new HashMap();
		for (Module module : moduleList) {
			moduleIdAndModuleInfoMap.put(module.getId(), module);
		}

		List<Asset> assetList = assetService.list();
		Map<Integer, String> assetIdAndHostMap = assetList.stream().collect(Collectors.toMap(Asset::getId, Asset::getHost));

		// endpoint 状态
		List<EndpointState> endpointStateList = new ArrayList<>();
		Module module;
		Date now = DateUtil.getUTCTimeByConfigTimeZone();
		for (Endpoint endpoint : endpointList) {
			module = moduleIdAndModuleInfoMap.get(endpoint.getModuleId());
			if (ToolUtil.isEmpty(endpoint.getPort())) {
				endpoint.setPort(module.getPort());
			}
			if (ToolUtil.isEmpty(endpoint.getParam())) {
				endpoint.setParam(module.getParam());
			}
			if (ToolUtil.isEmpty(endpoint.getLabels())) {
				endpoint.setLabels(module.getLabels());
			}
			if (ToolUtil.isEmpty(endpoint.getPath())) {
				endpoint.setPath(module.getPath());
			}
			if (ToolUtil.isEmpty(endpoint.getHost())) {
				endpoint.setHost(assetIdAndHostMap.get(endpoint.getAssetId()));
			}
			if (ToolUtil.isEmpty(endpoint.getEnabled())) {
				endpoint.setEnabled(1);
			}
			// endpoint state 新增状态时 默认为 0
			Integer endpointState = 0;
			// endpoint 停用状态
			if (endpoint.getEnabled() == 0) endpointState = 2;

			// 保存 endpoint
			this.saveOrUpdate(endpoint);

			// endpointState
			endpointStateList.add(new EndpointState(endpoint.getId(), endpoint.getId(), now, endpointState));
		}
		// 保存 endpointstate
		endpointStateService.saveOrUpdateBatch(endpointStateList);
	}
}
