package com.nis.modules.panel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.panel.dao.ChartDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.ChartElement;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartElementService;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class ChartServiceImpl extends ServiceImpl<ChartDao, Chart> implements ChartService {
    @Autowired
    private ChartElementService chartElementService;
    @Autowired
    private SysDictService sysDictService;
    @Autowired
    private PanelService panelService;

    @Override
    public List<Chart> queryCharts(Integer panelId, Integer id, String title) {

        List<Chart> charts=this.baseMapper.queryCharts(panelId,id,title);


        return charts;
    }

    @Override
    public void saveChart(Chart chart) {
        chart.setCreateAt(new Date());
        this.save(chart);
        for (ChartElement element:chart.getElements()){
            element.setChartId(chart.getId());
        }
        chartElementService.saveBatch(chart.getElements());
    }

    @Override
    public void updateChart(Chart chart) {
        this.updateById(chart);
        QueryWrapper<ChartElement> chartElementQueryWrapper = new QueryWrapper<>();
        chartElementQueryWrapper.eq("chart_id",chart.getId());
        List<ChartElement> list = chartElementService.list(chartElementQueryWrapper);

        List<ChartElement> saveList=new ArrayList<ChartElement>();
        List<ChartElement> updateList=new ArrayList<ChartElement>();

        for (ChartElement element:chart.getElements()){
            element.setChartId(chart.getId());

            if (element.getId()==null){
                saveList.add(element);
            }else{
                updateList.add(element);
            }

            if(list.contains(element)){ //数据库存在 element列表中没有的element，删除
                list.remove(element);
            }
        }
        if (saveList.size()>0){
            chartElementService.saveBatch(saveList);
        }
        if(updateList.size()>0){
            chartElementService.updateBatchById(updateList);
        }
        if(list.size()>0){
            List<Integer> elementIds=new ArrayList<Integer>();
            for (ChartElement element:list){
                elementIds.add(element.getId());
            }

            chartElementService.removeByIds(elementIds);
        }
    }

    @Override
    public void validateChart(Integer panelId, Chart chart) {
        //panel是否存在
        validatePanel(chart.getPanelId());
        //chart是否属于panel
        if(chart.getId()!=null){
            validateChartExists(panelId,chart.getId());
        }

        ValidateUtils.is(chart.getTitle()).notNull(RCode.CHART_TITLE_ISNULL);

        ValidateUtils.is(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL);

        ValidateUtils.is(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL);

        ValidateUtils.is(chart.getType()).notNull(RCode.CHART_TYPE_ISNULL);

        ValidateUtils.is(chart.getUnit()).notNull(RCode.CHART_UNIT_ISNULL);
        
        sysDictService.checkSysDicValue(Constant.DICT_CHART_TYPE,chart.getType(),RCode.CHART_TYPE_INVALIDE);;

        ValidateUtils.is(chart.getElements()).notNull(RCode.CHART_ELEMENT_ISNULL);

        chartElementService.validateElements(chart.getId(),chart.getElements());

    }

    private void validateChartExists(Integer panelId, Integer id) {
        QueryWrapper<Chart> chartQueryWrapper = new QueryWrapper<>();
        chartQueryWrapper.eq("panel_id",panelId);
        chartQueryWrapper.eq("id",id);

        Chart chart = this.getOne(chartQueryWrapper);

        if(chart == null){
            throw new NZException(RCode.CHART_NOTEXSITS);
        }
    }

    @Override
    public void validatePanel(Integer panelId) {
        Panel panel = panelService.getById(panelId);

        if(panel == null){
            throw new NZException(RCode.CHART_PANEL_ISNULL);
        }
    }

    @Override
    public void delCharts(Integer... ids) {
        if(ids.length<1){
            return;
        }
        this.removeByIds(Arrays.asList(ids));
        chartElementService.delElementByChartIds(ids);
    }

    @Override
    public void delChartsByPanelId(Integer... panelIds){
        if(panelIds.length<1){
            return ;
        }
        QueryWrapper<Chart> chartQueryWrapper = new QueryWrapper<>();
        chartQueryWrapper.in("panel_id",panelIds);

        this.remove(chartQueryWrapper);

        List<Integer> chartIds=this.baseMapper.queryChartIds(panelIds);

        chartElementService.delElementByChartIds(chartIds);
    }

}
