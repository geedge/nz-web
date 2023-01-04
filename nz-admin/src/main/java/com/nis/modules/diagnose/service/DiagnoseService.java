package com.nis.modules.diagnose.service;

import java.util.List;
import java.util.Map;


/**
 * DiagnoseService 故障排查
 *
 * @author shizhendong
 * @date 2023/1/4
 */
public interface DiagnoseService {

    /**
     * asset Diagnosis
     *
     * @param assetId
     * @param language
     * @return
     */
    List<Map<Object, Object>> assetErrorDiagnosis(Integer assetId, String language);

}

