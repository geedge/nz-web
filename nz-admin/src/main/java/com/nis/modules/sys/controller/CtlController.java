package com.nis.modules.sys.controller;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.utils.IPUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.Link;
import com.nis.modules.sys.service.CtlService;

import cn.hutool.core.util.StrUtil;

@RestController
@RequestMapping("/ctl")
public class CtlController extends AbstractController{
	
	@Autowired
	private CtlService ctlService;
	
	@PutMapping("/resetAdmin")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.CTL)
	public R resetAdmin(HttpServletRequest request) {
		String ipAddr = IPUtils.getIpAddr(request);
		Map<String, Object> result = null;
		if(StrUtil.equals(ipAddr, "127.0.0.1") || StrUtil.equals(ipAddr, "localhost") || StrUtil.equals(ipAddr, ":::1")) {
			result = ctlService.resetAdmin();
		}
		return R.ok(result);
	}
}
