package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.Cabinet;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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
     * cabinet 删除方法
     * @param force 0：存在关联数据时，删除失败 1：存在关联数据时，同时删除关联数据 默认：0
     * @param ids
     * @return
     */
    List<Map> deleteByIds(Integer force, Integer... ids);

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

