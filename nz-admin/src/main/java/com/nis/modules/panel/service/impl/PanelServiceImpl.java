package com.nis.modules.panel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.panel.dao.PanelDao;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class PanelServiceImpl extends ServiceImpl<PanelDao, Panel> implements PanelService {
    @Autowired
    private ChartService chartService;

    @Override
    public PageUtils queryPanels(Map<String, Object> params) {
        IPage<Panel> page = new Query<Panel>().getPage(params);

        List<Panel> list = this.baseMapper.queryPanelsWithPage(page, params);

        page.setRecords(list);

        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPanels(Integer... ids) {
        if (ToolUtil.isEmpty(ids)) {
            return;
        }
        // 内置panel不能删除
        List<Panel> buildInPanels = this.list(new QueryWrapper<Panel>().lambda().eq(Panel::getBuildIn, 1));
        Map<Integer, String> buildInPanelIdAndNameMap = buildInPanels.stream().collect(Collectors.toMap(Panel::getId, Panel::getName));

        List<Integer> list = Arrays.asList(ids);
        list.stream().forEach(id -> {
            if (ToolUtil.isNotEmpty(buildInPanelIdAndNameMap.get(id))) {
                throw new NZException(buildInPanelIdAndNameMap.get(id) + " panel is built-in and cannot be deleted", RCode.PANEL_BUILDIN_CAN_NOT_REMOVE.getCode());
            }
        });
        this.removeByIds(list);

        chartService.delChartsByPanelId(ids);
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

        this.saveOrUpdate(panel);
    }
}
