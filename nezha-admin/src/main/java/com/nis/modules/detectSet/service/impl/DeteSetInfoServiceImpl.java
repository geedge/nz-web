package com.nis.modules.detectSet.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.detectSet.dao.DeteSetInfoDao;
import com.nis.modules.detectSet.entity.DeteSetInfoEntity;
import com.nis.modules.detectSet.service.DeteSetInfoService;
import com.nis.modules.sys.entity.PermissionInfoEntity;


@Service("deteSetInfoService")
public class DeteSetInfoServiceImpl extends ServiceImpl<DeteSetInfoDao, DeteSetInfoEntity> implements DeteSetInfoService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<DeteSetInfoEntity> page = this.page(
                new Query<DeteSetInfoEntity>().getPage(params),
                new QueryWrapper<DeteSetInfoEntity>()
        );

        return new PageUtils(page);
    }

	@Override
	public List<DeteSetInfoEntity> queryByPermission(PermissionInfoEntity entity) {
		List<DeteSetInfoEntity> queryByPermission = baseMapper.queryByPermission(entity);
		return queryByPermission;
	}

}