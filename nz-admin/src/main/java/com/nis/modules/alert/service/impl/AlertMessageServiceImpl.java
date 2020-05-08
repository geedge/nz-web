package com.nis.modules.alert.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.nis.common.exception.NZException;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertMessageDao;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.AlertMessageWithExport;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
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
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service("alertMessageService")
public class AlertMessageServiceImpl extends ServiceImpl<AlertMessageDao, AlertMessageEntity> implements AlertMessageService {
	private Logger logger= LoggerFactory.getLogger(AlertMessageServiceImpl.class);

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

    @Override
    public PageUtils queryPage(Map<String, Object> params)  {
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
		    logger.error("时间转换异常,参数是 {}", params, e);
	    }

    	Integer searchId=resolveParamToInteger(params,"id");
    	params.put("id",searchId);

		Integer projectId=resolveParamToInteger(params,"projectId");
		params.put("projectId",projectId);

		Integer moduleId=resolveParamToInteger(params,"moduleId");
		params.put("moduleId",moduleId);

		Integer endpointId=resolveParamToInteger(params,"endpointId");
		params.put("endpointId",endpointId);

		Integer assetId=resolveParamToInteger(params,"assetId");
		params.put("assetId",assetId);

		Integer dcId=resolveParamToInteger(params,"dcId");
		params.put("dcId",dcId);

    	Integer searchState=resolveParamToInteger(params,"state");
		params.put("state",searchState);

		Integer searchRuleId=resolveParamToInteger(params,"ruleId");
		params.put("ruleId",searchRuleId);

		IPage<AlertMessageEntity> page=this.page(new Query<AlertMessageEntity>().getPage(params));

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

		List<Project> projectList = projectService.list();
		Map<Integer,Project> projectMap= new HashMap<>();
		for (Project project:projectList) {
			projectMap.put(project.getId(), project);
		}

		List<Module> moduleList = moduleService.list();
		Map<Integer,Module> moduleMap= new HashMap<>();
		for (Module module:moduleList) {
			moduleMap.put(module.getId(),module);
		}

		List<Endpoint> endpointList = endpointService.list();
		Map<Integer,Endpoint> endpointMap= new HashMap<>();
		for (Endpoint endpoint: endpointList) {
			endpointMap.put(endpoint.getId(),endpoint);
		}

		List<Asset> assetList = assetService.list();
		Map<Integer,Asset> assetMap= new HashMap<>();
		for (Asset asset:assetList) {
			assetMap.put(asset.getId(), asset);
		}

