package com.nis.modules.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.agent.entity.Agent;

import java.util.Map;

public interface AgentService extends IService<Agent> {

    PageUtils queryPage(Map<String, Object> params);

    Integer saveOrUpdateAgent(Agent entity, boolean modify);

    void updateStatusInfo(Agent agent);

    /**
     * 详情
     *
     * @param id
     * @return
     */
    Agent queryAgentDetails(Integer id);

    void syncAgentStateCache(String agentJsonStr);

}
