package com.nis.modules.asset.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.IdcTrafficDao;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.IdcTraffic;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.asset.service.IdcTrafficService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据中心    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("idcTrafficService")
public class IdcTrafficServiceImpl extends ServiceImpl<IdcTrafficDao, IdcTraffic> implements IdcTrafficService {

    @Autowired
    private IdcService idcService;

    @Autowired
    private IdcTrafficDao idcTrafficDao;

    @Override
    public PageUtils queryTrafficSetting(Map<String, Object> params) {
        IPage<IdcTraffic> page = new Query<IdcTraffic>().getPage(params);

        // 校验 direction 是否在可选值内
        String direction = (String) params.get("direction");
        if (StringUtils.isNotEmpty(direction) && !Constant.TRAFFIC_DIRECTIONS.contains(direction)) {
            throw new NZException(RCode.IDC_TRAFFIC_DIRECTIONS_INVALIDE);
        }
        List<IdcTraffic> traffics = idcTrafficDao.queryTrafficSetting(page, params);

        traffics.stream().forEach(idcTraffic -> {
            if (ToolUtil.isNotEmpty(idcTraffic.getTags())) {
                idcTraffic.setTags(JSONObject.parseObject(idcTraffic.getTags().toString(), Map.class));
            }
            if (ToolUtil.isNotEmpty(idcTraffic.getAuth())) {
                idcTraffic.setAuth(JSONObject.parseObject(idcTraffic.getAuth().toString(), Map.class));
            }
        });
        page.setRecords(traffics);
        return new PageUtils(page);
    }

    @Override
    public void validateTrafficInfo(Integer idcId, List<IdcTraffic> setting) {
        ValidateUtils.is(idcId).notNull(RCode.IDC_IDCID_ISNULL)
                .and(setting).notNull(RCode.IDC_TRAFFIC_SETTING_ISNULL);

        Idc idc = idcService.getById(idcId);
        if (ToolUtil.isEmpty(idc)) {
            throw new NZException(RCode.IDC_NOT_EXISTS);
        }

        String host, direction;
        Integer port, version;
        for (IdcTraffic traffic : setting) {
            host = traffic.getHost();
            port = traffic.getPort();
            version = traffic.getVersion();
            direction = traffic.getDirection();

            ValidateUtils.is(host).notNull(RCode.IDC_TRAFFIC_HOST_ISNULL)
                    .and(port).notNull(RCode.IDC_TRAFFIC_PORT_ISNULL)
                    .and(version).notNull(RCode.IDC_TRAFFIC_VERSION_ISNULL)
                    .and(direction).notNull(RCode.IDC_TRAFFIC_DIRECTIONS_ISNULL)
                    .and(traffic.getIfindex()).notNull(RCode.IDC_TRAFFIC_IFINDEX_ISNULL);

            // host
            if (!CommonUtils.checkUrl(host)) {
                throw new NZException(RCode.IDC_TRAFFIC_HOST_FORMAT);
            }

            // PORT
            if (!CommonUtils.checkPort(port.toString())) {
                throw new NZException(RCode.IDC_TRAFFIC_PORT_INVALIDE);
            }

            // version
            if (!Constant.SNMPV2.equals(version) && !Constant.SNMPV3.equals(version)) {
                throw new NZException(RCode.SNMP_MIB_VERSION_INVALIDE);
            }

            if (StringUtils.isEmpty(traffic.getCommunity())) {
                traffic.setCommunity(Constant.SNMP_DEFAULT_COMMUNITY);
            }

            // 流量方向可多选
            String[] split = direction.split(",");
            Arrays.asList(split).stream().forEach(s -> {
                if (!Constant.TRAFFIC_DIRECTIONS.contains(s)) {
                    throw new NZException(RCode.IDC_TRAFFIC_DIRECTIONS_INVALIDE);
                }
            });

            if (Constant.SNMPV3.equals(version)) {
                Map auth = (Map) traffic.getAuth();
                // 校验参数是否正确
                if (ToolUtil.isEmpty(auth)) {
                    throw new NZException(RCode.SNMP_MIB_AUTH_ISNULL);
                } else {
                    SnmpUtil.ValidateAuthForSnmpV3(auth);
                }
            }

            Map tags = (Map) traffic.getTags();
            if (ToolUtil.isNotEmpty(tags)) {
                tags.keySet().stream().forEach(key -> {
                    if (!Constant.PROMETHEUS_LABELSKEY_PATTERN.matcher(key.toString()).matches()) {
                        throw new NZException("tags's key " + key + " format is incorrect", RCode.IDC_TRAFFIC_TAGSKEY_FORMAT.getCode());
                    }
                });
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTraffic(Integer idcId, List<IdcTraffic> setting) {
        // 先删再增
        this.remove(new QueryWrapper<IdcTraffic>().lambda().eq(IdcTraffic::getIdcId, idcId));

        setting.stream().forEach(idcTraffic -> {
            idcTraffic.setIdcId(idcId);
            if (ToolUtil.isNotEmpty(idcTraffic.getTags())) {
                idcTraffic.setTags(JSONObject.toJSONString(idcTraffic.getTags()));
            } else {
                idcTraffic.setTags("");
            }
            if (ToolUtil.isNotEmpty(idcTraffic.getAuth())) {
                idcTraffic.setAuth(JSONObject.toJSONString(idcTraffic.getAuth()));
            }
        });

        this.saveBatch(setting);
    }
}