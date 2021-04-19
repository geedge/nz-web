package com.nis.modules.promserver.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.promserver.dao.PromserverDao;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("promserverService")
public class PromserverServiceImpl extends ServiceImpl<PromserverDao, Promserver> implements PromserverService{

	@Autowired
	private DcService dcService;

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String host = (String)params.get("host");
		String type = (String)params.get("type");
		String port = (String)params.get("port");
		String token = (String) params.get("token");
		IPage<Promserver> page = this.page(
				new Query<Promserver>().getPage(params),
				new QueryWrapper<Promserver>().lambda()
						.in(ToolUtil.isNotEmpty(params.get("ids")), Promserver::getId, ToolUtil.isNotEmpty(params.get("ids")) ? Arrays.asList(params.get("ids").toString().split(",")) : null)
						.in(ToolUtil.isNotEmpty(params.get("dcIds")), Promserver::getDcId, ToolUtil.isNotEmpty(params.get("dcIds")) ? Arrays.asList(params.get("dcIds").toString().split(",")) : null)
						.in(ToolUtil.isNotEmpty(params.get("statuss")), Promserver::getStatus, ToolUtil.isNotEmpty(params.get("statuss")) ? Arrays.asList(params.get("statuss").toString().split(",")) : null)
						.like(StringUtils.isNotBlank(host), Promserver::getHost, host)
						.eq(StringUtils.isNotBlank(type), Promserver::getType, type)
						.eq(StringUtils.isNotBlank(port), Promserver::getPort, port)
						.like(StringUtils.isNotEmpty(token), Promserver::getToken, token)
		);

		List<Promserver> promserverList = page.getRecords();
		if (CollectionUtils.isNotEmpty(promserverList)) {
			promserverList = this.handleResultViewData(promserverList);
		}
		page.setRecords(promserverList);
		return new PageUtils(page);
	}

	private List<Promserver> handleResultViewData(List<Promserver> promserverList) {
		List<Dc> dcList = dcService.list();
		Map<Integer, Dc> dcIdAndEntityMap = dcList.stream().collect(Collectors.toMap(Dc::getId, Function.identity()));

		for (Promserver pojo : promserverList) {
			pojo.setDc(dcIdAndEntityMap.get(pojo.getDcId()));
		}
		return promserverList;
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
		ValidateUtils.is(promserverEntity.getDcId()).notNull(RCode.PROMSERVER_DCID_ISNULL)
				.and(host).notNull(RCode.PROMSERVER_HOST_ISNULL)
				.and(type).notNull(RCode.PROMSERVER_TYPE_ISNULL)
				.and(port).notNull(RCode.PROMSERVER_PORT_ISNULL)
				.and(promserverEntity.getToken()).notNull(RCode.PROMSERVER_TOKEN_ISNULL);

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

		// dc 存在校验
		Dc dc = dcService.getById(promserverEntity.getDcId());
		ValidateUtils.is(dc).notNull(RCode.DC_NOT_EXISTS);

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
				.eq(promId==null&&idcId != null, Promserver::getDcId, idcId));
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
	public Integer saveOrUpdatePromserver(Promserver promserverEntity, boolean modify) {
        // 校验
		this.validateParam(promserverEntity,modify);

		// 添加设置默认值
		if(!modify){
			promserverEntity.setStatus(1);
			promserverEntity.setStatusInfo(JSONObject.toJSONString(new HashMap<>()));
		}

		// 保存/修改
		this.saveOrUpdate(promserverEntity);
		return promserverEntity.getId();
	}

	@Override
	public void updateStatusInfo(Promserver promserver) {
		this.baseMapper.updateStatusInfo(promserver);
	}

	/**
	 * 详情
	 * @param id
	 * @return
	 */
	@Override
	public Promserver queryPromServerDetails(Integer id) {
		Promserver promserver = this.getById(id);
		ValidateUtils.is(promserver).notNull(RCode.PROMSERVER_NOTEXSITS);

		Dc dc = dcService.getById(promserver.getDcId());
		promserver.setDc(dc);
		return promserver;
	}
}
