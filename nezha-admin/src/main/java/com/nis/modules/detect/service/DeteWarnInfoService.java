package com.nis.modules.detect.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.entity.DeteWarnInfoEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface DeteWarnInfoService extends DeteCommonService<DeteWarnInfoEntity> {
    void saveEntity(DeteWarnInfoEntity deteWarnInfoEntity,MultipartFile file);

    void updateEntity(DeteWarnInfoEntity deteWarnInfoEntity,MultipartFile file);

    void deleteEntity(Long[] ids);

    PageUtils queryPage(Map<String, Object> params);

    DeteWarnInfoEntity queryDetail(Long id);

    void deleteByTypeId(Long typeId);

     boolean checkTriggerTypeIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

     boolean checkWarnModeIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

     boolean checkStatTypeIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

     boolean checkSymbolIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

     boolean checkWarnLevelIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

     boolean checkCallbackTypeIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

     boolean checkCallbackContentIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

    boolean checkThresholdIsValide(DeteWarnInfoEntity deteWarnInfoEntity);

    boolean checkCallbackScirptFileIsValide(DeteWarnInfoEntity deteWarnInfoEntity, MultipartFile file);
}
