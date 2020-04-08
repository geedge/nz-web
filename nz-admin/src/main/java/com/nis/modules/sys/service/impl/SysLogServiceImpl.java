/**

 *

 *

 */

package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Query;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.dao.SysLogDao;
import com.nis.modules.sys.entity.SysLogEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysLogService;

import com.nis.common.utils.PageUtils;

import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("sysLogService")
@AllArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogDao, SysLogEntity> implements SysLogService {

    SysUserService userService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String)params.get("username");
        List<SysUserEntity> userList = userService.list(new LambdaQueryWrapper<SysUserEntity>().like(ToolUtil.isNotEmpty(username), SysUserEntity::getUsername,username));
        List<Long> userIds = userList.stream().map(SysUserEntity::getUserId).distinct().collect(Collectors.toList());
        if(ToolUtil.isEmpty(userIds)){userIds.add(-999L);}
        String operation = (String) params.get("operation");
        String type = (String) params.get("type");
        IPage<SysLogEntity> page = this.page(
            new Query<SysLogEntity>().getPage(params),
            new LambdaQueryWrapper<SysLogEntity>()
                    .in(SysLogEntity::getUserId, userIds)
                    .like(ToolUtil.isNotEmpty(operation),SysLogEntity::getOperation,operation)
                    .like(ToolUtil.isNotEmpty(type),SysLogEntity::getType,type)
        );
        page.getRecords().stream().forEach(item-> {
            SysUserEntity userEntity = userService.getById(item.getUserId());
            if(ToolUtil.isNotEmpty(userEntity)){
                item.setUsername(userEntity.getUsername());
                item.setUserId(null);
            }
        });

        return new PageUtils(page);
    }
}
