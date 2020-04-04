package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.sys.entity.SnmpMib;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface SnmpMibService extends IService<SnmpMib> {

    PageUtils queryPage(Map<String, Object> params);

    void saveOrUpdateSnmpMib(SnmpMib snmpMib, MultipartFile file);

    String getMibTrees(String[] ids, String[] models);
}
