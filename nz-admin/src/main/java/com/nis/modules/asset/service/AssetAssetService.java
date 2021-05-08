package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.panel.entity.VisualChart;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 资产 service
 *
 * @author shizhendong
 * @date 2019-11-21
 */
public interface AssetAssetService extends IService<AssetAsset> {

    AssetAsset queryAssetInfo(Integer id);

    PageUtils queryAssetPage(Map<String, Object> params);

    Integer saveAsset(AssetAsset asset);

    Integer updateAsset(AssetAsset asset);

    void deleteAsset(Integer... ids);

//    Map alertStatByAsset(Integer assetId);

    Map endpointStatByAsset(Integer id);

    Map featureStatByAsset(Integer assetId);

    Map queryParams();

    List<AssetFieldMeta> queryAssetFieldValue(Integer assetId, Integer metaId);

    void getExportTemplateByLanguage(HttpServletResponse response, String language);

    void collectData(Integer assetId);

    void export(HttpServletResponse response, Map<String, Object> params);

    Map importAssetsFromExcel(MultipartFile excelFile, String language);

    void cancelImport(String seq);

    void bulkEdit(Map<String,Object> params);
    /**
     * 获取 blackbox 所需 asset 详情信息
     * @return
     */
    List<AssetAsset> getBlackboxAssetInfoList();

    List<VisualChart> getAssetsWithTagByChartIds(List<Integer> idList);
}
