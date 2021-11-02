/**

 *

 *

 */

package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.StringUtils;
import com.nis.modules.sys.dao.SysLogDao;
import com.nis.modules.sys.entity.SysLogEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.SysLogService;
import com.nis.modules.sys.service.SysUserService;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("sysLogService")
@AllArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogDao, SysLogEntity> implements SysLogService {

    @Autowired
    private SysUserService userService;

    @Autowired
    private SysLogDao sysLogDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String) params.get("username");
        if (StringUtils.isNotEmpty(username)) {
            List<SysUserEntity> userList = userService.list(new LambdaQueryWrapper<SysUserEntity>().like(SysUserEntity::getUsername, username).or().like(SysUserEntity::getName, username));
            List<Long> userIds = userList.stream().map(SysUserEntity::getId).distinct().collect(Collectors.toList());
            params.put("userIds", userIds);
        }

        String ids = (String) params.get("operaId");
        if(StrUtil.isNotBlank(ids)) {
        	List<String> listIds = Arrays.asList(ids.split(","));
        	params.put("operaId",listIds);
        }
        IPage page = new Query(SysLogEntity.class).getPage(params);
        List<SysLogEntity> sysLogEntities = sysLogDao.queryList(page, params);

        page.setRecords(sysLogEntities);

        return new PageUtils(page);
    }
}
