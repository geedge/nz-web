package com.nis.common.update.v3_5_to_3_6;

import cn.hutool.log.Log;
import com.nis.common.update.VersionUpdateInterface;
import com.nis.common.update.v3_5_to_3_6.service.V3_6_DataFormatConversionService;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * V3_6Update
 * 支持自动将 已有asset model 配置的 chart template 转换为 dashboard template 实现。
 * 1、复制 asset_model_v3.6 表格
 * 2、遍历 asset model
 *     新建 dashboard template,，name： [assetModel.name] + template, 例： TSG-X template
 *     新建 chart （复制asset model已关联）
 *     更新使用 chart template 创建的图表 pid
 *     更新asset dashboard tmpl_id
 * 3、更新 asset model dashboard id
 * 4、删除 chart.dashboard_id == 0 的历史数据
 * 5、删除 asset_model_v3.6
 *
 * @author HUAWEI
 */
@Component
public class V3_6Update implements VersionUpdateInterface {

    private static final Log log = Log.get();

    @Autowired
    private V3_6_DataFormatConversionService service;

    @Override
    public boolean preUpdate() {

        return VersionUpdateInterface.super.preUpdate();
    }

    @Override
    public void excute() throws Exception {
        log.info("[V3_6Update] [Data Format Conversion Begin]");

        StopWatch sw = new StopWatch();
        sw.start();

        // excute data Format Conversion
        service.dataFormatConversion();

        sw.stop();
        log.info("[V3_6Update] [Data Format Conversion Finshed] [Run time: {}]", sw.toString());
    }

}
