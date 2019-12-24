package com.nis.modules.panel.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.panel.dao.PanelDao;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class PanelServiceImpl extends ServiceImpl<PanelDao, Panel> implements PanelService {
    @Autowired
    private ChartService chartService;

    @Override
    public PageUtils queryPanels(Map<String, Object> params) {

        Integer id=params.get("id")==null?null:Integer.parseInt((String)params.get("id"));

        String name=params.get("name")==null?null:(String)params.get("name");

        Integer createBy=params.get("createBy")==null?null:Integer.parseInt((String)params.get("createBy"));

        IPage<Panel> page=this.page(new Query<Panel>().getPage(params));

        List<Panel> list=this.baseMapper.queryPanelsWithPage(page,id,name,createBy);

        page.setRecords(list);

        return new PageUtils(page);
    }

    @Override
    public void delPanels(Integer... ids) {
        this.removeByIds(Arrays.asList(ids));

        chartService.delChartsByPanelId(ids);
    }
}
