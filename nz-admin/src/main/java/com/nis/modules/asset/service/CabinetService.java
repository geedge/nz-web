package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.Cabinet;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 机柜 service
 *
 * @author shizhendong
 * @date 2019-11-21
 */
public interface CabinetService extends IService<Cabinet> {

    /**
     * 详情
     * @param id
     * @return
     */
    Cabinet queryCabinetDetails(Integer id);

    /**
     * 分页列表
     * @param params
     * @return
     */
    PageUtils queryCabinetPage(Map<String, Object> params);

    /**
     * 保存
     * @param cabinet
     * @return
     */
    Integer saveCabinet(Cabinet cabinet);

    /**
     * 修改
     * @param cabinet
     * @return
     */
    Integer updateCabinet(Cabinet cabinet);

    /**
     * 删除
     * @param ids
     */
    void deleteByIds(Integer... ids);

    /**
     * 机柜u位查询
     * @param id
     * @return
     */
    Map getCabinetUsizeDetails(Integer id);

    void getCabinetTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

    Map importCabinetData(MultipartFile excelFile) throws IOException;

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    void cancelImport(String seq);
}