		Integer projectId ,moduleId ,endpointId, assetId, dcId;
		for (AlertMessageEntity entity:alertMessages) {
			projectId = entity.getProjectId();
			if(projectId != null){
				entity.setProject(projectMap.get(projectId));
			}
			moduleId = entity.getModuleId();
			if(moduleId != null){
				entity.setModule(moduleMap.get(moduleId));
			}
			endpointId = entity.getEndpointId();
			if(endpointId != null){
				entity.setEndpoint(endpointMap.get(endpointId));
			}
			assetId = entity.getAssetId();
			if(assetId != null){
				entity.setAsset(assetMap.get(assetId));
			}
			dcId = entity.getDcId();
			if(dcId != null){
				entity.setDc(idcMap.get(dcId));
			}
		}
	}

    @Override
    public void saveBatch(AlertMessageReceiveDTO[] receiveData)  {
    	logger.debug("receive promethues alert message {}",receiveData==null?null:Arrays.asList(receiveData).toString());
    	List<AlertMessageEntity> entities = new ArrayList<>();

	    HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();

	    redisTemplate.executePipelined(new RedisCallback<Object>() {
			@Override
			public Object doInRedis(RedisConnection redis) throws DataAccessException {

				for (AlertMessageReceiveDTO dto : receiveData) {
					try{
						getReceiveLabels(dto);
                        AlertMessageEntity entity = AlertMessageReceiveDTO.toAlertMessageEntity(dto);

						// 新增-  判断缓存中是否存在改告警信息 不存在则保存数据库
						Object cacheAlert = hashOperations.get(Constant.REDIS_KEY_ALERTMESSAGE, entity.getHashKey());
						if(cacheAlert == null){
                            entities.add(entity);
						}

						HashMap<String, Object> map = new HashMap<>();
						map.put("id",entity.getRuleId());
						map.put("time", System.currentTimeMillis());
						// 存在更新时间，不存在set
						redis.hSet(Constant.REDIS_KEY_ALERTMESSAGE.getBytes(),entity.getHashKey().getBytes(),JSON.toJSONString(map).getBytes());
					}catch (Exception e){
						// 告警信息解析失败
						throw new NZException(RCode.ALERTMSG_SAVE_ERROR);
					}
				}
				return null;
			}
		});
		logger.debug("数据格式化完成 : {}",entities.toString());
    	if (!CollectionUtils.isEmpty(entities)) {
	    	this.saveBatch(entities);
	    	String emailEnable = configService.getValue("email_enable");
	    	//邮件通知
	    	if (Constant.SWITCH_OPEN.equals(emailEnable)) {
	    		redisTemplate.opsForList().leftPush("alert_message_list",entities);
	    	}
    	}
    }

    private AlertMessageReceiveDTO getReceiveLabels(AlertMessageReceiveDTO dto){
		Map receiveLabels = dto.getLabels();
		Map labelIds = new HashMap();
		Integer assertId=null,moduleId=null,projectId=null,endpointId=null,dcId=null ;
		// 有endpointId ,即可查出所有
		if(ToolUtil.isNotEmpty(receiveLabels.get("endpoint"))){
			endpointId = Integer.valueOf(receiveLabels.get("endpoint").toString());
			Endpoint endpoint = endpointService.getById(endpointId);
			assertId = endpoint.getAssetId();
			moduleId = endpoint.getModuleId();
			Module module = moduleService.getById(moduleId);
			projectId = module.getProjectId();
			Asset asset = assetService.getById(assertId);
			dcId = asset.getIdcId();
		}else{
			if(ToolUtil.isNotEmpty(receiveLabels.get("module"))){
				Module module = moduleService.getOne(new LambdaQueryWrapper<Module>().eq(Module::getName, receiveLabels.get("module").toString()));
				moduleId = module.getId();
				projectId = module.getProjectId();
			}
			if(ToolUtil.isNotEmpty(receiveLabels.get("project"))&&ToolUtil.isEmpty(receiveLabels.get("module"))){
				Project project = projectService.getOne(new LambdaQueryWrapper<Project>().eq(Project::getName,receiveLabels.get("project").toString()));
				projectId = project.getId();
			}
			if(ToolUtil.isNotEmpty(receiveLabels.get("asset"))){
				Asset asset = assetService.getOne(new LambdaQueryWrapper<Asset>().eq(Asset::getHost,receiveLabels.get("asset").toString()));
				assertId = asset.getId();
				dcId = asset.getIdcId();
			}
			if(ToolUtil.isNotEmpty(receiveLabels.get("datacenter"))&&ToolUtil.isEmpty(receiveLabels.get("asset"))){
				Idc dc = idcService.getOne(new LambdaQueryWrapper<Idc>().eq(Idc::getName,receiveLabels.get("datacenter").toString()));
				dcId = dc.getId();
			}
		}
		labelIds.put("endpoint",endpointId);
		labelIds.put("project",projectId);
		labelIds.put("assetId",assertId);
		labelIds.put("module",moduleId);
		labelIds.put("datacenter",dcId);
		dto.setLabelIds(labelIds);

    	return dto;
	}

	@Override
	public boolean updateByIds(AlertMessageEntity alertMessage) {
		String ids = alertMessage.getIds();
		Object[] splitedIds = ids.split(",");
		baseMapper.update(null, new UpdateWrapper<AlertMessageEntity>().in("id", splitedIds).set("state", alertMessage.getState()));
		return true;
	}

	private Integer resolveParamToInteger(Map<String,Object> params,String paramName){
		Integer param=null;
		try {
			if(params.get(paramName)!=null&&StringUtils.isNotBlank(params.get(paramName).toString())){
				param=Integer.parseInt(params.get(paramName).toString());
			}
		}catch (NumberFormatException e){}

		return param;
	}

	@Override
	public List<MetricsDto> queryAlertMessageStatisticsInfo() {
		// TODO Auto-generated method stub
		return alertMessageDao.selectAlertMessageStatisticsInfo();
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
		if(ToolUtil.isNotEmpty(params.get("alertName"))) {
			List<AlertRuleEntity> ruleEntityList = alertRuleService.list(
					new LambdaQueryWrapper<AlertRuleEntity>().like(ToolUtil.isNotEmpty(params.get("alertName")), AlertRuleEntity::getAlertName, params.get("alertName").toString()));
			List<Integer> ruleIds = ruleEntityList.stream().map(AlertRuleEntity::getId).distinct().collect(Collectors.toList());
			params.put("ruleIds", ruleIds);
		}
		//根据页面参数查询需要导出的alert_message数据
		int total ;
		int pageNo = Integer.parseInt(params.get("pageNo").toString());
		int pageSize = Integer.parseInt(params.get("pageSize").toString());

		//Excel2003版最大行数是65536，Excel2007最大行数是1048576
		//Excel2003版最大列数是256，Excel2007最大列数是16384
		//使用SXSSFWorkbook避免Excel数据超出65536行时报错
		SXSSFWorkbook  workbook = new SXSSFWorkbook();
		Sheet sheet = workbook.createSheet("AlertMessages");
		// 创建样式对象
		CellStyle style = workbook.createCellStyle();
		style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);// 垂直
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER);// 水平

		int rowNum = 0;
		Row row = sheet.createRow(rowNum);
		Cell cell;
		// 添加标题行
		// 获取导出的语言选择
		String language = ToolUtil.isEmpty(params.get("language"))?"en":params.get("language").toString();
		// 从sys_config获取表头和查询数量限制
		String [] keys  =  {"alert_export_header","export_pagesize"};
		List<SysConfigEntity> paramValues = this.configService.list(
				new LambdaUpdateWrapper<SysConfigEntity>().in(SysConfigEntity::getParamKey,keys));
		// 得到表头
		Map map = (Map)(JSON.parseObject(paramValues.get(0).getParamValue(), Feature.OrderedField).get(language));
		Collection values =  map.values();
		List<String> valueList = new ArrayList<>(values.size());
		values.stream().forEach(value-> valueList.add(value.toString()));
		// 筛选表头并添加表头至excel
		for (int i = 0; i < valueList.size(); i++) {
			// 创建列、单元格
			cell = row.createCell(i);
			cell.setCellValue(valueList.get(i));
			cell.setCellStyle(style);
		}

		// 添加数据
		if("-1".equals(params.get("pageSize").toString())){
			total = this.count();
			int exportPagesize = Integer.parseInt(paramValues.get(1).getParamValue());
			pageNo = (total % exportPagesize == 0) ? (total / exportPagesize) : (total / exportPagesize + 1);
			for(int i =1;i<=pageNo;i++){
				int start = (i-1)*exportPagesize;
				int end = exportPagesize;
				params.put("pageNo",start);
				params.put("pageSize",end);
				List<AlertMessageWithExport> list =this.baseMapper.selectWithExport(params);
				rowNum = fill(list,rowNum,sheet);
			}
		}else {  //部分导出
			int start = (pageNo-1)*pageSize;
			int end = pageSize;
			params.put("pageNo",start);
			params.put("pageSize",end);
			List<AlertMessageWithExport> list = this.baseMapper.selectWithExport(params);
			fill(list,rowNum,sheet);
		}

		try {
			// 响应为流
			String fileName = "AlertMessagesExport.xlsx"; // 默认名称
			response.setContentType("application/excel");
			response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
			workbook.write(response.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateAlertStateByIds(Map<String, Object> params) {
		// 当前时间
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String formatDate = df.format(new Date());
		// 当前用户
		SysUserEntity user = (SysUserEntity) SecurityUtils.getSubject().getPrincipal();

		String remark = (String) params.get("remark");
		remark = formatDate + "  " + user.getUsername() + "  " + remark;
		params.put("remark", remark);

		String ids = (String) params.get("ids");
		List<String> idList = Arrays.asList(ids.split(","));

		alertMessageDao.updateAlertStateByIds(idList, params);
	}

	/**
	 * 填充表格数据
	 */
	private static int fill(List<AlertMessageWithExport> list,int rowNum,Sheet sheet){
		for (AlertMessageWithExport alertMessageWithExport : list) {
			Row r = sheet.createRow(++rowNum);
			Integer id = alertMessageWithExport.getId();
			String alertName = alertMessageWithExport.getAlertName();
			String datacenter = alertMessageWithExport.getDatacenter();
			String project = alertMessageWithExport.getProject();
			String module = alertMessageWithExport.getModule();
			String endpoint = alertMessageWithExport.getEndpoint();
			String asset = alertMessageWithExport.getAsset();
			String labels = alertMessageWithExport.getLabels();
			String severity = alertMessageWithExport.getSeverity();
			String summary = alertMessageWithExport.getSummary();
			String description = alertMessageWithExport.getDescription();
			String startAt = ToolUtil.isNotEmpty(alertMessageWithExport.getStartAt()) ? alertMessageWithExport.getStartAt() : "";
			String endAt = ToolUtil.isNotEmpty(alertMessageWithExport.getEndAt()) ? alertMessageWithExport.getEndAt() : "";
			String state = alertMessageWithExport.getState() == 1 ? "pending" : "expired";

			r.createCell(0).setCellValue(id);
			r.createCell(1).setCellValue(alertName);
			r.createCell(2).setCellValue(datacenter);
			r.createCell(3).setCellValue(project);
			r.createCell(4).setCellValue(module);
			r.createCell(5).setCellValue(endpoint);
			r.createCell(6).setCellValue(asset);
			r.createCell(7).setCellValue(labels);
			r.createCell(8).setCellValue(severity);
			r.createCell(9).setCellValue(summary);
			r.createCell(10).setCellValue(description);
			r.createCell(11).setCellValue(startAt);
			r.createCell(12).setCellValue(endAt);
			r.createCell(13).setCellValue(state);
		}
		return rowNum;
	}




}
