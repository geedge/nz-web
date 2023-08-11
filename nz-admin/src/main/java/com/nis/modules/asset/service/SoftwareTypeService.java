package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.asset.entity.SoftwareType;

import java.util.List;
import java.util.Map;

/**
 * Software Type Service
 *
 * @author shizhendong
 * @date 2023-08-10
 */
public interface SoftwareTypeService extends IService<SoftwareType> {

    SoftwareType querySoftwareTypeInfo(Integer id);

    PageUtils querySoftwareTypePage(Map<String, Object> params);

    Integer saveOrUpdateSoftwareType(SoftwareType softwareType);

    R deleteSoftwareType(Integer force, List<Integer> idList);

}
