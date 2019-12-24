package com.nis.modules.panel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.panel.dao.ChartElementDao;
import com.nis.modules.panel.entity.ChartElement;
import com.nis.modules.panel.service.ChartElementService;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ChartElementServiceImpl extends ServiceImpl<ChartElementDao, ChartElement> implements ChartElementService {
    @Autowired
    private SysDictService sysDictService;

    @Override
    public void validateElements(Integer chartId, List<ChartElement> elements) {

        List<Integer> elementIds=this.baseMapper.queryElementIds(chartId);

        List<SysDictEntity> list = sysDictService.querySysDicByType(Constant.DICT_EXPRESSION_TYPE);

        List<String> expressionTypes=new ArrayList<String>();

        for (SysDictEntity sysDic:list){
            expressionTypes.add(sysDic.getValue());
        }

        for (ChartElement element:elements){
            //element是否属于chart
            if (element.getId()!=null){
                if(!elementIds.contains(element.getId())){
                    throw new NZException(RCode.CHARTELEMENT_NOTEXSITS);
                }
            }

            ValidateUtils.is(element.getExpression()).notNull(RCode.CHARTELEMENT_EXPRESSION_ISNULL);

            ValidateUtils.is(element.getType()).notNull(RCode.CHARTELEMENT_TYPE_ISNULL);

            if(!expressionTypes.contains(element.getType())){
                throw new NZException(RCode.CHARTELEMENT_TYPE_INVALIDE);
            }
        }
    }

    @Override
    public void delElementByChartIds(Integer... ids) {
        delElementByChartIds(Arrays.asList(ids));
    }

    @Override
    public void delElementByChartIds(List<Integer> ids) {
        QueryWrapper<ChartElement> elementQueryWrapper = new QueryWrapper<>();
        elementQueryWrapper.in("chart_id",ids);

        this.remove(elementQueryWrapper);
    }
}
