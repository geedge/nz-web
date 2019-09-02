package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.node.dao.NodeGroupRelDao;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeGroupRelEntity;
import com.nis.modules.node.service.NodeGroupRelService;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * 节点 节点组关系表
 * @author shizhendong
 */
@Service("nodeGroupRelService")
public class NodeGroupRelServiceImpl extends ServiceImpl<NodeGroupRelDao, NodeGroupRelEntity> implements NodeGroupRelService {

	@Override
	public List<NodeGroupRelEntity> queryBySystemId(Long id) {
		List<NodeGroupRelEntity> queryBySystemId = baseMapper.queryBySystemId(id);
		return queryBySystemId;
	}


	/**
	* @Description 查询单个节点组下，某种类型节点的数量
	* @Author  rui
	* @Date   2019/9/2
	* @Param  nodeGroupId 节点组id type 节点的类型
	* @Return
	* @Exception
	*/
	public Integer queryCount(Long nodeGroupId,String type){
		return this.baseMapper.queryCountByGroupId(nodeGroupId, type);
	}
}