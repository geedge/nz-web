package com.nis.modules.ipam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.modules.ipam.entity.IpamSubnet;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IncompatibleAddressException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IpamSubnetService extends IService<IpamSubnet> {

	PageUtils queryPage(Map<String,Object> params);
}
