package com.nis.modules.sys.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.SysRoleEntity;
import com.nis.modules.sys.service.SysRoleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/sys/role")
public class SysRoleController extends AbstractController {

	@Autowired
	private SysRoleService sysRoleService;

	/**
	 * 角色列表查询
	 * @param params
	 * @return
	 */
	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYSTEM)
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = sysRoleService.queryPage(params);

		return R.ok(page);
	}

    /**
     * 角色菜单查询
     * @param roleId
     * @return
     */
    @GetMapping("/menu/{id}")
    @SysLog(operation = OperationEnum.QUERY,type = TypeEnum.SYSTEM)
    public R list(@PathVariable("id") Integer roleId) {
        ValidateUtils.is(roleId).notNull(RCode.SYS_ROLE_ID_ISNULL);

        return R.ok(sysRoleService.getMenuListByRoleId(roleId));
    }


    /**
     * 角色保存
     * @param role
     * @return
     */
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.SYSTEM)
	@RequiresPermissions("sys:role:save")
	public R save(@RequestBody SysRoleEntity role){
		sysRoleService.saveRole(role);
		return R.ok();
	}

	/**
	 * 修改角色
	 * @param role
	 * @return
	 */
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.SYSTEM)
	@RequiresPermissions("sys:role:update")
	public R update(@RequestBody SysRoleEntity role){
		ValidateUtils.is(role.getId()).notNull(RCode.SYS_ROLE_ID_ISNULL);

		sysRoleService.update(role);
		return R.ok();
	}

	/**
	 * 删除角色
	 * @param ids
	 * @return
	 */
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.SYSTEM)
	@RequiresPermissions("sys:role:delete")
	public R delete(@RequestParam(required = false) Integer force, Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.SYS_ROLE_ID_ISNULL);

		List<Map> delRelObjectMsgList = sysRoleService.deleteBatch(force, ids);

		force = Tool.ObjectUtil.defaultIfNull(force, 0);
		switch (force) {
			case 0: {// 0：存在关联数据时，删除失败
				if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
					return R.error(RCode.ASSET_MODEL_RELOBJ_CAN_NOT_REMOVE).putData("list", delRelObjectMsgList);
				}
			}
			case 1: // 1：存在关联数据时，同时删除关联数据
			default:
				break;
		}
		return R.ok().putData("list", delRelObjectMsgList);
	}
}
