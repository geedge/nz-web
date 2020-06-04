package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.Model;

import java.util.Map;

/**
 * 资产型号 service
 *
 * @author shizhendong
 * @date 2019-11-21
 */
public interface ModelService extends IService<Model> {

    PageUtils queryPage(Map<String, Object> params);

    void saveOrUpdateModel(Model model);

    void delete(Integer ... ids);

    boolean checkName(Integer id, String name);

    void checkUsize(Model usize);

    void syncChart(Map<String, Object> params);
}

