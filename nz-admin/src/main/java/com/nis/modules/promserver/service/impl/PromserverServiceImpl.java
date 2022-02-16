package com.nis.modules.promserver.service.impl;

import cn.hutool.core.net.URLEncoder;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlPath;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("promserverService")
public class PromserverServiceImpl extends ServiceImpl<PromserverDao, Promserver> implements PromserverService{

	private Log log = Log.get();

	@Autowired
	private DcService dcService;

	@Autowired
	private PromserverDao promserverDao;
	@Autowired
	private SysConfigService configService;
	@Autowired
	private RestTemplate restTemplate;
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String ids = (String) params.get("ids");
		if (StrUtil.isNotBlank(ids)) {
			params.put("ids", Arrays.asList(ids.split(",")));
		}
		String dcIds = (String) params.get("dcIds");
		if (StrUtil.isNotBlank(dcIds)) {
			params.put("dcIds", Arrays.asList(dcIds.split(",")));
		}
		String statuss = (String) params.get("statuses");
		if (StrUtil.isNotBlank(statuss)) {
			params.put("statuses", Arrays.asList(statuss.split(",")));
		}

		IPage page = new Query(Promserver.class).getPage(params);
		List<Promserver> promserverList = promserverDao.queryList(page, params);

		page.setRecords(promserverList);
		return new PageUtils(page);
	}


	public void validateParam(Promserver promserverEntity) {
		// host port type 唯一
		List<Promserver> pList = this.list(new LambdaQueryWrapper<Promserver>()
				.eq(Promserver::getHost, promserverEntity.getHost())
				.eq(Promserver::getPort, promserverEntity.getPort())
				.eq(Promserver::getType, promserverEntity.getType()));

		boolean flag = false;
		if ((pList == null || pList.size() == 0) || (pList != null && pList.size() == 1 && pList.get(0).getId().equals(promserverEntity.getId()))) {
			flag = true;
		}

		if (!flag) {
			throw new NZException(RCode.PROMSERVER_EXIST_ERROR);
		}

		Integer type = promserverEntity.getType();
		String host = promserverEntity.getHost();
		Integer port = promserverEntity.getPort();
		ValidateUtils.is(promserverEntity.getDcId()).notNull(RCode.PROMSERVER_DCID_ISNULL)
				.and(host).notNull(RCode.PROMSERVER_HOST_ISNULL).maxLength(64)
				.and(type).notNull(RCode.PROMSERVER_TYPE_ISNULL)
				.and(port).notNull(RCode.PROMSERVER_PORT_ISNULL).maxLength(10)
				.and(promserverEntity.getToken()).notNull(RCode.PROMSERVER_TOKEN_ISNULL).maxLength(64)
		        .and(promserverEntity.getName()).notNull(RCode.PROMSERVER_NAME_ISNULL).maxLength(256);

		// 校验ip是否合法
		boolean ipFlag = CommonUtils.checkIp(host);
		if (!ipFlag) {
			throw new NZException(RCode.PROMSERVER_HOST_ERROR);
		}
		// 校验port是否合法
		boolean portFlag = CommonUtils.checkPort(port.toString());
		if (!portFlag) {
			throw new NZException(RCode.PROMSERVER_PORT_ERROR);
		}

		// dc 存在校验
		Dc dc = dcService.getById(promserverEntity.getDcId());
		ValidateUtils.is(dc).notNull(RCode.DC_NOT_EXISTS);

		// 校验 type 合法性
		Constant.PromserverType[] values = Constant.PromserverType.values();
		List<Integer> list = Arrays.asList(values).stream().map(Constant.PromserverType::getValue).collect(Collectors.toList());
		if (!list.contains(type)) {
			throw new NZException(RCode.PROMSERVER_TYPE_INVALIDE);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Promserver getOneAvaliableGlobalAgent() {
		Integer type = Constant.PromserverType.GLOBAL.getValue();
		//按照id 排序，暂时 使用相对固定的 健康 global
		return this.getOne(new QueryWrapper<Promserver>().lambda()
				.eq(Promserver::getStatus, 1)
				.eq(Promserver::getType, type)
				.orderByAsc(Promserver::getId)
				.last("limit 1"));
	}
	
	@Override
	public Promserver getOneAvaPromServer(Integer promId,Integer idcId,Integer type) {
		type = (type == null) ? 1 :type;
		List<Promserver> list = this.list(new QueryWrapper<Promserver>().lambda()
				.eq(true, Promserver::getStatus, 1)
				.eq(promId!=null, Promserver::getId, promId)
				.eq(promId==null&&type!=null, Promserver::getType, type)
				.eq(promId==null&&idcId != null, Promserver::getDcId, idcId));
		// 校验是否有可用 prometheus server
		// ValidateUtils.is(list).notNull(RCode.PROMSERVER_HOST_ISNULL);
		// Promserver server = list.get(RandomUtils.nextInt(0, list.size()));

		Promserver server = null;
		if (CollectionUtils.isNotEmpty(list)) {
			server = list.get(RandomUtils.nextInt(0, list.size()));
		}
		return server;
	}

	/**
	 * 获取 prometheus 请求路径前缀
	 * 本地存储使用prometheus，直接请求prometheus
	 * @return
	 */
	@Override
	public String getPromPathPrefix() {
		String storageType = configService.getValue(Constant.SYSCONFIG_KEY_METRICS_STORAGE_TYPE);
		String agentPath = configService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
		String prePath = "1".equals(storageType) ? "/prometheus/proxy" :"/cortex/proxy/prometheus";
		String proxyPath = String.format("%s%s",agentPath, prePath);
		return proxyPath;
	}
	
	
	@Override
	public Integer saveOrUpdatePromserver(Promserver promserverEntity, boolean modify) {
        // 校验
		this.validateParam(promserverEntity);

		// 添加设置默认值
		if(!modify){
			promserverEntity.setStatus(1);
			promserverEntity.setStatusInfo(JSONObject.toJSONString(new HashMap<>()));
		}

		// 保存/修改
		this.saveOrUpdate(promserverEntity);
		return promserverEntity.getId();
	}

	@Override
	public void updateStatusInfo(Promserver promserver) {
		this.baseMapper.updateStatusInfo(promserver);
	}

	/**
	 * 详情
	 * @param id
	 * @return
	 */
	@Override
	public Promserver queryPromServerDetails(Integer id) {
		Promserver promserver = this.getById(id);
		ValidateUtils.is(promserver).notNull(RCode.PROMSERVER_NOTEXSITS);

		Dc dc = dcService.getById(promserver.getDcId());
		promserver.setDc(dc);
		return promserver;
	}

	@Override
	public JSONObject requestAgent(Promserver server, String urlPath, String requestAgent) {
		String urlStr = UrlBuilder.create().setScheme(server.getProtocol()).setHost(server.getHost())
				.setPort(server.getPort()).setPath(UrlPath.of(urlPath, Charset.forName("UTF-8")))
				.setQuery(UrlQuery.of(URLEncoder.createQuery().encode(requestAgent, Charset.forName("UTF-8")), Charset.forName("UTF-8"))).toString();

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, server.getToken());
		HttpEntity httpEntity = new HttpEntity(headers);
		ResponseEntity<String> responseEntity = null;
		try {
			responseEntity = restTemplate.exchange(new URI(urlStr), HttpMethod.GET, httpEntity, String.class);
		} catch (URISyntaxException e) {
			log.error(e);
		}
		log.info("agent request url:{},responseStatus:{}",urlStr,responseEntity.getStatusCodeValue());
		if (log.isDebugEnabled()) {
			log.debug("agent request body:{}",urlStr,responseEntity.getStatusCodeValue(),responseEntity.getBody());
		}
		return JSONObject.parseObject(responseEntity.getBody());
	}
	
}
