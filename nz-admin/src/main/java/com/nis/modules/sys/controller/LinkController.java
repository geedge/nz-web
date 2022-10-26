package com.nis.modules.sys.controller;

import cn.hutool.core.util.StrUtil;
import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.Link;
import com.nis.modules.sys.service.LinkService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/link")
public class LinkController extends AbstractController{
	@Autowired
	private LinkService linkService;
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.LINK)
	public R list(@RequestParam Map<String, Object> params){
		List<Link> result = linkService.queryList(params);
		return R.ok().putData("list",result);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.LINK)
	@RequiresPermissions({"sys:link:save"})
	public R save(@RequestBody Link link) {
		ValidateUtils.is(link.getName()).notNull(RCode.LINK_NAME_ISNULL).maxLength(64);
		ValidateUtils.is(link.getUrl()).notNull(RCode.LINK_URL_ISNULL).maxLength(1024);
		linkService.saveLink(link);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.LINK)
	@RequiresPermissions({"sys:link:update"})
	public R update(@RequestBody Link link) {
		ValidateUtils.is(link.getId()).notNull(RCode.LINK_ID_ISNULL);
		link.setName(StrUtil.trim(link.getName()));
		ValidateUtils.is(link.getName()).notNull(RCode.LINK_NAME_ISNULL).maxLength(64);
		ValidateUtils.is(link.getUrl()).notNull(RCode.LINK_URL_ISNULL).maxLength(1024);
		linkService.updateById(link);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.LINK)
	@RequiresPermissions({"sys:link:delete"})
	public R remove(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.LINK_ID_ISNULL);
		String[] split = ids.split(",");
		linkService.removeLinks(Arrays.asList(split));
		return R.ok();
	}
	
	@PutMapping("/weights")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.LINK)
	public R modify(@RequestBody List<Link> links){

		linkService.saveWeights(links);
		return R.ok();
	}
}
