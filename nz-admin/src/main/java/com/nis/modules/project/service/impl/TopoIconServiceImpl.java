package com.nis.modules.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.project.dao.TopoIconDao;
import com.nis.modules.project.entity.TopoIcon;
import com.nis.modules.project.service.TopoIconService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;


@Slf4j
@Service("topoIconService")
public class TopoIconServiceImpl extends ServiceImpl<TopoIconDao, TopoIcon> implements TopoIconService {

    @Autowired
    private TopoIconDao topoIconDao;

    @Override
    public List<TopoIcon> queryIconList(Map<String, Object> params) {
        List<TopoIcon> topoIcons = topoIconDao.queryIconList(params);
        return topoIcons;
    }

    @Override
    public Integer saveTopoIcon(String name, MultipartFile file) throws IOException {
        // 校验文件类型
        this.validateIconFileType(file);
        String filename = file.getOriginalFilename();
        String fileType = filename.substring(filename.lastIndexOf(".") + 1);

        TopoIcon topoIcon = new TopoIcon();
        topoIcon.setName(name);
        topoIcon.setType(fileType);
        topoIcon.setBytes(Base64.getEncoder().encodeToString(file.getBytes()));
        this.save(topoIcon);
        return topoIcon.getId();
    }

    @Override
    public void removeIcons(Integer[] ids) {
        List<Integer> idList = Arrays.asList(ids);
        this.removeByIds(idList);
    }

    @Override
    public void downloadIconFileById(Integer id, HttpServletResponse response) {
        TopoIcon icon = this.getById(id);
        if (ToolUtil.isEmpty(icon)) {
            throw new NZException(RCode.PROJECT_ICON_NOTFOUND);
        }

        byte[] bytes = Base64.getDecoder().decode(icon.getBytes());

        try (OutputStream os = new BufferedOutputStream(response.getOutputStream());) {
            String contentType = "jpg".equals(icon.getType().toLowerCase()) ? "jpeg" : icon.getType().toLowerCase();
            response.setContentType("image/" + contentType);
            response.setHeader("Content-Disposition", "inline");
            os.write(bytes);
            os.flush();
        } catch (IOException e) {
            log.error(String.format("拓扑图图标预览失败，参数是：%s,错误信息是：%s", id, e));
        }
    }


    private void validateIconFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (!filename.contains(".")) {
            throw new NZException(RCode.PROJECT_TOPOICON_FILETYPE_ERROR);
        }

        String fileType = filename.substring(filename.lastIndexOf(".") + 1);
        fileType = fileType.toLowerCase();
        switch (fileType){
            case "jpg":break;
            case "jpeg":break;
            case "png":break;
            case "gif":break;
            default:
                throw new NZException(RCode.PROJECT_TOPOICON_FILETYPE_ERROR);
        }
    }
}