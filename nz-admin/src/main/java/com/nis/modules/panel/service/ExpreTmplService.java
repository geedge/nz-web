package com.nis.modules.panel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.panel.entity.ExpressionTmpl;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 表达式模板 service
 *
 * @author shizhendong
 * @date 2021-03-11
 */
public interface ExpreTmplService extends IService<ExpressionTmpl> {

    ExpressionTmpl queryTmplInfoById(Integer id);

    PageUtils queryPage(Map<String, Object> params);

    List<String> queryTmplsByGname(String gname);

    void saveTmpl(ExpressionTmpl tmpl);

    void updateTmpl(ExpressionTmpl tmpl);

    void delete(Integer... ids);

    String renderExpression(ExpressionTmpl tmpl);

    void getExportTemplateByLanguage(HttpServletResponse response, String language);

    Map importExpreTmplsFromExcel(MultipartFile excelFile, String language);

    void export(HttpServletResponse response, Map<String, Object> params);

    void cancelImport(String seq);
}
