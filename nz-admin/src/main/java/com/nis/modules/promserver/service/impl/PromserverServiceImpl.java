package com.nis.modules.promserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

	public void validateParam(Promserver promserverEntity,boolean modify) {
		Promserver promserver = this.getOne(new QueryWrapper<Promserver>().lambda()
				.eq(Promserver::getHost, promserverEntity.getHost())
				.eq(Promserver::getPort, promserverEntity.getPort())
				.eq(Promserver::getType, promserverEntity.getType()));
		if (promserver != null && !modify) {
			throw new NZException(RCode.PROMSERVER_EXIST_ERROR);
		}

		Integer type = promserverEntity.getType();
		String host = promserverEntity.getHost();
		Integer port = promserverEntity.getPort();
		ValidateUtils.is(promserverEntity.getIdcId()).notNull(RCode.PROMSERVER_IDCID_ISNULL)
				.and(host).notNull(RCode.PROMSERVER_HOST_ISNULL)
				.and(type).notNull(RCode.PROMSERVER_TYPE_ISNULL)
				.and(port).notNull(RCode.PROMSERVER_PORT_ISNULL);

		// 校验ip是否合法
		boolean ipFlag = CommonUtils.checkIp(host);
		if (!ipFlag) {
			throw new NZException(RCode.PROMSERVER_HOST_ERROR);
		}
		// 校验port是否合法
		boolean portFlag = CommonUtils.checkPort(port.toString());
		if (!portFlag) {
			throw new NZException(RCode.PROMSERVER_PORT_ERROR);
		}

		// 校验 type 合法性
		Constant.PromserverType[] values = Constant.PromserverType.values();
		List<Integer> list = Arrays.asList(values).stream().map(Constant.PromserverType::getValue).collect(Collectors.toList());
		if (!list.contains(type)) {
			throw new NZException(RCode.PROMSERVER_TYPE_INVALIDE);
		}
	}

	@Override
	public Promserver getOneAvaPromServer(Integer promId,Integer idcId,Integer type) {
		type = (type == null) ? 1 :type;
		List<Promserver> list = this.list(new QueryWrapper<Promserver>().lambda()
				.eq(true, Promserver::getStatus, 1)
				.eq(promId!=null, Promserver::getId, promId)
				.eq(promId==null&&type!=null, Promserver::getType, type)
				.eq(promId==null&&idcId != null, Promserver::getIdcId, idcId));
		// 校验是否有可用 prometheus server
		// ValidateUtils.is(list).notNull(RCode.PROMSERVER_HOST_ISNULL);
		// Promserver server = list.get(RandomUtils.nextInt(0, list.size()));

		Promserver server = null;
		if (CollectionUtils.isNotEmpty(list)) {
			server = list.get(RandomUtils.nextInt(0, list.size()));
		}
		return server;
	}

	@Override
	public void saveOrUpdatePromserver(Promserver promserverEntity,boolean modify) {
        // 校验
		this.validateParam(promserverEntity,modify);

		// 保存/修改
		this.saveOrUpdate(promserverEntity);
	}

	@Override
	public void updateStatus(List<Promserver> promservers) {
		this.baseMapper.updateStatus(promservers);
	}
}
