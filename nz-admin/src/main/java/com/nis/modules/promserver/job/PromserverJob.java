package com.nis.modules.promserver.job;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.utils.HttpClientUtils;
import com.nis.modules.sys.service.SysConfigService;


public class PromserverJob extends QuartzJobBean {
	private Logger logger= LoggerFactory.getLogger(PromserverJob.class);
	private final static String HTTP_HEADER_PATH="http://";
	private final static String COLON=":";
	private final static String HEALTHY_PATH="/-/healthy"; 
	private final static String CONFAGENT_HEALTHY_PATH="/healthy";
	@Autowired
	private PromserverService promserverService;
	@Autowired
	private SysConfigService sysConfigService;
	
	@SuppressWarnings("unchecked")
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		Map params=new HashMap();
		//查询内置confagent配置信息
		String confagentPort=sysConfigService.getValue(Constant.CONFAGENT_PORT);
		String confagentPath=sysConfigService.getValue(Constant.CONFAGENT_PATH);
		//循环查询所有promserver信息
		PageUtils datas = promserverService.queryPage(params);
		List<Promserver> promservers = (List<Promserver>) datas.getList();
		logger.info("all promserver info {}",JSON.toJSON(promservers));
		if(promservers!=null&&promservers.size()>0) {
			for(Promserver promserver:promservers) {
				String host = promserver.getHost();
				Integer port = promserver.getPort();
				String req=HTTP_HEADER_PATH+host+COLON+port+HEALTHY_PATH;
				Integer result = requestPromAPI(req);
				String confagentReq = HTTP_HEADER_PATH+host+COLON+confagentPort+"/"+confagentPath+CONFAGENT_HEALTHY_PATH;
//				String confagentReq = "http://192.168.41.87:8989/nz-confagent/healthy";
				Integer confagentResult = requestPromAPI(confagentReq);
				logger.info("current promserver info : {}",JSON.toJSON(promserver));
				logger.info("prometheus healthy response :{}",JSON.toJSON(result));
				logger.info("confagent healthy response :{}",JSON.toJSON(confagentResult));
				if(result!=null&&result.equals(200)&&confagentResult!=null&&confagentResult.equals(200)) {
					promserver.setStatus(1);
					promserver.setCheckTime(new Date());
				}else if(result!=null&&result.equals(200)){
					//confagent 不可用但是promserver可用
					promserver.setStatus(-1);
					promserver.setCheckTime(new Date());
				}else if(confagentResult!=null&&confagentResult.equals(200)) {
					//promserver不可用 但是confagent可用
					promserver.setStatus(0);
				}else {
					//都不可用
					promserver.setStatus(-2);
				}
			}
			promserverService.updateBatchById(promservers);
		}
	}

	public Integer requestPromAPI(String url){
        Integer code=null;
        try{
            code = HttpClientUtils.httpGetForCode(url);
            logger.info("request Prometheus Server api with request:[{}]",url);

        }catch (Exception e){
            logger.error("request Prometheus Server query api with error {}",e);
            // throw new NZException(RCode.PROMSERVER_REQUEST_FAILD);
        }
        return code;
    }
}
