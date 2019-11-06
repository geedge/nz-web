package com.nis.modules.detect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface DeteCommonService<T> extends IService<T>{
    boolean checkFieldExists(Long id,String field,String value);
    String formatString(String tableName);

    boolean checkNameExists(String name,Long id);
    boolean checkFieldIsValide(String type,String code);
    boolean checkViewLevelValide(String code);
    boolean checkTypeExists(Long typeId);

    String getUploadFileJson(MultipartFile file);
    void uploadFile(String jsonInfo,MultipartFile file);
    void delUploadFile(String jsonInfo);
}
