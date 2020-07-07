package com.nis.modules.sys.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.Link;
import com.nis.modules.sys.service.LinkService;

@RestController
@RequestMapping("/link")
public class LinkController extends AbstractController{
	@Autowired
	private LinkService linkService;
	
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.LINK)
	public R list(@RequestParam Map<String, Object> params){
		List<Link> result = linkService.queryList(params);
		return R.ok(result);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.LINK)
	public R save(@RequestBody Link link) {
		ValidateUtils.is(link.getName()).notNull(RCode.LINK_NAME_ISNULL);
		ValidateUtils.is(link.getUrl()).notNull(RCode.LINK_URL_ISNULL);
		linkService.saveLink(link);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.LINK)
	public R update(@RequestBody Link link) {
		ValidateUtils.is(link.getId()).notNull(RCode.LINK_ID_ISNULL);
		ValidateUtils.is(link.getName()).notNull(RCode.LINK_NAME_ISNULL);
		ValidateUtils.is(link.getUrl()).notNull(RCode.LINK_URL_ISNULL);
		linkService.updateById(link);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.LINK)
	public R remove(@RequestParam String ids) {
		ValidateUtils.is(ids).notNull(RCode.LINK_ID_ISNULL);
		String[] split = ids.split(",");
		linkService.removeLinks(Arrays.asList(split));
		return R.ok();
	}
	
	@PutMapping("/modify")
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.LINK)
	public R modify(@RequestBody Link link){
		ValidateUtils.is(link.getId()).notNull(RCode.LINK_ID_ISNULL);
		ValidateUtils.is(link.getPrev()).notNull(RCode.LINK_PREV_ISNULL);
		ValidateUtils.is(link.getNext()).notNull(RCode.LINK_NEXT_ISNULL);
		linkService.modifyOrder(link);
		return R.ok();
	}
}
