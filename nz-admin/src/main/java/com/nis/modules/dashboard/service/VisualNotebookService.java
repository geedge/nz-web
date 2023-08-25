package com.nis.modules.dashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.dashboard.entity.VisualNotebook;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * visual notebook service
 *
 * @author shizhendong
 * @date 2023-08-16
 */
public interface VisualNotebookService extends IService<VisualNotebook> {

    VisualNotebook queryNotebookInfo(Integer id);

    PageUtils queryNotebookPage(Map<String, Object> params);

    Integer saveOrUpdateNotebook(VisualNotebook notebook);

    void duplicateNotebook(Integer id, String name);

    void deleteNotebook(List<Integer> idList);

    Map<Object, Object> importNotebook(MultipartFile jsonFile, Integer existed, Integer ignoreError);

    void exportNotebook(HttpServletResponse response, Map<String, Object> params);

}
