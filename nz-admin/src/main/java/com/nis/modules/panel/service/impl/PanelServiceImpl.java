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
        String type = (String) params.get("type");
        if (StringUtils.isEmpty(type)) {
            type = "dashboard";
            params.put("type", type);
        }

        List<Panel> panels = this.baseMapper.queryList(params);
        if (CollectionUtils.isEmpty(panels)) {
            return panels;
        }
        List<Integer> ids = panels.stream().map(Panel::getId).collect(Collectors.toList());

        // 该type下所有的panel
        List<Panel> panelList = this.baseMapper.queryListByType(type);
        // 排序
        panelList = this.getSequentialPanelList(panelList);

        Iterator<Panel> iterator = panelList.iterator();
        while (iterator.hasNext()) {
            Panel next = iterator.next();
            if (!ids.contains(next.getId())) {
                iterator.remove();
            }
        }
        return panelList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPanels(Integer... ids) {
        if (ToolUtil.isEmpty(ids)) {
            return;
        }

        // 校验内置panel不能被删除
        List<Integer> idList = Arrays.asList(ids);
        this.validateBuildInPanels(idList);

        // 所有 panel
        List<Panel> panelList = this.list();
        Map<String, List<Panel>> panelsGroupByType = panelList.stream().collect(Collectors.groupingBy(Panel::getType));

        List<Panel> deletePanel = this.list(new QueryWrapper<Panel>().lambda().in(Panel::getId, idList));
        Map<String, List<Panel>> deletePanelsGroupByTypes = deletePanel.stream().collect(Collectors.groupingBy(Panel::getType));


        // 删除panel
        this.removeByIds(idList);

        // 删除关联chart信息
        chartService.delChartsByPanelId(ids);

        // 处理删除之后的位置 因为每个id可能type不一致 所以要依次删除并处理顺序
        List<Integer> delIdsByType = new ArrayList<>();
        for (Map.Entry<String, List<Panel>> entry : panelsGroupByType.entrySet()) {
            delIdsByType = deletePanelsGroupByTypes.get(entry.getKey()).stream().map(Panel::getId).collect(Collectors.toList());
            this.handlePositionAfterDelete(this.getSequentialPanelList(entry.getValue()), delIdsByType);
        }
    }

    private void handlePositionAfterDelete(List<Panel> sequentialPanelList, List<Integer> deleteIds) {
        // 全删除了
        if (sequentialPanelList.size() == deleteIds.size()) {
            return;
        }

        // 保证原先次序 并且 删除之后剩余的集合
        List<Panel> deletedPanelList = new ArrayList<>();
        for (Panel obj : sequentialPanelList) {
            if (!deleteIds.contains(obj.getId())) {
                deletedPanelList.add(obj);
            }
        }

        Panel firstPanel = deletedPanelList.get(0);
        firstPanel.setPrev(0);

        // 剩余一个
        if (deletedPanelList.size() == 1) {
            firstPanel.setNext(-1);
            this.updateById(firstPanel);
        } else {
            // 剩余数量大于 1
            List<Panel> updatePanels = new ArrayList<>();

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
            updatePanels.get(updatePanels.size() - 1).setNext(-1);
            this.saveOrUpdateBatch(updatePanels);
        }
    }

    private void validateBuildInPanels(List<Integer> idList) {
        // 内置panel不能删除
        List<Panel> buildInPanels = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getBuildIn, 1));
        Map<Integer, String> buildInPanelIdAndNameMap = buildInPanels.stream().collect(Collectors.toMap(Panel::getId, Panel::getName));

        idList.stream().forEach(id -> {
            if (ToolUtil.isNotEmpty(buildInPanelIdAndNameMap.get(id))) {
                throw new NZException(buildInPanelIdAndNameMap.get(id) + " panel is built-in and cannot be deleted", RCode.PANEL_BUILDIN_CAN_NOT_REMOVE.getCode());
            }
        });
    }

    @Override
    public boolean validatePanelName(Integer id, String name) {
        List<Panel> list = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getName, name));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
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
            if (!Constant.PANEL_TYPES.contains(type)) {
                throw new NZException(RCode.PANEL_TYPE_INVALIDE);
            }
        } else {
            panel.setType("dashboard");
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
                Panel endPanel = list.stream().filter(panel1 -> panel1.getNext().equals(-1)).findAny().orElse(null);
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
        if (!Constant.PANEL_TYPES.contains(type)) {
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
            oldPanel.setPrev(0);
            oldPanel.setNext(-1);
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

    private List<Panel> getSequentialPanelList(List<Panel> panels) {
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
