package com.nis.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.project.entity.TopoIcon;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TopoIconService extends IService<TopoIcon> {

    List<TopoIcon> queryIconList(Map<String, Object> params);

    Integer saveTopoIcon(TopoIcon icon, MultipartFile file) throws IOException;

    void removeIcons(Integer[] ids);

    void downloadIconFileById(Integer id, Integer base64, HttpServletResponse response);
    
    List<String> queryTopoIconUnitName(String name);

    void deleteTopoIconByUnit(String unit);
}
