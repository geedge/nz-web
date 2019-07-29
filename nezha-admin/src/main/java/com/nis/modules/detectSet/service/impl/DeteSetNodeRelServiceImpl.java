package com.nis.modules.detectSet.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.detectSet.dao.DeteSetNodeRelDao;
import com.nis.modules.detectSet.entity.DeteSetNodeRelEntity;
import com.nis.modules.detectSet.service.DeteSetNodeRelService;
import com.nis.modules.sys.entity.PermissionInfoEntity;


@Service("deteSetNodeRelService")
public class DeteSetNodeRelServiceImpl extends ServiceImpl<DeteSetNodeRelDao, DeteSetNodeRelEntity> implements DeteSetNodeRelService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<DeteSetNodeRelEntity> page = this.page(
                new Query<DeteSetNodeRelEntity>().getPage(params),
                new QueryWrapper<DeteSetNodeRelEntity>()
        );

        return new PageUtils(page);
    }

	@Override
	public List<DeteSetNodeRelEntity> queryBySystemId(PermissionInfoEntity entity) {
		List<DeteSetNodeRelEntity> queryBySystemId = baseMapper.queryBySystemId(entity);
		return queryBySystemId;
	}
    
}