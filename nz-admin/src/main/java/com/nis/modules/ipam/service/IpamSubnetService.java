package com.nis.modules.ipam.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.ipam.entity.IpamSubnet;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IncompatibleAddressException;

public interface IpamSubnetService extends IService<IpamSubnet> {
	
	IpamSubnet queryInfo(Integer id);
	
	PageUtils queryPage(Map<String,Object> params);
		
	Integer saveOrModifyIpamSubnet(IpamSubnet ipamSubnet) throws AddressStringException, IncompatibleAddressException;
	
	void delIpamSubnets(List<Integer> ids);
		
	void getTemplateByLanguage(HttpServletResponse response,String format) throws IOException;
	
	Map importIpamSubnetData(MultipartFile excelFile) throws IOException,AddressStringException, IncompatibleAddressException ;
	
	void export(HttpServletResponse response, Map<String, Object> params) throws IOException;
		
	void cancelImport(String seq);
}
