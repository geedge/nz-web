package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SysFileEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * 文件管理 service
 */
public interface SysFileService extends IService<SysFileEntity> {

    SysFileEntity querySysFileInfo(Integer id);

    PageUtils querySysFilePage(Map<String, Object> params);

    SysFileEntity sysFileUpload(MultipartFile file) throws IOException;

    void sysFileDownload(String uuid, HttpServletResponse response) throws IOException;

    void deleteSysFile(Integer... ids);
}
