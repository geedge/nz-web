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
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.entity.SysConfigEntity;

@Service("promserverService")
public class PromserverServiceImpl extends ServiceImpl<PromserverDao, Promserver> implements PromserverService{

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String)params.get("id");
		String idcId = (String)params.get("idcId");
		String host = (String)params.get("host");
		String type = (String)params.get("type");
		String port = (String)params.get("port");
		String status = (String)params.get("status");
		IPage<Promserver> page = this.page(
			new Query<Promserver>().getPage(params),
			new QueryWrapper<Promserver>().lambda()
				.eq(StringUtils.isNotBlank(id),Promserver::getId, id)
				.like(StringUtils.isNotBlank(host),Promserver::getHost, host)
				.eq(StringUtils.isNotBlank(type), Promserver::getType,type)
				.eq(StringUtils.isNotBlank(idcId), Promserver::getIdcId,idcId)
				.eq(StringUtils.isNotBlank(port), Promserver::getPort,port)
				.eq(StringUtils.isNotBlank(status),Promserver::getStatus,status)
		);
		return new PageUtils(page);
	}

	@Override
	public void validateParam(Promserver promserverEntity) {
		Promserver promserver = this.getOne(new QueryWrapper<Promserver>().lambda()
				.eq(Promserver::getHost, promserverEntity.getHost())
				.eq(Promserver::getPort, promserverEntity.getPort())
				.eq(Promserver::getType, promserverEntity.getType()));
		if(promserver!=null) {
			throw new NZException(RCode.PROMSERVER_EXIST_ERROR);
		}
		ValidateUtils.is(promserverEntity.getIdcId()).notNull(RCode.PROMSERVER_IDCID_ISNULL);
		ValidateUtils.is(promserverEntity.getHost()).notNull(RCode.PROMSERVER_HOST_ISNULL);
		ValidateUtils.is(promserverEntity.getType()).notNull(RCode.PROMSERVER_TYPE_ISNULL);
		ValidateUtils.is(promserverEntity.getPort()).notNull(RCode.PROMSERVER_PORT_ISNULL);
		// 校验ip是否合法
		boolean ipFlag = CommonUtils.checkIp(promserverEntity.getHost());
		if(!ipFlag) {
			throw new NZException(RCode.PROMSERVER_HOST_ERROR);
		}
		// 校验port是否合法
		boolean portFlag = CommonUtils.checkPort(promserverEntity.getPort().toString());
		if(!portFlag) {
			throw new NZException(RCode.PROMSERVER_PORT_ERROR);
		}
	}
}
