package com.nis.modules.sys.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SystemInfoEntity;
import com.nis.modules.sys.service.SystemInfoService;

@RestController
@RequestMapping("/system")
public class SystemInfoController extends AbstractController {
	
	@Autowired
	private SystemInfoService systemInfoService;
	
	@RequestMapping("/list")
	public R list(@RequestParam Map<String, Object> params){
		//单独校验的方式：
		//如下面的写法就是校验usergroupId非空、page非空且最大值30
		ValidateUtils.check(params.get("usergroupId")).notNull(RCode.ERROR)
					.and(params.get("page")).notNull();

		return R.ok().put("page", null);
	}
	
	@RequestMapping("/save")
	public R save(@RequestBody SystemInfoEntity systemInfo){
		//校验实体类的方式：
		//在SystemInfoEntity的name字段上加上注解  @NotNull(msg = RCode.XXX)
		//当name为空时，程序抛出ParamsException异常，捕获它并根据RCode.XXX获取错误msg
		/*try {
			ValidateUtils.check(systemInfo);
		} catch (ParamsException e) {
			return R.error(e.getMessage());
		}*/
		
		return R.ok();
	}
}