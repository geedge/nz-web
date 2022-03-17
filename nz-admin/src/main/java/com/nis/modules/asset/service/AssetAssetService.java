package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.project.entity.MonitorProject;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    Map<String,List> queryAssetGroupBy(Map<String, Object> params);
    
    Integer saveAsset(AssetAsset asset);

    Integer updateAsset(AssetAsset asset);

    void deleteAsset(Integer... ids);

//    Map alertStatByAsset(Integer assetId);

    Map endpointStatByAsset(Integer id);

    Map featureStatByAsset(Integer assetId);

    Map queryParams();

    List<AssetFieldMeta> queryAssetFieldValue(Integer assetId, Integer metaId);

    void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

    void collectData(Integer assetId);

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    Map importAssetsData(MultipartFile excelFile) throws IOException;

    void cancelImport(String seq);

    void bulkEdit(Map<String,Object> params);
    /**
     * 获取 blackbox 所需 asset 详情信息
     * @return
     */
    List<AssetAsset> getBlackboxAssetInfoList();

    /**
     * 获取 开启 ping 的 asset
     *
     * @return
     */
    List<AssetAsset> getOpenPingAssets();

    List<VisualChart> getAssetsWithTagByChartIds(List<Integer> idList);

    /**
     * 根据ip获取 asset
     * @param ip
     * @return
     */
    AssetAsset getAssetByIP(String ip);

    /**
     * 通过 assetid 获取 endpoint 列表
     * @param assetId
     * @return
     */
    List<MonitorEndpoint> getEndpointListByAssetId(Integer assetId);

    /**
     * 通过 assetid 获取 Module 列表
     * @param assetId
     * @return
     */
    List<MonitorModule> getModuleListByAssetId(Integer assetId);

    /**
     * 通过 assetid 获取 Project 列表
     * @param assetId
     * @return
     */
    List<MonitorProject> getProjectListByAssetId(Integer assetId);
}
