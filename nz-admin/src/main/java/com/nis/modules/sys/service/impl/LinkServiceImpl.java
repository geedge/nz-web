package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.panel.entity.Node;
import com.nis.modules.panel.service.NodeService;
import com.nis.modules.sys.dao.LinkDao;
import com.nis.modules.sys.entity.Link;
import com.nis.modules.sys.service.LinkService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LinkServiceImpl extends ServiceImpl<LinkDao, Link> implements LinkService {
	@Autowired
	private LinkDao linkDao;
	@Autowired
	private NodeService nodeService;
	@Override
	public List<Link> queryList(Map<String, Object> params) {
		List<Link> list = linkDao.queryList(params);
		return list;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveLink(Link link) {
        Link max = this.getOne(new QueryWrapper<Link>().select("id","weight").orderByDesc("id").last("limit 1"));
        if(ToolUtil.isEmpty(max)){
            link.setWeight(0);
        }else{
            link.setWeight(max.getWeight()+1);
        }
        this.save(link);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeLinks(List<String> ids) {
		if (ToolUtil.isEmpty(ids)) {
            return;
        }

        List<Link> links = this.list();


        // 内置chart不能删除
        this.validateBuildInLinks(ids);
        
        List<Integer> linkIdList = links.stream().map(Link::getId).collect(Collectors.toList());

        List<String> deleteIds = new ArrayList<>();
        for (String id : ids) {
            if (linkIdList.contains(Integer.valueOf(id))) {
                deleteIds.add(id);
            }
        }

        if (CollectionUtils.isNotEmpty(deleteIds)) {
            // 删除自身
            this.baseMapper.deleteBatchIds(deleteIds);
        }
		
	}

    @Override
    public void saveWeights(List<Link> links) {
        Link link = links.stream().filter(t -> ToolUtil.isEmpty(t.getId()) || ToolUtil.isEmpty(t.getWeight())).findAny().orElse(null);
        if(ToolUtil.isNotEmpty(link)){
            throw  new NZException(RCode.LINK_WEIGHT_PARAM_INVALID);
        }

        this.saveOrUpdateBatch(links);
    }

	/**
     * 校验是否是内置link
     *
     * @param linkIds
     */
    private void validateBuildInLinks(List<String> linkIds) {
        // 内置charts
        List<Link> buildInLinks = this.list(new QueryWrapper<Link>().lambda().eq(Link::getBuildIn, 1));
        Map<Integer, String> buildInLinkIdAndNameMap = buildInLinks.stream().collect(Collectors.toMap(Link::getId, Link::getName));


        linkIds.forEach(id -> {
            if (ToolUtil.isNotEmpty(buildInLinkIdAndNameMap.get(Integer.valueOf(id)))) {
                throw new NZException(buildInLinkIdAndNameMap.get(Integer.valueOf(id)) + " link is built-in and cannot be deleted", RCode.LINK_BUILDIN_CAN_NOT_REMOVE.getCode());
            }
        });
    }
	
}
