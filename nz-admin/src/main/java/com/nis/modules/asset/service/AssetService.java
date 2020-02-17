package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.metric.dto.MetricsDto;
import com.nis.modules.romote.entity.Account;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
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
    
    List<Asset> queryListByHosts(Collection<String> hosts);

    void saveOrUpdateAsset(Asset asset, MultipartFile[] sshKeys);

    void delete(Integer ... ids);

    void validateAccount(Account account);
    
    List<MetricsDto> queryAssetStatisticsByModel();
    
    List<MetricsDto> queryAssetStatisticsByModelOnLine();
    
    MetricsDto queryAssetStatisticsByModelOffLine();

    Map importAssetsFromExcel(MultipartFile assetsExcelFile, String seqUUID);

    void cancelImport(String seq);
}