package com.nis.modules.ipam.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.ipam.dao.IpamSubnetDao;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Service
public class IpamSubnetServiceImpl extends ServiceImpl<IpamSubnetDao, IpamSubnet> implements IpamSubnetService {

    protected Log log = Log.get();
    
    @Autowired
    private IpamSubnetDao ipamSubnetDao;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		
		if (ToolUtil.isNotEmpty(params.get("ids"))) {
            params.put("ids", Arrays.asList(String.valueOf(params.get("ids")).split(StringPool.COMMA)));
        }
		
		if (ToolUtil.isNotEmpty(params.get("dcIds"))) {
            params.put("dcIds", Arrays.asList(String.valueOf(params.get("dcIds")).split(StringPool.COMMA)));
        }

        // 当前登录用户
        params.put("currentLoginUserID", ShiroUtils.getUserId().intValue());

		IPage page = new Query(IpamSubnet.class).getPage(params);
		List<IpamSubnet> result = ipamSubnetDao.queryPage(page,params);
		page.setRecords(result);
        return new PageUtils(page);
	}

}
