package com.nis.modules.promserver.service.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.PromserverEntity;
import com.nis.modules.promserver.service.PromserverService;

@Service("promserverService")
public class PromserverServiceImpl extends ServiceImpl<PromserverDao, PromserverEntity> implements PromserverService{

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String)params.get("id");
		String idcId = (String)params.get("idcId");
		String host = (String)params.get("host");
		String type = (String)params.get("type");
		
		IPage<PromserverEntity> page = this.page(
			new Query<PromserverEntity>().getPage(params),
			new QueryWrapper<PromserverEntity>()
				.eq(StringUtils.isNotBlank(id),"id", id)
				.like(StringUtils.isNotBlank(host),"host", host)
				.eq(StringUtils.isNotBlank(type), "type",type)
				.eq(StringUtils.isNotBlank(idcId), "idc_id",idcId)
		);
		return new PageUtils(page);
	}

	@Override
	public void validateParam(PromserverEntity promserverEntity) {
		ValidateUtils.is(promserverEntity.getIdcId()).notNull(RCode.PROMSERVER_IDCID_ISNULL);
		ValidateUtils.is(promserverEntity.getHost()).notNull(RCode.PROMSERVER_HOST_ISNULL);
		ValidateUtils.is(promserverEntity.getType()).notNull(RCode.PROMSERVER_TYPE_ISNULL);
	}

}
