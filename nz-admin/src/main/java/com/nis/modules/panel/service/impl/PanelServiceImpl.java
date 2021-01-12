package com.nis.modules.panel.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.panel.dao.PanelDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.Node;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.NodeService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class PanelServiceImpl extends ServiceImpl<PanelDao, Panel> implements PanelService {
    @Autowired
    private ChartService chartService;

    @Autowired
    private NodeService nodeService;

    @Override
    public List<Panel> queryList(Map<String, Object> params) {
        // 1. 查询时 type 为空则默认 dashboard 类型
        String type = (String) params.get("type");
        if (StringUtils.isEmpty(type)) {
            type = Constant.PanelType.DASHBOARD.getValue();
            params.put("type", type);
        }
        // 2. dashboard 类型 需要进行排序返回
        List<Panel> panelList = new ArrayList<>();
        if (Constant.PanelType.DASHBOARD.getValue().equals(type)) {
            panelList = this.baseMapper.queryList(params);
            if (CollectionUtils.isEmpty(panelList) || panelList.size() == 1) {
                return panelList;
            }
            panelList = this.getPanelTreeList(panelList, 0);
            return panelList;
        } else {
            // 3. 其他类型不进行排序
            panelList = this.baseMapper.queryList(params);
        }
        return panelList;
    }

    /**
     * 递归获取树结构
     *
     * @param panelList
     * @param parentId
     * @return
     */
    private List<Panel> getPanelTreeList(List<Panel> panelList, Integer parentId) {
        List<Panel> resultList = new ArrayList<>();
        for (Panel panel : panelList) {
            Integer id = panel.getId();
            Integer pid = panel.getPid();
            if (parentId.equals(pid)) {
                panel.setChildren(this.getPanelTreeList(panelList, id));
                resultList.add(panel);
            }
        }
        return resultList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPanelsAndChartInfoByPanelIds(Set<Integer> panelIds) {
        if (CollectionUtils.isNotEmpty(panelIds)) {
            // 删除 panel
            this.removeByIds(panelIds);

            // 删除 chart info
            Integer[] removePanelIds = panelIds.stream().toArray(n -> new Integer[n]);
            chartService.delChartsByPanelId(removePanelIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPanels(Integer... ids) {
        if (ToolUtil.isEmpty(ids)) {
            return;
        }

        List<Panel> panels = this.list();

        // 1. 校验内置panel不能被删除
        this.validateBuildInPanels(panels, ids);

        // 获取这些节点的所有子节点
        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        List<Panel> panelList = this.list();
        Set<Integer> allPanelIds = new HashSet<>();
        for (Integer id : idList) {
            allPanelIds.add(id);
            allPanelIds.addAll(this.getChildrenIds(panelList, id));
        }
        Set<Integer> delPanelIds = ObjectUtil.clone(allPanelIds);

        // 2. 若关联 chart 则不允许删除
        List<Chart> charts = chartService.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, allPanelIds));
        Set<Integer> chartUsedPanelIds = charts.stream().map(Chart::getPanelId).collect(Collectors.toSet());
        allPanelIds.retainAll(chartUsedPanelIds);
        if (CollectionUtils.isNotEmpty(allPanelIds)) {
            List<String> panelNames = this.listByIds(allPanelIds).stream().map(Panel::getName).collect(Collectors.toList());
            throw new NZException("These panel contain charts and cannot be deleted, panels info: " + panelNames.toString(), RCode.PANEL_CHARTS_EXISTS.getCode());
        }

        // 3. 如果包含子节点 以及 子节点 ，也一并删除
        this.removeByIds(delPanelIds);
    }

    /**
     * 查询所有子节点 ID
     * @param panelList
     * @param parentId
     * @return
     */
    private List<Integer> getChildrenIds(List<Panel> panelList, Integer parentId) {
        List<Integer> resultList = new ArrayList<>();
        for (Panel panel : panelList) {
            Integer id = panel.getId();
            Integer pid = panel.getPid();
            if (parentId.equals(pid)) {
                resultList.add(id);
                resultList.addAll(this.getChildrenIds(panelList, id));
            }
        }
        return resultList;
    }

    /**
     * 处理删除后的位置
     * @param sequentialPanelList      删除之前有序的链表
     * @param deleteIds                删除的 IDS
     */
    @Override
    public void handlePositionAfterDelete(List<Panel> sequentialPanelList, List<Integer> deleteIds) {
        // 1. 全删除了
        if (sequentialPanelList.size() == deleteIds.size()) {
            return;
        }

        // 2. 删除之后的剩余元素  有序
        List<Panel> deletedPanelList = new ArrayList<>();
        for (Panel obj : sequentialPanelList) {
            if (!deleteIds.contains(obj.getId())) {
                deletedPanelList.add(obj);
            }
        }

        // 开头 panel
        Panel firstPanel = deletedPanelList.get(0);
        firstPanel.setPrev(Constant.START_PREV_VALUE);

        // 剩余一个
        if (deletedPanelList.size() == 1) {
            firstPanel.setNext(Constant.END_NEXT_VALUE);
            this.updateById(firstPanel);
        } else {
            // 剩余数量大于 1
            List<Panel> updatePanels = new ArrayList<>();

            // 批量更新 需要更新 prev next panel
            Panel tempPanel = null;
            int size = deletedPanelList.size();
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    firstPanel.setNext(deletedPanelList.get(i + 1).getId());
                    updatePanels.add(firstPanel);
                } else {
                    tempPanel = deletedPanelList.get(i);
                    tempPanel.setPrev(updatePanels.get(updatePanels.size() - 1).getId());
                    if (i < size - 1) {
                        tempPanel.setNext(deletedPanelList.get(i + 1).getId());
                    }
                    updatePanels.add(tempPanel);
                }
            }
            // 末尾 设置 next 为 -1
            updatePanels.get(updatePanels.size() - 1).setNext(Constant.END_NEXT_VALUE);
            this.saveOrUpdateBatch(updatePanels);
        }
    }

    /**
     * 校验内置 panel 不可删除
     * @param panels
     * @param removeIds
     */
    private void validateBuildInPanels(List<Panel> panels, Integer... removeIds) {
        // 内置 panel 不能删除
        List<Integer> buildInPanels = panels.stream().filter(panel -> panel.getBuildIn() == 1).map(Panel::getId).collect(Collectors.toList());
        List<Integer> removeIdList = Arrays.stream(removeIds).collect(Collectors.toList());
        removeIdList.retainAll(buildInPanels);
        if (CollectionUtils.isNotEmpty(removeIdList)) {
            List<String> panelNames = this.listByIds(removeIdList).stream().map(Panel::getName).collect(Collectors.toList());
            throw new NZException("These panel is built-in and cannot be deleted, panels info: " + panelNames.toString(), RCode.PANEL_BUILDIN_CAN_NOT_REMOVE.getCode());
        }
    }

    /**
     * 校验 panel 相关参数是否正确
     *
     * @param panel
     */
    public void validatePanel(Panel panel) {
        String name = panel.getName();
        ValidateUtils.is(name).notNull(RCode.PANEL_NAME_ISNULL);

        if (name.contains("/")) {
            throw new NZException(RCode.PANEL_NAME_FORMAT_ERROR);
        }

        Integer pid = panel.getPid();
        pid = (pid == null) ? 0 : pid;
        // 校验name是否重复
        List<Panel> list = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getPid, pid).eq(Panel::getName, panel.getName()));
        boolean flag = false;
        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(panel.getId()))) {
            flag = true;
        }

        if (!flag) {
            throw new NZException(RCode.PANEL_NAME_DUPLICATE);
        }

        // link 不为空时
        if (ToolUtil.isNotEmpty(panel.getLink())) {
            // 校验 type
            String type = panel.getType();
            ValidateUtils.is(type).notNull(RCode.PANEL_TYPE_ISNULL);
            List<Constant.PanelType> panelTypes = Arrays.asList(Constant.PanelType.values());
            List<String> types = panelTypes.stream().map(Constant.PanelType::getValue).collect(Collectors.toList());
            if (!types.contains(type)) {
                throw new NZException(RCode.PANEL_TYPE_INVALIDE);
            }
        }

        // pid 是否存在
        if (panel.getPid() != null && panel.getPid() != 0) {
            Panel entity = this.getById(panel.getPid());
            if (entity == null) {
                throw new NZException(RCode.PANEL_NOT_EXIST);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePanel(Panel panel) {
        this.validatePanel(panel);

        Long userId = ShiroUtils.getUserId();
        panel.setCreateBy(userId.intValue());

        panel.setPrev(0);
        panel.setNext(-1);

        if (ToolUtil.isEmpty(panel.getLink())) {
            panel.setType(Constant.PanelType.DASHBOARD.getValue());
        }

        Integer pid = panel.getPid();
        pid = (pid == null) ? 0 : pid;
        // 查询该父级节点下是否存在 Panel 节点
        List<Panel> list = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, panel.getType()).eq(Panel::getPid, pid));
        if (CollectionUtils.isEmpty(list)) {
            // 父级节点下暂无其他节点，直接保存
            this.save(panel);
        } else {
            // 找到当前末尾的 panel ， 加到其后
            Panel endPanel = list.stream().max(Comparator.comparingInt(Panel::getWeight)).get();
            Integer weight = endPanel.getWeight();
            panel.setWeight(++weight);
            this.save(panel);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePanel(Panel panel) {
        this.validatePanel(panel);

        Panel oldPanel = this.getById(panel.getId());

        if (oldPanel == null) {
            throw new NZException(RCode.PANEL_NOT_EXIST);
        }

        // 改变 pid 父级节点时，将该 panel 加入到该 pid 下末尾位置
        if (!oldPanel.getPid().equals(panel.getPid())) {
            Panel endPanel = this.getOne(new LambdaQueryWrapper<Panel>().eq(Panel::getPid, panel.getPid()).orderByDesc(Panel::getWeight).last("limit 0,1"));
            // 该 pid 下存在节点
            if (endPanel != null) {
                Integer weight = endPanel.getWeight();
                panel.setWeight(++weight);
            } else {
                panel.setWeight(0);
            }
        }

        // 修改
        this.updateById(panel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void modify(Panel panel) {
        String type = panel.getType();
        List<Constant.PanelType> panelTypes = Arrays.asList(Constant.PanelType.values());
        List<String> types = panelTypes.stream().map(Constant.PanelType::getValue).collect(Collectors.toList());
        if (!types.contains(type)) {
            throw new NZException(RCode.PANEL_TYPE_INVALIDE);
        }

        List<Panel> panelList = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, type));

        // 当前类型没有panel
        if (CollectionUtils.isEmpty(panelList)) {
            throw new NZException(RCode.PANEL_NOTFOUND_BYTYPE);
        }

        Map<Integer, Node> idAndPanelMap = new HashMap<>();
        for (Panel panel1 : panelList) {
            idAndPanelMap.put(panel1.getId(), panel1);
        }

        // 当前类型下不存在此panel
        Node oldPanel = idAndPanelMap.get(panel.getId());
        if (oldPanel == null) {
            throw new NZException(RCode.PANEL_NOT_EXIST);
        }

        // 就只有一个图表 不让其调整位置
        if (panelList.size() == 1) {
            oldPanel.setPrev(Constant.START_PREV_VALUE);
            oldPanel.setNext(Constant.END_NEXT_VALUE);
            this.updateById((Panel) oldPanel);
            return;
        }

        // 校验 prev next 参数是否合法
        if (nodeService.validatePrevAndNext(idAndPanelMap, panel)) {
            return;
        }

        List<Node> updatePanels = nodeService.modify(idAndPanelMap, panel, oldPanel);

        // 更新库中数据
        if (CollectionUtils.isNotEmpty(updatePanels)) {
            Set<Panel> panels = new HashSet<>();
            for (Node updatePanel : updatePanels) {
                panels.add((Panel) updatePanel);
            }
            this.saveOrUpdateBatch(panels);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(List<Panel> panelList) {
        if (CollectionUtils.isEmpty(panelList)) throw new NZException(RCode.PANEL_ID_ISNULL);
        for (Panel panel : panelList) {
            if (panel.getId() == null) throw new NZException(RCode.PANEL_ID_ISNULL);
            if (panel.getPid() == null) throw new NZException(RCode.PANEL_PID_ISNULL);
            if (panel.getWeight() == null) throw new NZException(RCode.PANEL_WEIGHT_ISNULL);
        }

        this.getBaseMapper().updateWeightBatch(panelList);
    }

    @Override
    public List<Panel> getSequentialPanelList(List<Panel> panels) {
        if (CollectionUtils.isEmpty(panels)) {
            return new ArrayList<>();
        }
        List<Panel> resultList = new ArrayList<>();

        // 按照prev升序排序
        panels = panels.stream().sorted(Comparator.comparing(Panel::getPrev)).collect(Collectors.toList());

        Map<Integer, Panel> idAndPanelMap = new HashMap<>();
        for (Panel panel : panels) {
            idAndPanelMap.put(panel.getId(), panel);
        }

        Panel firstPanel = panels.get(0);

        Panel tempPanel = firstPanel;
        resultList.add(firstPanel);
        Panel nextPanel = null;

        while (true) {
            nextPanel = idAndPanelMap.get(tempPanel.getNext());
            if (nextPanel != null) {
                resultList.add(nextPanel);
                tempPanel = nextPanel;
            } else {
                break;
            }
        }
        return resultList;
    }
}
