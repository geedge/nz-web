package com.nis.modules.ipam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.ipam.entity.IpamIp;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public interface IpamIpService extends IService<IpamIp> {

    IpamIp ipamIpInfo(Integer id);

    PageUtils ipamIpList(Map params);

    Integer saveOrModifyIpamSubnet(IpamIp ipamIp) throws UnknownHostException;

    void delIpamIps(List<Integer> asList);

    void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException;

    Map importIpamIpData(MultipartFile excelFile ,Map<String, Object> params) throws IOException;

    void export(HttpServletResponse response, Map<String, Object> params) throws IOException;

    void cancelImport(String seq);
}
