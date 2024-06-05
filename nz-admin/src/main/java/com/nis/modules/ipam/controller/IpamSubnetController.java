package com.nis.modules.ipam.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.utils.*;
import com.nis.modules.ipam.service.IpamSubnetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dc/subnet")
public class IpamSubnetController {

    @Autowired
    private IpamSubnetService ipamSubnetService;

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.IPAM_SUBNET)
	public R ipamSubnetList(@RequestParam Map<String, Object> params) {
		PageUtils page = ipamSubnetService.queryPage(params);
		return R.ok(page);
	}
    
}
