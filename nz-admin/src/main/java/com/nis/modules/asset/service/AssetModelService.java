package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetModel;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 资产型号 service
 *
 * @author shizhendong
 * @date 2021-03-16
 */
public interface AssetModelService extends IService<AssetModel> {

    AssetModel queryModelInfo(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    Integer saveModel(AssetModel model);

    Integer updateModel(AssetModel model);

    /**
     * asset model 删除方法
     * @param force 0：存在关联数据时，删除失败 1：存在关联数据时，同时删除关联数据 默认：0
     * @param ids
     * @return
     */
    List<Map> delete(Integer force, Integer... ids);

    void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

    Map importAssetModelData(MultipartFile excelFile, Map<String, Object> params) throws IOException;

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    void cancelImport(String seq);

}
