package com.nis.modules.promserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.promserver.entity.Promserver;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.Map;

public interface LokiserverService extends IService<Promserver> {

    void lokiServerProxy(HttpServletRequest request, HttpServletResponse response, String proxyPath);

    ResponseEntity<byte[]> lokiServerData(HttpServletRequest request,Integer promId,String proxyPath,byte[] body,String queryString);

    Map handleResultData(InputStream connInputStream, Map<String, String> paramData);
}
