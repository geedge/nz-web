package com.nis.modules.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.project.entity.TopoIcon;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public interface TopoIconService extends IService<TopoIcon> {

    List<String> queryUnitList(String name);

    void renameUnit(String oldUnitName, String newUnitName);

    void deleteIconByUnit(String unit);

    List<TopoIcon> queryIconList(Map<String, Object> params);

    Integer saveTopoIcon(TopoIcon icon, MultipartFile file);

    void removeIcons(Integer[] ids);

    void downloadIconFileById(Integer id, Integer base64, HttpServletResponse response);

    void downloadIconFile(String unit, String name, Integer base64, HttpServletResponse response);
}
