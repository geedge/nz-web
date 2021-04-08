package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.panel.entity.Chart;
import com.nis.modules.panel.entity.VisualChart;
import com.nis.modules.terminal.entity.Account;
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
public interface AssetService extends IService<Asset> {

    PageUtils queryPage(Map<String, Object> params);

    void saveOrUpdateAsset(Asset asset);

    void delete(Integer ... ids);

    void validateAccount(List<Account> accounts);
    
    List<MetricsDto> queryAssetStatisticsByModel();
    
    List<MetricsDto> queryAssetStatisticsByModelOnLine();
    
    Map importAssetsFromExcel(MultipartFile assetsExcelFile, String seqUUID, String language);

    void cancelImport(String seq);

    R uploadBuildExporter(Map<String, String> configs, Asset asset, Account accountCli);

    Map detail(Integer id);

    void export(HttpServletResponse response, Map<String, Object> params);

    void collectData(Integer assetId);

    Map filterStat();

    void getExportTemplateByLanguage(HttpServletResponse response, String language);

    List<String> getTagsBykeyOrderCounts(String key);

    List<String> getTagValsByKeyOrderCounts(String key, String value);

    List<Asset> getAssetsWithTagsByModelId(Integer modelId);

    List<VisualChart> getAssetsWithTagByChartIds(List<Integer> chartIds);

    void updateBatchAccountAndTagByAssetIds(Map<String, Object> param);
}