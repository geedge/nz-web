package com.nis.modules.endpoint.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ExcelUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.impl.AssetServiceImpl;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.entity.StateInfo;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.promserver.entity.PromApi;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverProxyService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.entity.SysDictEntity;

@Service("endpointService")
public class EndpointServiceImpl  extends ServiceImpl<EndpointDao, Endpoint> implements EndpointService {
	
	private static Logger logger = LoggerFactory.getLogger(EndpointServiceImpl.class);
	@Autowired
	private EndpointDao endpointDao;
	@Autowired
	private PromserverProxyService promserverProxyService;
	@Autowired
    private PromApi promApi;
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
	
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage<Endpoint> page = new Query<Endpoint>().getPage(params);
		List<Endpoint> datas = endpointDao.queryPage(page, params);
		//获取相关lastUpdate以及state数据信息
		String api = promApi.getPromApis().get("query");
		try {
			if(datas!=null&&datas.size()>0) {
				Map resultMap=new HashMap();
				Map map =new HashMap();
				map.put("query", "up");
				JSONObject promeResult = promserverProxyService.query(api, map);
				JSONObject promeObject = (JSONObject) promeResult.get("data");
				JSONArray str = (JSONArray) promeObject.get("result");
				if(str!=null&&str.size()>0) {
					for(int i=0;i<str.size();i++) {
						Map resultPromeMap = (Map) str.get(i);
						JSONObject metric = (JSONObject) resultPromeMap.get("metric");
						String jobName = (String) metric.get("job");
						resultMap.put(jobName, resultPromeMap.get("value"));
					}
				}
				for(Endpoint data: datas) {
					Promserver promserver = data.getPromserver();
					Integer promStatus = promserver.getStatus();
					StateInfo stateInfo =new StateInfo();
					String jn="ed_"+data.getId();
					JSONArray valueList = (JSONArray) resultMap.get(jn);
					if(valueList!=null) {
						BigDecimal time = (BigDecimal) valueList.get(0);
						long timeStamp = time.longValue();
						data.setLastUpdate(timeStamp);
						String stateValue=(String)valueList.get(1);
						Integer state = Integer.valueOf(stateValue);
						data.setState(state);
						if(state.equals(0)) {
							// promtheus 不可用
							stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_UNUSE.getCode());
							stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_UNUSE.getMsg());
						}else {
							stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_ON_USE.getCode());
							stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_ON_USE.getMsg());
						}
					}else {
						data.setState(0);
						stateInfo.setCode(RCode.ENDPOINT_PROMETHEUS_AVAILABLE.getCode());
						stateInfo.setMsg(RCode.ENDPOINT_PROMETHEUS_AVAILABLE.getMsg());
					}
					data.setStateInfo(stateInfo);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        page.setRecords(datas);
        return new PageUtils(page);
	}

	@Override
	public void validateParam(Endpoint endpointEntity) {
		ValidateUtils.is(endpointEntity.getModuleId()).notNull(RCode.ENDPOINT_MODULEID_ISNULL);
		ValidateUtils.is(endpointEntity.getAssetId()).notNull(RCode.ENDPOINT_ASSETID_ISNULL);
		ValidateUtils.is(endpointEntity.getPort()).notNull(RCode.ENDPOINT_PORT_ISNULL);
		ValidateUtils.is(endpointEntity.getPath()).notNull(RCode.ENDPOINT_PATH_ISNULL);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeByModuleIds(List moduleIds) {
		endpointDao.deleteByModuleIds(moduleIds);
	}

	@Override
	public List<MetricsDto> queryEndpointStatisticsInfoByModel() {
		return endpointDao.selectEndpointStatisticsInfoByModel();
	}

	@Override
	public Map importEndpoint(MultipartFile endpointExcelFile, String uuid) {
		logger.info("importEndpoint param endpointExcelFileName is {}",endpointExcelFile.getOriginalFilename());
		logger.info("importEndpoint param uuid is {}",uuid);
		String originalFilename = endpointExcelFile.getOriginalFilename();
		
		//防止导入数据过多 查询关联数据是否存在 提前将需要的数据查询放置到集合中 减少数据库交互
		List<Project> projects = projectService.list();
		List<Module> modules = moduleService.list();
		List<Asset> assets = assetService.list();
		
		Map projectMap = new HashMap();
		if(projects!=null&&projects.size()>0) {
			for(Project project : projects) {
				projectMap.put(project.getName(), project);
			}
		}
		
		Map moduleMap = new HashMap();
		if(modules!=null&&modules.size()>0) {
			for(Module module : modules) {
				moduleMap.put(module.getName(), module);
			}
		}
		
		Map assetMap = new HashMap();
		if(assets!=null&&assets.size()>0) {
			for(Asset asset : assets) {
				assetMap.put(asset.getSn(),asset);
			}
		}
        // 最终入库集合
        List<Endpoint> endpoints = new ArrayList<>();
        
        Map info = new HashMap();
        Workbook work = null;
        try {
            work = ExcelUtils.getWork(originalFilename, endpointExcelFile.getInputStream());
            // 默认得到第一个 sheet
            Sheet sheet = work.getSheetAt(0);
//            Sheet sheet = work.getSheet("assets");
            // 获得当前sheet的开始行
            int firstRowNum = sheet.getFirstRowNum();
            // 获得当前sheet的结束行
            int maxRowNum = sheet.getLastRowNum();

            // 校验表头是否与模板一致
            List<Map> mapList = this.validateExcelHeader(sheet, firstRowNum);
            if (CollectionUtils.isNotEmpty(mapList)) {
                info.put("seq", uuid);
                info.put("totalNum", 0);
                info.put("successNum", 0);
                info.put("failNum", 0);
                info.put("failDetail", mapList);
                return info;
            }
            
            Row row;

            // 返回导出错误信息
            List<Map> failDetails = new ArrayList<>();
            Map failDetail;
            // 成功条数
            int successNum = 0;

            String project,module,sn,port,param,path,host;
            Integer accountAuthType;

            File sshKey;
            InputStream sshKeyInput;
            // 第一行是表头 所以从第二行开始
            for (int i = firstRowNum + 1; i <= maxRowNum; i++) {
                boolean saveFlag = true;
                Endpoint endpoint = new Endpoint();
                row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                // 获取实际值为projectName
                project = ExcelUtils.getCellValue(row.getCell(0));
                Project projectObject = (Project) projectMap.get(project);
                if (StringUtils.isEmpty(project)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.PROJECT_NAME_ISNULL);
                    failDetails.add(failDetail);
                }
                // 获取实际值为moduleName
                module = ExcelUtils.getCellValue(row.getCell(1));
                Module moduleObject = (Module) moduleMap.get(module);
                if (StringUtils.isEmpty(module)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.MODULE_NAME_ISNULL);
                    failDetails.add(failDetail);
                }
                // 资产sn信息
                sn = ExcelUtils.getCellValue(row.getCell(2));
                if (StringUtils.isEmpty(sn)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.ASSET_SN_ISNULL);
                    failDetails.add(failDetail);
                }else {
                	Asset assetObject = (Asset) assetMap.get(sn);
                	if(assetObject==null) {
                		saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ASSET_NOT_EXIST);
                        failDetails.add(failDetail);
                	}else {
                		endpoint.setAsset(assetObject);
                		endpoint.setAssetId(assetObject.getId());
                	}
                }
                
                // 端口号
                port = ExcelUtils.getCellValue(row.getCell(3));
                // 判断如果参数port以及module内port均为空则导入失败
                if (StringUtils.isEmpty(port)&&StringUtils.isEmpty(module)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.MODULE_PORT_ISNULL);
                    failDetails.add(failDetail);
                }else if(StringUtils.isEmpty(port)&&(moduleObject!=null)&&(moduleObject.getPort()==null)) {
                	 saveFlag = false;
                     failDetail = new HashMap();
                     failDetail.put("lineNo", i);
                     failDetail.put("errorMsg", RCode.MODULE_PORT_ISNULL);
                     failDetails.add(failDetail);
                }else if(StringUtils.isNotEmpty(port)) {
                	// 检查端口是否格式正确
                	boolean flag = CommonUtils.checkPort(port);
                	if(!flag) {
                		 saveFlag = false;
                         failDetail = new HashMap();
                         failDetail.put("lineNo", i);
                         failDetail.put("errorMsg", RCode.MODULE_PORT_ERROR);
                         failDetails.add(failDetail);
                	}
                }
               
                param = ExcelUtils.getCellValue(row.getCell(4));
                if (StringUtils.isEmpty(param)) {
                    endpoint.setParam(param);
                } 

                // path
                path = ExcelUtils.getCellValue(row.getCell(5));
                if (StringUtils.isEmpty(path)&&StringUtils.isEmpty(module)) {
                    saveFlag = false;
                    failDetail = new HashMap();
                    failDetail.put("lineNo", i);
                    failDetail.put("errorMsg", RCode.MODULE_PATH_ISNULL);
                    failDetails.add(failDetail);
                }else if(StringUtils.isEmpty(path)&&(moduleObject!=null)&&(StringUtils.isEmpty(moduleObject.getPath()))) {
                	 saveFlag = false;
                     failDetail = new HashMap();
                     failDetail.put("lineNo", i);
                     failDetail.put("errorMsg", RCode.MODULE_PATH_ISNULL);
                     failDetails.add(failDetail);
                }

                // host
                host = ExcelUtils.getCellValue(row.getCell(6));
                if (StringUtils.isNotEmpty(host)) {
                	// 不为空的情况下 校验ip正确性
                	if(CommonUtils.checkIp(host)) {
                		endpoint.setHost(host);
                	}else {
                		saveFlag = false;
                        failDetail = new HashMap();
                        failDetail.put("lineNo", i);
                        failDetail.put("errorMsg", RCode.ENDPOINT_HOST_ERROR);
                        failDetails.add(failDetail);
                	}
                    
                }

                // 前面没有报错
                if (saveFlag) {
                	boolean projectFlag=false;
                	boolean moduleFlag=false;
                	endpoint.setSeq(uuid);
                	// 前述操作均正常的情况下 判断是否需要新增关联project 以及 module信息
                	if(projectObject==null) {
                		projectObject=new Project();
                		projectObject.setName(project);
                		projectObject.setSeq(uuid);
                		projectObject.setBuildIn(0);
                		projectFlag=true;
                		
                	}
                	if(moduleObject==null) {
                		moduleObject=new Module();
                		moduleObject.setBuildIn(0);
                		moduleObject.setName(module);
                		moduleObject.setParam(param);
                		moduleObject.setPort(Integer.valueOf(port));
                		moduleObject.setPath(path);
                		moduleObject.setSeq(uuid);
                		moduleFlag=true;
                		
                	}
                	if(projectFlag||moduleFlag) {
                		// 关联关系事务处理
                		// 手动开启事务
                        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
                		try {
                			this.importTransactionHandle(projectObject, moduleObject, endpoint);
                			if(projectFlag) {
                				projectMap.put(project, projectObject);
                			}else {
                				moduleMap.put(module,moduleObject);
                			}
                			// 手动提交事务
                            dataSourceTransactionManager.commit(transactionStatus);
                			successNum++;
                		}catch(Exception e) {
                			 logger.error(e.getMessage());
                			 // 手动回滚事务
                             dataSourceTransactionManager.rollback(transactionStatus);
                			 failDetail = new HashMap();
                             failDetail.put("lineNo", i);
                             failDetail.put("errorMsg", e.getMessage());
                             failDetails.add(failDetail);
                		}
                	}else {
                		endpoint.setProject(projectObject);
                    	endpoint.setPort(moduleObject.getPort());
                    	endpoint.setPath(moduleObject.getPath());
                    	endpoint.setModule(moduleObject);
                		endpoint.setModuleId(moduleObject.getId());
                    	endpoints.add(endpoint);
                    	successNum++;
                	}      
                	
                }
                    
            }

            // 批量入库 asset
            this.saveBatch(endpoints);

            int totalNum = maxRowNum - firstRowNum;
            info.put("seq", uuid);
            info.put("totalNum", totalNum);
            info.put("successNum", successNum);
            info.put("failNum", totalNum - successNum);
            info.put("failDetail", failDetails);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                work.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return info;
	}

	private List<Map> validateExcelHeader(Sheet sheet, int firstRowNum) {
        Row row = sheet.getRow(firstRowNum);

        List<Map> mapList = new ArrayList<>();
        Map map;

        String[] str = {"Project", "Module", "Sn", "Port", "Param", "Path", "Host"};
        List<String> titles = Arrays.asList(str);

        String titleValue, title;
        for (int i = 0; i < titles.size(); i++) {
            titleValue = ExcelUtils.getCellValue(row.getCell(i)).toLowerCase();
            title = titles.get(i);
            if (!titleValue.equalsIgnoreCase(title)) {
                map = new HashMap();
                map.put("lineNo", firstRowNum);
                map.put("errorMsg", "第" + (i + 1) + "列表头为 " + titleValue + " ,与模板不符，请您核实。谢谢！");
                mapList.add(map);
            }
        }

        return mapList;
    }
	
	
	public void importTransactionHandle(Project project,Module module,Endpoint endpoint) {
		projectService.save(project);
		module.setProjectId(project.getId());
		moduleService.save(module);
		this.save(endpoint);
	}
	
	@Override
	public void cancelImport(String seq) {
		logger.info("endpoint cancelImport start seq info is {}",seq);
		// endpoint
        this.remove(new QueryWrapper<Endpoint>().eq("seq", seq));
        // module
        moduleService.remove(new QueryWrapper<Module>().eq("seq", seq));
        // project
        projectService.remove(new QueryWrapper<Project>().eq("seq", seq));
        
        logger.info("endpoint cancelImport success seq info is {}",seq);
	}

}
