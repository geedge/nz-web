package com.nis.modules.terminal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.service.AssetService;
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
    private AssetService assetService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String id = null, assetId = null, protocol = null, authType = null, user = null;

        if (params != null && params.size() > 0) {
            id = (String) params.get("id");
            assetId = (String) params.get("assetId");
            protocol = (String) params.get("protocol");
            authType = (String) params.get("authType");
            user = (String) params.get("user");
        }

        QueryWrapper<Account> accountQueryWrapper = new QueryWrapper<Account>();
        accountQueryWrapper.eq(StringUtils.isNotEmpty(id), "id", id);
        accountQueryWrapper.eq(StringUtils.isNotEmpty(assetId), "asset_id", assetId);
        accountQueryWrapper.like(StringUtils.isNotEmpty(protocol), "protocol", protocol);
        accountQueryWrapper.eq(StringUtils.isNotEmpty(authType), "auth_type", authType);
        accountQueryWrapper.like(StringUtils.isNotEmpty(user), "user", user);

        IPage<Account> page = this.page(new Query<Account>().getPage(params), accountQueryWrapper);

        return new PageUtils(page);
    }

    @Override
    public void validateAccount(Account account) {
        // 1.校验资产
        Integer assetId = account.getAssetId();
        ValidateUtils.is(assetId).notNull(RCode.ASSET_ID_ISNULL);
        ValidateUtils.is(assetService.getById(assetId)).notNull(RCode.ASSET_NOT_EXIST);
        // 2.协议protocol不能为空
        String protocol = account.getProtocol();
        ValidateUtils.is(protocol).notNull(RCode.ACCOUNT_PROTOCOL_ISNULL);
        if (!Constant.ACCOUNT_SSH_PROTOCOL.equals(protocol) && !Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
            throw new NZException(RCode.ACCOUNT_PROTOCOL_INCORRECT);
        }
        // 3.认证类型必须合法
        Integer authType = account.getAuthType();
        ValidateUtils.is(authType).notNull(RCode.ACCOUNT_AUTHTYPE_ISNULL);
        if (!Constant.AUTH_PASSWORD.equals(authType) && !Constant.AUTH_CERTIFICATE.equals(authType)) {
            throw new NZException(RCode.ACCOUNT_AUTHTYPE_INVALIDE);
        }
        if (Constant.ACCOUNT_TELNET_PROTOCOL.equals(protocol)) {
            if (!Constant.AUTH_PASSWORD.equals(authType)) {
                throw new NZException(RCode.ACCOUNT_AUTHTYPE_INVALIDE);
            }
        }
        // 4.用户名不能为空
        ValidateUtils.is(account.getUser()).notNull(RCode.ACCOUNT_USER_ISNULL);
        // 5.port 不能为空
        ValidateUtils.is(account.getPort()).notNull(RCode.ACCOUNT_PORT_ISNULL);
        boolean portFlag = CommonUtils.checkPort(account.getPort().toString());
        if (!portFlag) {
            // 账号端口不正确
            throw new NZException(RCode.ACCOUNT_PORT_ERROR);
        }
    }


    @Override
    public void saveOrUpdateAccount(Account account) {
        // 在SSH协议 telnet提示信息无效
        if(Constant.ACCOUNT_SSH_PROTOCOL.equals(account.getProtocol())){
            account.setUserTip(null);
            account.setPasswordTip(null);
            account.setReloginPasswordTip(null);
        } else {
            account.setPwd(null);
            account.setPrivateKey(null);
        }
        if (account.getId() == null) {
            // 密码
            if (Constant.AUTH_PASSWORD.equals(account.getAuthType())) {
                if (StringUtils.isNotEmpty(account.getPwd())) {
                    String encodePwd = Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                    account.setPwd(encodePwd);
                }
                account.setPrivateKey(null);
            } else {
                account.setPwd(null);
            }

            this.save(account);
        } else {
            // 修改
            if (Constant.AUTH_PASSWORD.equals(account.getAuthType())) {
                // 密码不为空
                if (StringUtils.isNotEmpty(account.getPwd())) {
                    Account accountEntity = this.getById(account.getId());

                    // 前台传参密码和数据库存储密码不对应 证明修改了密码
                    if (!account.getPwd().equals(accountEntity.getPwd())) {
                        String encodePwd = Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                        account.setPwd(encodePwd);
                    }
                }
                account.setPrivateKey(null);
            } else {
                account.setPwd(null);
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
