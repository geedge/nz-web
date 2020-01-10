package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.dao.AssetDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.CabinetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.romote.dao.AccountDao;
import com.nis.modules.romote.entity.Account;
import com.nis.modules.romote.service.AccountService;
import com.nis.modules.romote.util.StringUtils;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 资产    serviceImpl
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Service("assetService")
public class AssetServiceImpl extends ServiceImpl<AssetDao, Asset> implements AssetService {

    private static Logger logger = LoggerFactory.getLogger(AssetServiceImpl.class);

    // 数据中心  service
    @Autowired
    private IdcService idcService;

    // 机柜  service
    @Autowired
    private CabinetService cabinetService;

    // 资产型号  service
    @Autowired
    private ModelService modelService;

    // 字典   service
    @Autowired
    private SysDictService sysDictService;

    // 资产  dao
    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private AccountDao accountDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String idcIds = null;
        if (params != null && params.size() > 0) {
            idcIds = (String) params.get("idcIds");
        }

        if (StringUtils.isNotEmpty(idcIds)) {
            // 1. 清空idcid
            params.put("idcId", null);
            // 2. 校验idcids
            String[] idcIdsArr = idcIds.split(",");
            List<Integer> idcidList = new ArrayList<>();
            for (String id : idcIdsArr) {
                try {
                    if (id.length() > 0) {
                        idcidList.add(Integer.parseInt(id));
                    }
                } catch (NumberFormatException e) {
                    // ID转换异常
                    throw new NZException(RCode.IDC_ID_FORMAT);
                }
            }
            // 3. 放回参数集合中
            params.put("idcIds", idcidList);
        } else {
            // 4. 解决参数为空字符串问题
            params.put("idcIds", null);
        }

        IPage<Asset> page = new Query<Asset>().getPage(params);

        List<Asset> assetEntities = assetDao.queryList(page, params);
        page.setRecords(assetEntities);

