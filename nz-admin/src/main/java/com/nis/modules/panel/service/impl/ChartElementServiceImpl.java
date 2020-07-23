package com.nis.modules.panel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.panel.dao.ChartElementDao;
import com.nis.modules.panel.entity.ChartElement;
import com.nis.modules.panel.service.ChartElementService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChartElementServiceImpl extends ServiceImpl<ChartElementDao, ChartElement> implements ChartElementService {

    @Autowired
    private SysDictService sysDictService;

    @Override
    public void validateElements(List<ChartElement> elements) {
        List<SysDictEntity> list = sysDictService.querySysDicByType(Constant.DICT_EXPRESSION_TYPE);
        List<String> expressionTypes = list.stream().map(SysDictEntity::getValue).collect(Collectors.toList());

        for (ChartElement element : elements) {
            ValidateUtils.is(element.getExpression()).notNull(RCode.CHARTELEMENT_EXPRESSION_ISNULL)
                    .and(element.getType()).notNull(RCode.CHARTELEMENT_TYPE_ISNULL);

            if (!expressionTypes.contains(element.getType())) {
                throw new NZException(RCode.CHARTELEMENT_TYPE_INVALIDE);
            }
        }
    }

    @Override
    public void delElementByChartIds(List<Integer> ids) {
        if(ToolUtil.isEmpty(ids)){
            return ;
        }

        this.remove(new QueryWrapper<ChartElement>().lambda().in(ChartElement::getChartId, ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateElements(Integer chartId, List<ChartElement> chartElements) {
        if(ToolUtil.isEmpty(chartElements)){
            chartElements = new ArrayList<>();
        }
        // 1. 查询当前 chart 下所有的 chartElements
        List<ChartElement> list = this.list(new QueryWrapper<ChartElement>().lambda().eq(ChartElement::getChartId, chartId));

        List<ChartElement> saveList = new ArrayList<>();
        List<ChartElement> updateList = new ArrayList<>();

        // 2. 通过 id 判断是否 新增/修改
        for (ChartElement element : chartElements) {
            // 设置 关联 chart id
            element.setChartId(chartId);

            if (element.getId() == null) {
                saveList.add(element);
            } else {
                updateList.add(element);
            }

            // 3. 数据库存在 element 列表中没有的 element，删除
            if (list.contains(element)) {
                list.remove(element);
            }
        }
        if (saveList.size() > 0) {
            this.saveBatch(saveList);
        }
        if (updateList.size() > 0) {
            this.updateBatchById(updateList);
        }
        if (list.size() > 0) {
            List<Integer> elementIds = list.stream().map(ChartElement::getId).collect(Collectors.toList());
            this.removeByIds(elementIds);
        }
    }
}
