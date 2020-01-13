package com.nis.modules.promserver.service.impl;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;

@Service("promserverService")
public class PromserverServiceImpl extends ServiceImpl<PromserverDao, Promserver> implements PromserverService{

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String)params.get("id");
		String idcId = (String)params.get("idcId");
		String host = (String)params.get("host");
		String type = (String)params.get("type");
		String port = (String)params.get("port");
		IPage<Promserver> page = this.page(
			new Query<Promserver>().getPage(params),
			new QueryWrapper<Promserver>()
				.eq(StringUtils.isNotBlank(id),"id", id)
				.like(StringUtils.isNotBlank(host),"host", host)
				.eq(StringUtils.isNotBlank(type), "type",type)
				.eq(StringUtils.isNotBlank(idcId), "idc_id",idcId)
				.eq(StringUtils.isNotBlank(port), "port",port)
		);
		return new PageUtils(page);
	}

	@Override
	public void validateParam(Promserver promserverEntity) {
		
		ValidateUtils.is(promserverEntity.getIdcId()).notNull(RCode.PROMSERVER_IDCID_ISNULL);
		ValidateUtils.is(promserverEntity.getHost()).notNull(RCode.PROMSERVER_HOST_ISNULL);
		ValidateUtils.is(promserverEntity.getType()).notNull(RCode.PROMSERVER_TYPE_ISNULL);
		ValidateUtils.is(promserverEntity.getPort()).notNull(RCode.PROMSERVER_PORT_ISNULL);
		Integer port = promserverEntity.getPort();
		// 端口合法性校验
		String regex = "^([1-9]|[1-9]\\d{3}|[1-6][0-5][0-5][0-3][0-5])$";
		boolean flag = Pattern.matches(regex, port.toString());
		if(!flag) {
			throw new NZException(RCode.PROMSERVER_PORT_ERROR);
		}
	}

}
