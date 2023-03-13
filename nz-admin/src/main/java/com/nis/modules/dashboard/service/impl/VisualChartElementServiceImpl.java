package com.nis.modules.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.dashboard.dao.VisualChartElementDao;
import com.nis.modules.dashboard.entity.VisualChartElement;
import com.nis.modules.dashboard.service.VisualChartElementService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("visualChartElementService")
public class VisualChartElementServiceImpl extends ServiceImpl<VisualChartElementDao, VisualChartElement> implements VisualChartElementService {

    @Autowired
    private SysDictService sysDictService;

    @Override
    public void validateElements(List<VisualChartElement> elements) {
        List<SysDictEntity> list = sysDictService.querySysDicByType(Constant.DICT_EXPRESSION_TYPE);
        List<String> expressionTypes = list.stream().map(SysDictEntity::getValue).collect(Collectors.toList());

        for (VisualChartElement element : elements) {
            ValidateUtils.is(element.getExpression()).notNull(RCode.CHARTELEMENT_EXPRESSION_ISNULL).maxLength(4096)
                    .and(element.getType()).notNull(RCode.CHARTELEMENT_TYPE_ISNULL)
                    .and(element.getLegend()).maxLength(512)
                    .and(element.getName()).notNull(RCode.CHARTELEMENT_NAME_ISNULL).maxLength(64);

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

        this.remove(new QueryWrapper<VisualChartElement>().lambda().in(VisualChartElement::getChartId, ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateElements(Integer chartId, List<VisualChartElement> chartElements) {
        if(ToolUtil.isEmpty(chartElements)){
            chartElements = new ArrayList<>();
        }
        // 1. 查询当前 chart 下所有的 chartElements
        List<VisualChartElement> list = this.list(new QueryWrapper<VisualChartElement>().lambda().eq(VisualChartElement::getChartId, chartId));
        List<Integer> elementIds = list.stream().map(VisualChartElement::getId).collect(Collectors.toList());
        
        List<VisualChartElement> saveList = new ArrayList<>();
        List<VisualChartElement> updateList = new ArrayList<>();

        
        
        // 2. 通过 id 判断是否 新增/修改
        for (VisualChartElement element : chartElements) {
            // 设置 关联 chart id
            element.setChartId(chartId);

            if (element.getId() == null) {
                saveList.add(element);
            } else {
                updateList.add(element);
            }

            // 3. 数据库存在 element 列表中没有的 element，删除
            if (elementIds.contains(element.getId())) {
            	elementIds.remove(element.getId());
            }
        }
        if (saveList.size() > 0) {
            this.saveBatch(saveList);
        }
        if (updateList.size() > 0) {
            this.updateBatchById(updateList);
        }
        if (elementIds.size() > 0) {
            this.removeByIds(elementIds);
        }
    }
}
