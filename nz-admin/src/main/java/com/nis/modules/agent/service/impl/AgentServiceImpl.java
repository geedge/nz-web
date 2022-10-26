package com.nis.modules.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.agent.dao.AgentDao;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentDao, Agent> implements AgentService {

	private static Log log = Log.get();

	@Autowired
	private DcService dcService;

	@Autowired
	private AgentDao agentDao;

	@Autowired
	private RedisTemplate redisTemplate;

	@SuppressWarnings("unchecked")
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

		IPage page = new Query(Agent.class).getPage(params);
		List<Agent> agentList = agentDao.queryList(page, params);
		// 获取缓存中检查时间信息
		Map agentCaches = redisTemplate.opsForHash().entries(Constant.AGENT_STATE_CACHE_KEY);
		if(ToolUtil.isNotEmpty(agentCaches)) {
			for(Agent prom : agentList) {
				Object promCache = agentCaches.get(prom.getId().toString());
				if(ToolUtil.isNotEmpty(promCache)) {
					prom.setCheckTime(JSON.parseObject((String)promCache, Agent.class).getCheckTime());
					prom.setTs(JSON.parseObject((String) promCache, Agent.class).getTs());
				}
			}
		}
		page.setRecords(agentList);
		return new PageUtils(page);
	}


	public void validateParam(Agent agentEntity) {
		// host port type 唯一
		List<Agent> pList = this.list(new LambdaQueryWrapper<Agent>()
				.eq(Agent::getHost, agentEntity.getHost())
				.eq(Agent::getPort, agentEntity.getPort())
				.eq(Agent::getType, agentEntity.getType()));

		boolean flag = false;
		if ((pList == null || pList.size() == 0) || (pList != null && pList.size() == 1 && pList.get(0).getId().equals(agentEntity.getId()))) {
			flag = true;
		}

		if (!flag) {
			throw new NZException(RCode.AGENT_EXIST_ERROR);
		}

		Agent agent = null;
		if (agentEntity.getId() == null){
			agent = this.getOne(new LambdaQueryWrapper<Agent>().eq(Agent::getName, agentEntity.getName()));
			if (Tool.ObjectUtil.isNotEmpty(agent)){
				throw new NZException(RCode.AGENT_NAME_EXISTED);
			}
		}else {
			agent = this.getOne(new LambdaQueryWrapper<Agent>().eq(Agent::getName, agentEntity.getName()));
			if (Tool.ObjectUtil.isNotEmpty(agent) && !agent.getId().equals(agentEntity.getId())){
				throw new NZException(RCode.AGENT_NAME_EXISTED);
			}
		}

		Integer type = agentEntity.getType();
		String host = agentEntity.getHost();
		Integer port = agentEntity.getPort();
		ValidateUtils.is(agentEntity.getDcId()).notNull(RCode.AGENT_DCID_ISNULL)
				.and(host).notNull(RCode.AGENT_HOST_ISNULL).maxLength(64)
				.and(type).notNull(RCode.AGENT_TYPE_ISNULL)
				.and(port).notNull(RCode.AGENT_PORT_ISNULL).maxLength(10)
				.and(agentEntity.getToken()).notNull(RCode.AGENT_TOKEN_ISNULL).maxLength(64)
		        .and(agentEntity.getName()).notNull(RCode.AGENT_NAME_ISNULL).maxLength(256);

		// 校验ip是否合法
		boolean ipFlag = CommonUtils.checkIp(host);
		if (!ipFlag) {
			throw new NZException(RCode.AGENT_HOST_ERROR);
		}
		// 校验port是否合法
		boolean portFlag = CommonUtils.checkPort(port.toString());
		if (!portFlag) {
			throw new NZException(RCode.AGENT_PORT_ERROR);
		}

		// dc 存在校验
		Dc dc = dcService.getById(agentEntity.getDcId());
		ValidateUtils.is(dc).notNull(RCode.DC_NOT_EXISTS);

		// 校验 type 合法性
		Constant.AgentType[] values = Constant.AgentType.values();
		List<Integer> list = Arrays.asList(values).stream().map(Constant.AgentType::getValue).collect(Collectors.toList());
		if (!list.contains(type)) {
			throw new NZException(RCode.AGENT_TYPE_INVALIDE);
		}
	}

	@Override
	public Integer saveOrUpdateAgent(Agent agent, boolean modify) {
        // 校验
		agent.setName(StrUtil.trim(agent.getName()));
		this.validateParam(agent);

		// 添加设置默认值
		if(!modify){
			agent.setStatus(1);
			agent.setStatusInfo(JSONObject.toJSONString(new HashMap<>()));
		}

		// 保存/修改
		this.saveOrUpdate(agent);
		return agent.getId();
	}

	@Override
	public void updateStatusInfo(Agent agent) {
		this.baseMapper.updateStatusInfo(agent);
	}

	/**
	 * 详情
	 *
	 * @param id
	 * @return
	 */
	@Override
	public Agent queryAgentDetails(Integer id) {
		Agent agent = this.getById(id);
		ValidateUtils.is(agent).notNull(RCode.AGENT_NOTEXSITS);

		Dc dc = dcService.getById(agent.getDcId());
		agent.setDc(dc);
		return agent;
	}
}
