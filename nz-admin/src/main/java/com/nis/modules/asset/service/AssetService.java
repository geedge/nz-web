package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.metric.dto.MetricsDto;
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

    void validateAccount(Asset asset, List<Account> accounts);
    
    List<MetricsDto> queryAssetStatisticsByModel();
    
    List<MetricsDto> queryAssetStatisticsByModelOnLine();
    
    MetricsDto queryAssetStatisticsByModelOffLine();

    Map importAssetsFromExcel(MultipartFile assetsExcelFile, String seqUUID);

    void cancelImport(String seq);

    R uploadBuildExporter(Map<String, String> configs, Asset asset, Account accountCli);

    Map detail(Integer id);

    void export(HttpServletResponse response, Map<String, Object> params);

    void collectData(Integer assetId);

    Map filterStat();
}