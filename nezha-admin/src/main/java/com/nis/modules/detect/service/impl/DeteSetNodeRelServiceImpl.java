package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.detect.dao.DeteSetNodeRelDao;
import com.nis.modules.detect.entity.DeteSetNodeRelEntity;
import com.nis.modules.detect.service.DeteSetNodeRelService;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;


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

    @Override
    public void deleteBySetId(Long setId) {

        UpdateWrapper<DeteSetNodeRelEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("del_flag", Constant.DELFLAG_TRUE);
        updateWrapper.set("operator",ShiroUtils.getUserId());
        updateWrapper.set("op_time",new Date());
        updateWrapper.eq("set_id",setId);
        this.update(updateWrapper);
    }
}