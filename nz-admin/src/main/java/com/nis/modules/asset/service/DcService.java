package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 数据中心 service
 *
 * @author shizhendong
 * @date 2021-4-6
 */
public interface DcService extends IService<Dc> {

    /**
     * 详情
     * @param id
     * @return
     */
    Dc queryDcDetails(Integer id);

    /**
     * 分页查询
     * @param params
     * @return
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 新增
     * @param dc
     * @return
     */
    Integer saveDc(Dc dc);

    /**
     * 修改
     * @param dc
     * @return
     */
    Integer updateDc(Dc dc);

    /**
     * 删除
     * @param ids
     */
    void delete(Integer... ids);

    void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

    Map importDcData(MultipartFile excelFile) throws IOException;

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    void cancelImport(String seq);

    /**
     * 通过 dcId 获取 AssetAsset 列表
     * @param dcId
     * @return
     */
    List<AssetAsset> getAssetListByDcId(Integer dcId);

    List<MonitorProject> getProjectListByDcId(Integer dcId);

    List<MonitorModule> getModuleListByDcId(Integer dcId);

    List<MonitorEndpoint> getEndpointListByDcId(Integer dcId);
}

