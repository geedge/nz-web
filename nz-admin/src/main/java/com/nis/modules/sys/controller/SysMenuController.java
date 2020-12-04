package com.nis.modules.sys.controller;

import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SysMenuEntity;
import com.nis.modules.sys.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/menu")
public class SysMenuController extends AbstractController {

	@Autowired
	private SysMenuService sysMenuService;

	/**
	 * 所有菜单列表
	 * @param id
	 * @return
	 */
	@GetMapping
	public R list(Integer id) {
		List<SysMenuEntity> menuList = sysMenuService.getMenuList(id);
		Map map = new HashMap(2);
		map.put("list", menuList);
		return R.ok(map);
	}

	/**
	 * 保存
	 * @param menu
	 * @return
	 */
	@PostMapping
	public R save(@RequestBody SysMenuEntity menu) {
		sysMenuService.saveMenu(menu);
		return R.ok();
	}

	/**
	 * 修改
	 * @param menu
	 * @return
	 */
	@PutMapping
	public R update(@RequestBody SysMenuEntity menu) {
		ValidateUtils.is(menu.getId()).notNull(RCode.SYS_MENU_ID_ISNULL);

		sysMenuService.updateMenu(menu);
		return R.ok();
	}

	/**
	 * 删除
	 * @param ids
	 * @return
	 */
	@DeleteMapping
	public R delete(@RequestParam Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.SYS_MENU_ID_ISNULL);

		sysMenuService.delete(ids);
		return R.ok();
	}
}