package com.nis.modules.endpoint.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.endpoint.dao.EndpointDao;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.dao.ModuleDao;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.promserver.entity.PromApi;
import com.nis.modules.promserver.service.PromserverProxyService;

@Service("endpointService")
public class EndpointServiceImpl  extends ServiceImpl<EndpointDao, Endpoint> implements EndpointService {
	
	@Autowired
	private EndpointDao endpointDao;
	@Autowired
	private PromserverProxyService promserverProxyService;
	@Autowired
    private PromApi promApi;
	
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
				for(Endpoint data:datas) {
					String jn="ed_"+data.getId();
					JSONArray valueList = (JSONArray) resultMap.get(jn);
					if(valueList!=null) {
						BigDecimal time = (BigDecimal) valueList.get(0);
						long timeStamp = time.longValue();
						data.setLastUpdate(timeStamp);
						String stateValue=(String)valueList.get(1);
						Integer state = Integer.valueOf(stateValue);
						data.setState(state);
					}
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

}