        return new PageUtils(page);
    }

    @Override
    public List<Asset> queryListByHosts(Collection<String> hosts) {
        List<Asset> assetList = assetDao.queryListByHosts(hosts);
        return assetList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateAsset(Asset asset, MultipartFile[] sshKeys) {
        // 1. sn 唯一校验
        if (!this.checkFieldExist(asset.getId(), "sn", asset.getSn())) {
            throw new NZException(RCode.ASSET_SN_DUPLICATE);
        }

        // 2. host地址唯一校验
        if (!this.checkFieldExist(asset.getId(), "host", asset.getHost())) {
            throw new NZException(RCode.ASSET_HOST_DUPLICATE);
        }

        // 3. 型号有效性校验
        Model model = modelService.getById(asset.getModelId());
        if (model == null) {
            // 所选资产型号不存在
            throw new NZException(RCode.ASSET_MODEL_NOTFOUND);
        }

        // 4.数据中心有效性校验
        Idc idc = idcService.getById(asset.getIdcId());
        if (idc == null) {
            // 数据中心不存在
            throw new NZException(RCode.ASSET_IDC_NOTFOUND);
        }

        // 5.机柜有效性校验
        if (asset.getCabinetId() != null) {
            Cabinet cabinet = cabinetService.getById(asset.getCabinetId());
            if (cabinet == null) {
                // 机柜未找到
                throw new NZException(RCode.ASSET_CABINET_NOTFOUND);
            }
            if (!idc.getId().equals(cabinet.getIdcId())) {
                // 数据中心与机柜不符  当前数据中心没有含有所选机柜
                throw new NZException(RCode.IDC_NOTHAS_CABINET);
            }
        }

        // 6. 状态有效性校验
        if (asset.getState() != null) {
            Map<String, Object> map = new HashMap<>(2);

            map.put("type", Constant.DICT_ASSET_STATE_TYPE); // 字典表中资产状态 type 值 assetState
            List<SysDictEntity> assetTypes = sysDictService.queryList(map);
            boolean flag = false;
            for (SysDictEntity dict : assetTypes) {
                if (asset.getState().equals(dict.getCode())) {
                    flag = true;
                }
            }
            if (!flag) {
                // 未找到资产状态
                throw new NZException(RCode.ASSET_STATE_NOTFOUND);
            }
        } else {
            asset.setState(1);
        }

        Date now = new Date();
        Long userId = ShiroUtils.getUserId();
        if (asset.getId() == null) {
            asset.setCreateAt(now);
            asset.setCreateBy(userId);
        }
        asset.setUpdateAt(now);
        asset.setUpdateBy(userId);

        if (asset.getId() == null) {
            assetDao.saveAsset(asset);
        } else {
            this.updateById(asset);
        }

        // 保存 accounts
        if (CollectionUtils.isNotEmpty(asset.getAccounts())) {
            // int currSshKeys = 0;
            for (int i = 0; i < asset.getAccounts().size(); i++) {
                Account account = asset.getAccounts().get(i);
               /* if (Constant.AUTH_CERTIFICATE.equals(account.getAuthType())) {
                    try {
                        // ssh 登录选择文件
                        MultipartFile file = sshKeys[currSshKeys++];
                        account.setPrivateKey(file.getBytes());
                        account.setPwd(null);
                    } catch (Exception e) {
                        logger.error("transfer ssh certificate error", e);
                    }
                }*/

                // 添加
                if (account.getId() == null) {
                    // 处理密码
                    if (StringUtils.isNotEmpty(account.getPwd())) {
                        String encodePwd = Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                        account.setPwd(encodePwd);
                        try {
                            account.setPrivateKey(null);
                        } catch (Exception e) {
                            // TO DO
                        }
                    } else {
                        try {
                            // ssh 登录选择文件
                            MultipartFile file = sshKeys[0];
                            account.setPrivateKey(file.getBytes());
                            account.setPwd(null);
                        } catch (Exception e) {
                            logger.error("transfer ssh certificate error", e);
                        }
                    }
                    account.setAssetId(asset.getId());

                    accountService.save(account);
                } else {
                    // 修改参数含有密码
                    if (StringUtils.isNotEmpty(account.getPwd())) {
                        Account accountEntity = accountService.getById(account.getId());
                        // 前台传参密码和数据库存储密码不对应 证明修改了密码
                        if (!account.getPwd().equals(accountEntity.getPwd())) {
                            String encodePwd = Base64.getEncoder().encodeToString(account.getPwd().getBytes());
                            account.setPwd(encodePwd);
                            try {
                                account.setPrivateKey(null);
                            } catch (Exception e) {
                                // TO DO
                            }
                        }
                        accountService.updateById(account);
                    }

                    // 修改时没有指定密钥文件
                    if (sshKeys.length < 1) {
                        accountDao.updateForPrivateKey(account);
                    } else {
                        try {
                            // ssh 登录选择文件
                            MultipartFile file = sshKeys[0];
                            account.setPrivateKey(file.getBytes());
                        } catch (Exception e) {
                            logger.error("transfer ssh certificate error", e);
                        }
                        accountService.updateById(account);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer... ids) {
        List<Integer> idList = Arrays.asList(ids);

        // 1. 删除
        this.removeByIds(idList);

        // 2. 删除关联的endponiet
        endpointService.remove(new QueryWrapper<Endpoint>().in("asset_id", idList));

        // 3. 删除关联的alert信息
        alertRuleService.removeByTypeIds(Constant.AlertType.ALERT_TYPE_ASSET.getCode(), idList);
        alertMessageService.removeByTypeIds(Constant.AlertType.ALERT_TYPE_ASSET.getCode(), idList);
    }

    @Override
    public void validateAccount(Account account) {
        //1.认证类型必须合法
        ValidateUtils.is(account.getAuthType()).notNull(RCode.ACCOUNT_AUTHTYPE_ISNULL);
        QueryWrapper<SysDictEntity> sysDictyQueryWrapper = new QueryWrapper<SysDictEntity>();
        sysDictyQueryWrapper.eq("type", Constant.DICT_ACCOUNT_AUTH_TYPE); // authType 字典中的type值
        sysDictyQueryWrapper.eq("value", account.getAuthType());

        List<SysDictEntity> authTypeList = sysDictService.list(sysDictyQueryWrapper);

        if (authTypeList == null || authTypeList.size() > 0) {
            throw new NZException(RCode.ACCOUNT_AUTHTYPE_INVALIDE);
        }

        //2.用户名不能为空
        ValidateUtils.is(account.getUser()).notNull(RCode.ACCOUNT_USER_ISNULL);

        /*//3.密码登录，密码不能为空，证书登录，证书不能为空
        if (Constant.AUTH_PASSWORD == account.getAuthType()) {
            ValidateUtils.is(account.getPwd()).notNull(RCode.ACCOUNT_PWD_ISNULL);
        } else {
            ValidateUtils.is(file).notNull(RCode.ACCOUNT_KEYFILE_ISNULL);
        }*/
        //4.port 不能为空
        ValidateUtils.is(account.getPort()).notNull(RCode.ACCOUNT_PORT_ISNULL);
    }

    public boolean checkFieldExist(Integer id, String column, String columnValue) {
        List<Asset> list = this.list(new QueryWrapper<Asset>().eq(column, columnValue));
        boolean flag = false;

        //当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) ||
                (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
            flag = true;
        }
        return flag;
    }
}