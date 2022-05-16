package com.nis.modules.promserver.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.promserver.entity.Promserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface LokiserverService extends IService<Promserver> {

    Map lokiServerProxy(HttpServletRequest request, HttpServletResponse response, String proxyPath, Map<String, String> paramData);
}
