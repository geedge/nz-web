package com.nis.common.update.v3_2_to_3_3.service.impl;

import cn.hutool.log.Log;
import com.nis.common.update.v3_2_to_3_3.dao.V3_2_AssetModelDao;
import com.nis.common.update.v3_2_to_3_3.service.V3_3_DataFormatConversionService;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.dao.AssetModelDao;
import com.nis.modules.asset.entity.AssetModel;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetTypeConfService;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class V3_3_DataFormatConversionServiceImpl implements V3_3_DataFormatConversionService {

    private static Log logger = Log.get();

    @Autowired
    private V3_2_AssetModelDao v3_2_assetModelDao;

    @Autowired
    private AssetAssetDao assetAssetDao;

    @Autowired
    private AssetTypeConfService typeConfService;

    @Autowired
    private AssetModelDao assetModelDao;

    @Override
    public void assetModelDataFormatConversion() {
        // 1. 检查 asset_model_v3_2 是否存在
        Map checkTableExists = v3_2_assetModelDao.checkTableExists("asset_model_v3_2");
        if (Tool.CollectionUtil.isEmpty(checkTableExists)) {
            logger.info("Table asset_model_v3_2 does not exist. The asset_model data format processing will not be executed");
            return;
        }

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // 2. 处理数据变化
            logger.info("The asset_model data format change processing start.");

            List<AssetModel> oldAssetModels = v3_2_assetModelDao.queryList();
            Map<Integer, AssetModel> oldAssetModelMap = oldAssetModels.stream().collect(Collectors.toMap(AssetModel::getId, Function.identity()));

            List<AssetTypeConf> typeConfList = typeConfService.list();
            Map<Integer, String> typeConfMap = typeConfList.stream().collect(Collectors.toMap(AssetTypeConf::getId, AssetTypeConf::getName));

            // 统计 asset_asset 表 model_id 与 type_id 关联关系      注：未被 asset 引用的 model 数据直接删除，不再保留
            List<Map<String, Object>> modelIdAndTypeIdsMapList = v3_2_assetModelDao.queryAssetModelIdAndTypesMapping();
            Map<Integer, String> modelIdAndTypeIdssMap = modelIdAndTypeIdsMapList.stream().collect(Collectors.toMap(o -> Integer.valueOf(o.get("modelId").toString()), o -> o.get("typeId").toString()));

            //  保存之前的数据到 新表中
            for (Map.Entry<Integer, String> entry : modelIdAndTypeIdssMap.entrySet()) {
                Integer oldModelId = entry.getKey();
                String typeIds = entry.getValue();
                // 判断 typeids 是否包含 , 包含则证明 部分 asset 选择了多个 type 对应同一个 model
                if (!typeIds.contains(",")) {
                    // typeids 未包含 , asset 引用 model_id 和 type_id 一一对应，修改 model 对应 type_id 为该 type_id 即可
                    AssetModel assetModel = oldAssetModelMap.get(oldModelId);
                    assetModel.setTypeId(Integer.valueOf(typeIds));
                    assetModel.setSysObjectId(Tool.StrUtil.EMPTY);
                    assetModelDao.insertModel(assetModel);
                }
            }

            for (Map.Entry<Integer, String> entry : modelIdAndTypeIdssMap.entrySet()) {
                Integer oldModelId = entry.getKey();
                String typeIds = entry.getValue();
                // 判断 typeids 是否包含 , 包含则证明 部分 asset 选择了多个 type 对应同一个 model
                if (typeIds.contains(",")) {
                    // 新增 model 并 处理 asset 对应关系
                    List<Integer> typeIdList = Tool.StrUtil.split(typeIds, ',').stream().map(Integer::parseInt).collect(Collectors.toList());
                    for (Integer typeId : typeIdList) {
                        // 新增 model name = oldModelName-typeConfName
                        String typeConfName = typeConfMap.get(typeId);
                        AssetModel assetModel = oldAssetModelMap.get(oldModelId);

                        AssetModel saveAssetModel = new AssetModel();
                        Tool.BeanUtil.copyProperties(assetModel, saveAssetModel);
                        saveAssetModel.setId(null);
                        saveAssetModel.setTypeId(typeId);
                        saveAssetModel.setName(String.format("%s-%s", saveAssetModel.getName(), typeConfName));
                        saveAssetModel.setSysObjectId(Tool.StrUtil.EMPTY);
                        assetModelDao.insert(saveAssetModel);

                        // 修改 asset 对应 model_id  sql: update asset_asset set model_id = {newModelID} where model_id = {oldModelID} and type_id = {typeID}
                        assetAssetDao.updateModelId(saveAssetModel.getId(), oldModelId, typeId);
                    }
                }
            }

            logger.info("The asset_model data format change processing done.");
            // 4. 删除 asset_model_v3_2 表
            v3_2_assetModelDao.dropTable("asset_model_v3_2");
        } catch (Exception e) {
            logger.error("asset_model 数据转换有误", e);
        } finally {
            sw.stop();
        }
        logger.info("asset_model data format conversion done Run Time: {}", sw.toString());
    }

}

