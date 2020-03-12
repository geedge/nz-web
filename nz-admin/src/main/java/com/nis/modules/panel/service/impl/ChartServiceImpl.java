package com.nis.modules.panel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.modules.panel.dao.ChartDao;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.ChartElement;
import com.nis.modules.panel.entity.Panel;
import com.nis.modules.panel.service.ChartElementService;
import com.nis.modules.panel.service.ChartService;
import com.nis.modules.panel.service.PanelService;
import com.nis.modules.sys.service.SysDictService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

        List<Chart> charts = this.baseMapper.queryCharts(panelId);
        if(CollectionUtils.isEmpty(charts)){
            return charts;
        }

        Map<Integer, Chart> idAndChartMap = new HashMap<>();
        for (Chart chart1 : charts) {
            idAndChartMap.put(chart1.getId(), chart1);
        }

        // 按照顺序排序后返回前端
        List<Chart> resultList = new ArrayList<>();

        // 后台查询时 order by prev
        Chart firstChart = charts.get(0);
        Chart tempChar = firstChart;
        boolean idFlag = (id != null);
        boolean titleFlag = StringUtils.isNotEmpty(title);

        // 如果 id 条件参数不为空
        if (idFlag) {
            List<Chart> list = new ArrayList<>();
            Chart chart1 = null;
            for (Chart obj : charts) {
                if (obj.getId().equals(id)) {
                    chart1 = obj;
                    break;
                }
            }
            // 通过id找到实体
            if (chart1 != null) {
                if (titleFlag) {
                    if (chart1.getTitle().contains(title)) {
                        list.add(chart1);
                    }
                } else {
                    list.add(chart1);
                }
            }
            return list;
        }


        if (titleFlag) {
            if (firstChart.getTitle().contains(title)) {
                resultList.add(firstChart);
            }
        } else {
            resultList.add(firstChart);
        }

        Chart nextChart = null;

        while (true) {
            nextChart = idAndChartMap.get(tempChar.getNext());
            if (nextChart != null) {
                // 模糊查询title
                if (titleFlag) {
                    if (nextChart.getTitle().contains(title)) {
                        resultList.add(nextChart);
                    }
                } else {
                    resultList.add(nextChart);
                }
                tempChar = nextChart;
            } else {
                break;
            }
        }
        return resultList;
    }

    @Override
    public void saveChart(Chart chart) {

        // 保存chart信息
        chart.setCreateAt(new Date());
        chart.setNext(-1);

        // 判断当前panel下是否存在chart
        List<Chart> list = this.list(new QueryWrapper<Chart>().eq("panel_id", chart.getPanelId()));

        if (CollectionUtils.isEmpty(list)) {
            // 一开始没有内容
            chart.setPrev(0);
            this.save(chart);
        } else {
            // 已存在chart 再次新增时自动排到末尾
            Chart endChart = this.getOne(new QueryWrapper<Chart>().eq("next", -1).eq("panel_id", chart.getPanelId()));
            chart.setPrev(endChart.getId());
            this.save(chart);
            // 更新原来最后图表顺序
            endChart.setNext(chart.getId());
            this.updateById(endChart);
        }
        // 保存chart elements信息
        for (ChartElement element : chart.getElements()) {
            element.setChartId(chart.getId());
        }
        chartElementService.saveBatch(chart.getElements());
    }

    @Override
    public void updateChart(Chart chart) {
        Integer prev = chart.getPrev();
        Integer next = chart.getNext();
        // 修改位置了
        if (prev != null && next != null) {
            this.modify(chart.getPanelId(), chart);
        }
        this.updateById(chart);

        // 处理 chart element 信息
        QueryWrapper<ChartElement> chartElementQueryWrapper = new QueryWrapper<>();
        chartElementQueryWrapper.eq("chart_id", chart.getId());
        List<ChartElement> list = chartElementService.list(chartElementQueryWrapper);

        List<ChartElement> saveList = new ArrayList<ChartElement>();
        List<ChartElement> updateList = new ArrayList<ChartElement>();

        for (ChartElement element : chart.getElements()) {
            element.setChartId(chart.getId());

            if (element.getId() == null) {
                saveList.add(element);
            } else {
                updateList.add(element);
            }

            if (list.contains(element)) { //数据库存在 element列表中没有的element，删除
                list.remove(element);
            }
        }
        if (saveList.size() > 0) {
            chartElementService.saveBatch(saveList);
        }
        if (updateList.size() > 0) {
            chartElementService.updateBatchById(updateList);
        }
        if (list.size() > 0) {
            List<Integer> elementIds = new ArrayList<Integer>();
            for (ChartElement element : list) {
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
        if (chart.getId() != null) {
            validateChartExists(panelId, chart.getId());
        }

        ValidateUtils.is(chart.getTitle()).notNull(RCode.CHART_TITLE_ISNULL);

        ValidateUtils.is(chart.getSpan()).notNull(RCode.CHART_SPAN_ISNULL);

        ValidateUtils.is(chart.getHeight()).notNull(RCode.CHART_HEIGHT_ISNULL);

        ValidateUtils.is(chart.getType()).notNull(RCode.CHART_TYPE_ISNULL);

        ValidateUtils.is(chart.getUnit()).notNull(RCode.CHART_UNIT_ISNULL);

        sysDictService.checkSysDicValue(Constant.DICT_CHART_TYPE, chart.getType(), RCode.CHART_TYPE_INVALIDE);

        ValidateUtils.is(chart.getElements()).notNull(RCode.CHART_ELEMENT_ISNULL);

        chartElementService.validateElements(chart.getId(), chart.getElements());

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
    @Transactional(rollbackFor = Exception.class)
    public void delCharts(Integer panelId, Integer... ids) {
        if (ids.length < 1) {
            return;
        }
        // 只能删除当前 panel 下的 charts 这里调用查询方法，保证查询出来的chart 是有序的
        List<Chart> chartList = this.queryCharts(panelId, null, null);
        Map<Integer, Integer> idAndChartMap = new HashMap<>();
        for (Chart entity : chartList) {
            idAndChartMap.put(entity.getId(), 1);
        }

        List<Integer> deleteIds = new ArrayList<>();
        for (Integer id : ids) {
            if (idAndChartMap.get(id) != null) {
                deleteIds.add(id);
            }
        }

        if (CollectionUtils.isNotEmpty(deleteIds)) {
            // 删除自身
            this.baseMapper.deleteByIds(panelId, deleteIds);

            // 删除char element
            chartElementService.delElementByChartIds(deleteIds);

            // 处理删除后的位置问题
            // 全删除了
            if (chartList.size() == deleteIds.size()) {
                return;
            }

            // 保证原先次序 并且 删除之后剩余的集合
            List<Chart> deletedChartList = new ArrayList<>();
            for (Chart obj : chartList) {
                if (!deleteIds.contains(obj.getId())) {
                    deletedChartList.add(obj);
                }
            }

            Chart firstChart = deletedChartList.get(0);
            firstChart.setPrev(0);

            // 剩余一个
            if (deletedChartList.size() == 1) {
                firstChart.setNext(-1);
                this.updateById(firstChart);
            } else {
                // 剩余数量大于 1
                // 最后批量更新的list
                List<Chart> updateCharts = new ArrayList<>();

                Chart tempChart = null;
                int size = deletedChartList.size();
                for (int i = 0; i < size; i++) {
                    if (i == 0) {
                        firstChart.setNext(deletedChartList.get(i + 1).getId());
                        updateCharts.add(firstChart);
                    } else {
                        tempChart = deletedChartList.get(i);
                        tempChart.setPrev(updateCharts.get(updateCharts.size() - 1).getId());
                        if (i < size - 1) {
                            tempChart.setNext(deletedChartList.get(i + 1).getId());
                        }
                        updateCharts.add(tempChart);
                    }
                }
                updateCharts.get(updateCharts.size() - 1).setNext(-1);
                this.saveOrUpdateBatch(updateCharts);
            }
        }
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


    /**
     * cahrts 调整位置接口
     *
     * @param panelId
     * @param chart
     */
    @Override
    public void modify(Integer panelId, Chart chart) {
        // 取出改panel下所有的charts
        List<Chart> chartList = this.list(new QueryWrapper<Chart>().eq("panel_id", panelId).orderByAsc("prev"));

        // 就只有一个图表 不让其调整位置
        if (chartList.size() <= 1) {
            throw new NZException(RCode.CHART_CANNOT_MODIFY);
        }

        Map<Integer, Chart> idAndChartMap = new HashMap<>();
        for (Chart chart1 : chartList) {
            idAndChartMap.put(chart1.getId(), chart1);
        }

        Chart oldChart = idAndChartMap.get(chart.getId());

        // chart不存在于当前panel
        if (oldChart == null) {
            throw new NZException(RCode.CHART_NOTEXSITS);
        }

        // 校验 prev next 参数是否合法
        this.validatePrevAndNext(idAndChartMap, chart);

        Integer firstIndex = 0;
        Integer lastIndex = -1;
        // 最后需要更改的实体集合
        List<Chart> updateCharts = new ArrayList<>();

        Integer id = chart.getId();
        // 判断旧位置是否为第一位 并且移动了位置
        if (firstIndex.equals(oldChart.getPrev()) && !firstIndex.equals(chart.getPrev())) {
            // 新的第一位
            Chart newFirstIndex = idAndChartMap.get(oldChart.getNext());
            newFirstIndex.setPrev(firstIndex);
            // 第一种 移动到末尾
            if (lastIndex.equals(chart.getNext())) {
                // 新的倒数第二位
                Chart newCountdownSecond = idAndChartMap.get(chart.getPrev());
                newCountdownSecond.setNext(id);

                updateCharts.add(newCountdownSecond);
            } else {
                // 第二种 不是移动到最后 而是移动到中间位置 即 有前有后
                // 移动后 前面的
                Chart newPrev = idAndChartMap.get(chart.getPrev());
                // 移动后 后面的
                Chart newNext = idAndChartMap.get(chart.getNext());
                newPrev.setNext(id);
                newNext.setPrev(id);
                updateCharts.add(newPrev);
                updateCharts.add(newNext);
            }
            updateCharts.add(chart);
            updateCharts.add(newFirstIndex);
        }

        // 原来是末尾 并且移动了位置
        if (lastIndex.equals(oldChart.getNext()) && !lastIndex.equals(chart.getNext())) {

            // 新的最后一位
            Chart newLastIndex = idAndChartMap.get(oldChart.getPrev());
            newLastIndex.setNext(lastIndex);
            // 第一种 末尾移动到了首位
            if (firstIndex.equals(chart.getPrev())) {
                // 新的第二位
                Chart newSecond = idAndChartMap.get(chart.getNext());
                newSecond.setPrev(id);

                updateCharts.add(newSecond);
            } else {
                // 第二种 不是移动到开始 而是移动到中间位置 即 有前有后
                // 移动后 前面的
                Chart newPrev = idAndChartMap.get(chart.getPrev());
                // 移动后 后面的
                Chart newNext = idAndChartMap.get(chart.getNext());
                newPrev.setNext(id);
                newNext.setPrev(id);
                updateCharts.add(newPrev);
                updateCharts.add(newNext);
            }

            updateCharts.add(chart);
            updateCharts.add(newLastIndex);
        }

        // 原来是中间的情况
        if (!firstIndex.equals(oldChart.getPrev()) && !lastIndex.equals(oldChart.getNext())) {
            boolean flag = false;

            // 原来位置的前后
            Chart oldPrevChart = idAndChartMap.get(oldChart.getPrev());
            Chart oldNextChart = idAndChartMap.get(oldChart.getNext());
            // 第一种 移动到了开头
            if (firstIndex.equals(chart.getPrev())) {
                // 新的第二位
                Chart newSecondIndex = idAndChartMap.get(chart.getNext());
                newSecondIndex.setPrev(id);

                if (oldPrevChart.getId().equals(newSecondIndex.getId())) {
                    flag = true;
                    newSecondIndex.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(newSecondIndex.getId());
                } else {
                    oldPrevChart.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(oldPrevChart.getId());
                }

                updateCharts.add(chart);
                updateCharts.add(newSecondIndex);
                updateCharts.add(oldNextChart);
                if (!flag) {
                    updateCharts.add(oldPrevChart);
                }
            } else if (lastIndex.equals(chart.getNext())) {
                // 第二种移动到末尾
                // 新的倒数第二位
                Chart newCountdownSecond = idAndChartMap.get(chart.getPrev());
                newCountdownSecond.setNext(id);

                // 原来位置的前后
                if (oldNextChart.getId().equals(newCountdownSecond.getId())) {
                    flag = true;
                    newCountdownSecond.setPrev(oldPrevChart.getId());
                    oldPrevChart.setNext(newCountdownSecond.getId());
                } else {
                    oldPrevChart.setNext(oldNextChart.getId());
                    oldNextChart.setPrev(oldPrevChart.getId());
                }

                updateCharts.add(chart);
                updateCharts.add(newCountdownSecond);
                updateCharts.add(oldPrevChart);

                if (!flag) {
                    updateCharts.add(oldNextChart);
                }
            } else {
                // 第三种 中间调整到中间
                Chart newPrevChart = idAndChartMap.get(chart.getPrev());
                Chart newNextChart = idAndChartMap.get(chart.getNext());

                oldPrevChart.setNext(oldNextChart.getId());
                oldNextChart.setPrev(oldPrevChart.getId());

                newNextChart.setPrev(id);

                // 换位前后面的 和 换位后前面的是同一个
                if (oldNextChart.getId().equals(newPrevChart.getId())) {
                    flag = true;
                    oldNextChart.setNext(id);
                } else {
                    newPrevChart.setNext(id);
                }

                updateCharts.add(oldPrevChart);
                updateCharts.add(oldNextChart);
                updateCharts.add(newNextChart);

                if (!flag) {
                    updateCharts.add(newPrevChart);
                }
                updateCharts.add(chart);
            }
        }

        // 更新库中数据
        if (CollectionUtils.isNotEmpty(updateCharts)) {
            this.saveOrUpdateBatch(updateCharts);
        }
    }

    /**
     * 校验参数是否合法
     *
     * @param idAndChartMap
     * @param chart
     */
    private void validatePrevAndNext(Map<Integer, Chart> idAndChartMap, Chart chart) {

        Integer prev = chart.getPrev();
        Integer next = chart.getNext();

        Integer firstIndex = 0;
        Integer lastIndex = -1;
        // 移动至开头的
        if (firstIndex.equals(prev)) {
            // 校验 next 参数值是否正确
            Chart oldFirstChart = idAndChartMap.get(next);
            if (oldFirstChart == null || !firstIndex.equals(oldFirstChart.getPrev())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        } else if (lastIndex.equals(next)) { // 移动至末尾的
            // 校验 prev 参数是否正确
            Chart oldLastChart = idAndChartMap.get(prev);
            if (oldLastChart == null || !lastIndex.equals(oldLastChart.getNext())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        } else {
            // 移动到中间的
            Chart oldMiddlePrev = idAndChartMap.get(prev);
            Chart oldMiddleNext = idAndChartMap.get(next);
            if (oldMiddlePrev == null || oldMiddleNext == null) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }

            // 判断是否连续
            if (!oldMiddlePrev.getNext().equals(oldMiddleNext.getId())) {
                throw new NZException(RCode.CHART_PREVNEXT_INCORRECT);
            }
        }
    }

}
