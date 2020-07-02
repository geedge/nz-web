package com.nis.modules.sys.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.panel.entity.Node;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.NodeService;
import com.nis.modules.sys.dao.LinkDao;
import com.nis.modules.sys.entity.Link;
import com.nis.modules.sys.service.LinkService;

@Service
public class LinkServiceImpl extends ServiceImpl<LinkDao, Link> implements LinkService {
	@Autowired
	private LinkDao linkDao;
	@Autowired
	private NodeService nodeService;
	@Override
	public List<Link> queryList(Map<String, Object> params) {
		List<Link> list = linkDao.queryList(params);
		if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        List<Integer> ids = list.stream().map(Link::getId).collect(Collectors.toList());

        // 该type下所有的panel
        Map<String, Object> param =new HashMap<String,Object>();
        List<Link> allDatas = linkDao.queryList(param);
        // 排序
        allDatas = this.getSequentialLinkList(allDatas);

        Iterator<Link> iterator = allDatas.iterator();
        while (iterator.hasNext()) {
            Link next = iterator.next();
            if (!ids.contains(next.getId())) {
                iterator.remove();
            }
        }
		return allDatas;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void modifyOrder(Link link) {
		List<Link> links = this.list();
		Map<Integer,Node> map =new HashMap<Integer,Node>();
		for(Link data : links) {
			map.put(data.getId(), data);
		}
		Link oldLink = this.getById(link.getId());
		// 校验 prev next 参数是否合法
        if (nodeService.validatePrevAndNext(map, link)) {
            return;
        }
		List<Node> modify = nodeService.modify(map, link, oldLink);
		if(ToolUtil.isNotEmpty(modify)) {
			List<Link> results =new ArrayList<Link>();
			for(Node node : modify) {
				results.add((Link)node);
			}
			this.saveOrUpdateBatch(results);
		}
	}

	
	
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveLink(Link link) {
		
		// 判断当前panel下是否存在chart
		List<Link> list = this.list();
        // 保存chart信息
        link.setNext(-1);

        if (CollectionUtils.isEmpty(list)) {
            // 一开始没有内容
            link.setPrev(0);
            this.save(link);
        } else {
            // 已存在chart 再次新增时自动排到末尾
            Link data = this.getOne(new QueryWrapper<Link>().lambda().eq(Link::getNext, -1));
            link.setPrev(data.getId());
            this.save(link);
            // 更新原来最后图表顺序
            data.setNext(link.getId());
            this.updateById(data);
        }
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeLinks(List<String> ids) {
		if (ToolUtil.isEmpty(ids)) {
            return;
        }

        List<Link> links = this.list();
        // 排序
        links = this.getSequentialLinkList(links);


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

            // 处理删除之后的位置问题
            this.handlePositionAfterDelete(links, deleteIds);
        }
		
	}

	private List<Link> getSequentialLinkList(List<Link> links) {
        List<Link> resultList = new ArrayList<>();

        // 按照prev升序排序
        links = links.stream().sorted(Comparator.comparing(Link::getPrev)).collect(Collectors.toList());

        Map<Integer, Link> idAndLinkMap = new HashMap<>();
        for (Link link : links) {
        	idAndLinkMap.put(link.getId(), link);
        }

        Link firstLink = links.get(0);

        Link tempLink = firstLink;
        resultList.add(firstLink);
        Link nextLink = null;

        while (true) {
            nextLink = idAndLinkMap.get(tempLink.getNext());
            if (nextLink != null) {
                resultList.add(nextLink);
                tempLink = nextLink;
            } else {
                break;
            }
        }
        return resultList;
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
	
    /**
     * 处理删除之后的位置信息
     *
     * @param sequentialChartList 有序集合列表
     * @param deleteIds           删除的charts ids
     */
    private void handlePositionAfterDelete(List<Link> sequentialLinkList, List<String> deleteIds) {
        // 全删除了
        if (sequentialLinkList.size() == deleteIds.size()) {
            return;
        }

        // 保证原先次序 并且 删除之后剩余的集合
        List<Link> deletedLinkList = new ArrayList<>();
        for (Link obj : sequentialLinkList) {
            if (!deleteIds.contains(obj.getId().toString())) {
                deletedLinkList.add(obj);
            }
        }

        Link firstLink = deletedLinkList.get(0);
        firstLink.setPrev(0);

        // 剩余一个
        if (deletedLinkList.size() == 1) {
            firstLink.setNext(-1);
            this.updateById(firstLink);
        } else {
            // 剩余数量大于 1
            List<Link> updateLinks = new ArrayList<>();

            Link tempLink = null;
            int size = deletedLinkList.size();
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    firstLink.setNext(deletedLinkList.get(i + 1).getId());
                    updateLinks.add(firstLink);
                } else {
                    tempLink = deletedLinkList.get(i);
                    tempLink.setPrev(updateLinks.get(updateLinks.size() - 1).getId());
                    if (i < size - 1) {
                        tempLink.setNext(deletedLinkList.get(i + 1).getId());
                    }
                    updateLinks.add(tempLink);
                }
            }
            updateLinks.get(updateLinks.size() - 1).setNext(-1);
            this.saveOrUpdateBatch(updateLinks);
        }
    }
}
