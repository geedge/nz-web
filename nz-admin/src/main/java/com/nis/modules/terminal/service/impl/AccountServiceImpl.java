package com.nis.modules.terminal.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.terminal.dao.AccountDao;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service("accountService")
public class AccountServiceImpl extends ServiceImpl<AccountDao, Account> implements AccountService {

    @Autowired
    private AssetDao assetDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String id = null, assetId = null, protocol = null;

        if (params != null && params.size() > 0) {
            id = (String) params.get("id");
            assetId = (String) params.get("assetId");
            protocol = (String) params.get("protocol");
        }

        QueryWrapper<Account> accountQueryWrapper = new QueryWrapper<Account>();
        accountQueryWrapper.lambda().eq(StringUtils.isNotEmpty(id), Account::getId, id);
        accountQueryWrapper.lambda().eq(StringUtils.isNotEmpty(assetId), Account::getAssetId, assetId);
        accountQueryWrapper.lambda().eq(StringUtils.isNotEmpty(protocol), Account::getProtocol, protocol);

        IPage<Account> page = this.page(new Query<Account>().getPage(params), accountQueryWrapper);

        page.getRecords().forEach(account -> {
            Object param = account.getParams();
            if(ToolUtil.isNotEmpty(param)){
              account.setParams(JSONObject.parseObject((String) param, Map.class));
            }
        });

        return new PageUtils(page);
    }

    @Override
    public void validateAccount(Account account) {
        Integer assetId = account.getAssetId();
        String protocol = account.getProtocol();
        Integer port = account.getPort();
        Object params = account.getParams();

        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL)
                    .and(assetDao.selectById(assetId)).notNull(RCode.ASSET_NOT_EXIST)
                    .and(protocol).notNull(RCode.ACCOUNT_PROTOCOL_ISNULL)
                    .and(port).notNull(RCode.ACCOUNT_PORT_ISNULL)
                    .and(params).notNull(RCode.ACCOUNT_PROTOCOL_PARAMS_ISNULL);

        if(!StrUtil.equals(Constant.ACCOUNT_SSH_PROTOCOL,protocol) && !StrUtil.equals(Constant.ACCOUNT_TELNET_PROTOCOL,protocol) && !StrUtil.equals(Constant.ACCOUNT_SNMP_PROTOCOL,protocol)) {
            throw new NZException(RCode.ACCOUNT_PROTOCOL_INCORRECT);
        }

        boolean portFlag = CommonUtils.checkPort(port.toString());
        if (!portFlag) {
            throw new NZException(RCode.ACCOUNT_PORT_ERROR);
        }

        try {
            Map map = (Map) params;
            account.setParams(JSONObject.toJSONString(map));
        } catch (RuntimeException e) {
            throw new NZException(RCode.ACCOUNT_PROTOCOL_PARAMS_FORMATERROR);
        }
    }


    @Override
    public void saveOrUpdateAccount(Account account) {
        if (account.getId() == null) {
            Map<String, String> params = JSONObject.parseObject((String) account.getParams(), Map.class);
            if (!Constant.ACCOUNT_SNMP_PROTOCOL.equals(account.getProtocol())) {
                // 不是 SNMP 类型才对密码进行加密
                if (ToolUtil.isNotEmpty(params.get("password"))) {
                    String encodePwd = Base64.getEncoder().encodeToString(params.get("password").getBytes());
                    params.put("password", encodePwd);
                }
            }
            account.setParams(JSONObject.toJSONString(params));

            this.save(account);
        } else {
            // 修改

            // 密码不为空
            Map<String, String> params = JSONObject.parseObject((String) account.getParams(), Map.class);
            if(ToolUtil.isNotEmpty(params.get("password"))){
                Account accountEntity = this.getById(account.getId());

                // 前台传参密码和数据库存储密码不对应 证明修改了密码
                Map<String, String> oldAccount = JSONObject.parseObject((String) accountEntity.getParams(), Map.class);

                // 不是 SNMP 协议类型 并且 密码不对应
                if (!params.get("password").equals(oldAccount.get("password")) && !Constant.ACCOUNT_SNMP_PROTOCOL.equals(account.getProtocol())) {
                    String encodePwd = Base64.getEncoder().encodeToString(params.get("password").getBytes());
                    params.put("password", encodePwd);
                }
                account.setParams(JSONObject.toJSONString(params));
            }

            this.updateById(account);
        }
    }

    @Override
    public void delete(Integer... ids) {
        List<Integer> idList = Arrays.asList(ids);

        this.removeByIds(idList);
    }
}
