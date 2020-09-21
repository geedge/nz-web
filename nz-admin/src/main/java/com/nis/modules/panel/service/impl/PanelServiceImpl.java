package com.nis.modules.panel.service.impl;

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
            List<Panel> panels = this.baseMapper.queryList(params);
            if (CollectionUtils.isEmpty(panels)) {
                return panels;
            }
            List<Integer> ids = panels.stream().map(Panel::getId).collect(Collectors.toList());

            // 该type下所有的panel
            panelList = this.baseMapper.queryListByType(type);
            // 排序
            panelList = this.getSequentialPanelList(panelList);

            Iterator<Panel> iterator = panelList.iterator();
            while (iterator.hasNext()) {
                Panel next = iterator.next();
                if (!ids.contains(next.getId())) {
                    iterator.remove();
                }
            }
        } else {
            // 3. 其他类型不进行排序
            panelList = this.baseMapper.queryList(params);
        }
        return panelList;
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

        // 2. 若关联 chart 则不允许删除
        List<Integer> idList = Arrays.stream(ids).collect(Collectors.toList());
        List<Chart> charts = chartService.list(new QueryWrapper<Chart>().lambda().in(Chart::getPanelId, idList));
        Set<Integer> chartUsedPanelIds = charts.stream().map(Chart::getPanelId).collect(Collectors.toSet());
        idList.retainAll(chartUsedPanelIds);
        if (CollectionUtils.isNotEmpty(idList)) {
            List<String> panelNames = this.listByIds(idList).stream().map(Panel::getName).collect(Collectors.toList());
            throw new NZException("These panel contain charts and cannot be deleted, panels info: " + panelNames.toString(), RCode.PANEL_CHARTS_EXISTS.getCode());
        }

        // 3. dashboard panel 是有序的 获取当前所有 panel 留作稍后处理删除后的位置
        List<Panel> dashboardPanels = panels.stream().filter(panel -> Constant.PanelType.DASHBOARD.getValue().equals(panel.getType())).collect(Collectors.toList());

        List<Panel> deletePanel = dashboardPanels.stream().filter(panel -> Arrays.asList(ids).contains(panel.getId()) ? true : false).collect(Collectors.toList());
        List<Integer> delDeletePanelIds = deletePanel.stream().map(Panel::getId).collect(Collectors.toList());

        // 4. 删除 panel
        this.removeByIds(Arrays.asList(ids));

        // 5. 处理删除后的位置
        this.handlePositionAfterDelete(this.getSequentialPanelList(dashboardPanels), delDeletePanelIds);
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

    @Override
    public boolean validatePanelName(Integer id, String name) {
        List<Panel> list = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getName, name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }

    /**
     * 保存/修改
     *
     * @param panel
     */
    @Override
    public void saveOrUpdatePanel(Panel panel) {
        // 校验name是否重复
        if (!this.validatePanelName(panel.getId(), panel.getName())) {
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
        } else {
            panel.setType(Constant.PanelType.DASHBOARD.getValue());
        }

        Long userId = ShiroUtils.getUserId();
        panel.setCreateBy(userId.intValue());

        if (panel.getId() == null) {
            panel.setNext(-1);
            // 保存
            List<Panel> list = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getType, panel.getType()));
            if (CollectionUtils.isEmpty(list)) {
                panel.setPrev(0);
                this.save(panel);
            } else {
                // 已存在chart 再次新增时自动排到末尾
                Panel endPanel = list.stream().filter(panel1 -> panel1.getNext().equals(Constant.END_NEXT_VALUE)).findAny().orElse(null);
                panel.setPrev(endPanel.getId());
                this.save(panel);

                // 更新原来最后图表顺序
                endPanel.setNext(panel.getId());
                this.updateById(endPanel);
            }
        } else {
            // 修改
            this.updateById(panel);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void modify(Panel panel) {
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
