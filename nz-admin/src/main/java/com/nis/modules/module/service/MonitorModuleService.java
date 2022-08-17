package com.nis.modules.module.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MonitorModuleService extends IService<MonitorModule> {

    MonitorModule queryModuleInfo(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    List<MonitorProject> queryModuleTree(String name);

    Integer saveModule(MonitorModule module);

    Integer updateModule(MonitorModule module);

    void removeModules(Integer... ids);

    void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

    void cancelImport(String seq);

    Map importModuleData(MultipartFile excelFile) throws IOException;

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    List<AssetAsset> getAssetListByModuleId(Integer moduleId);

    List<Dc> getDcListByModuleId(Integer moduleId);

    String queryAssetMoudules(Integer assetId);

    void deleteModuleChartTemps(List<Integer> chartIdList);
}
