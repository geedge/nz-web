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

@RestController
@RequestMapping("/ctl")
public class CtlController extends AbstractController{
	
	@Autowired
	private CtlService ctlService;
	
	
	@GetMapping("/aes")
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.CTL)
	public R ase(@RequestParam String content){
		Map<String, String> result = ctlService.ase(content);
		return R.ok(result);
	}
	
	@PutMapping("/resetAdmin")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.CTL)
	public R resetAdmin(HttpServletRequest request) {
		String ipAddr = IPUtils.getIpAddr(request);
		LinkedHashSet<String> localIpv4s = Tool.NetUtil.localIpv4s();
		LinkedHashSet<String> localIpv6s = Tool.NetUtil.localIpv6s();
		Map<String, Object> result = null;
		if(localIpv4s.contains(ipAddr) || localIpv6s.contains(ipAddr)) {
			result = ctlService.resetAdmin();
		}
		return R.ok(result);
	}
}
