package com.nis.modules.asset.service.impl;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.asset.dao.AssetAccountDao;
import com.nis.modules.asset.dao.AssetAccountRelDao;
import com.nis.modules.asset.entity.AssetAccount;
import com.nis.modules.asset.entity.AssetAccountRel;
import com.nis.modules.asset.service.AssetAccountRelService;
import com.nis.modules.asset.service.AssetAccountService;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.log.Log;

@Service("assetAccountService")
public class AssetAccountServiceImpl extends ServiceImpl<AssetAccountDao,AssetAccount> implements AssetAccountService{

	private final static Log logger = Log.get();
	
	@Autowired
	private AssetAccountDao assetAccountDao;
	
	@Autowired
	private AssetAccountRelService assetAccountRelService;
	
	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		IPage<AssetAccount> page = new Query<AssetAccount>().getPage(params);
		List<AssetAccount> assetAccounts = assetAccountDao.selectAssetAccounts(page,params);
		page.setRecords(assetAccounts);
		return new PageUtils(page);
	}

	@Override
	public void removeByIds(List<Integer> ids) {
		
		//删除asset account表中数据
		assetAccountDao.deleteBatchIds(ids);
		
		//删除关联表数据
		assetAccountRelService.remove(new LambdaQueryWrapper<AssetAccountRel>().in(AssetAccountRel::getAccountId, ids));
	}

	@Override
	public AssetAccount queryAssetAccountById(Integer id) {
		return assetAccountDao.selectAssetAccountById(id);
	}

	@Override
	public void saveAssetAccountRel(Integer accountId, List<Integer> assetIds) {
		// 先清空跟accountId关联的现有数据  再全量新增
		assetAccountRelService.remove(new QueryWrapper<AssetAccountRel>().lambda().eq(AssetAccountRel::getAccountId, accountId));
		List<AssetAccountRel> datas =new ArrayList<AssetAccountRel>();
		for(Integer assetId: assetIds) {
			AssetAccountRel data = new AssetAccountRel();
			data.setAssetId(assetId);
			data.setAccountId(accountId);
			datas.add(data);
		}
		assetAccountRelService.saveBatch(datas);
	}

	@Override
	public PageUtils queryPageByAssetId(Integer assetId) {
		Map<String, Object> params = new HashMap<String, Object>();
		IPage<AssetAccount> page = new Query<AssetAccount>().getPage(params);
		List<AssetAccount> assetAccounts = assetAccountDao.selectAssetAccountsByAssetId(page,assetId);
		page.setRecords(assetAccounts);
		return new PageUtils(page);
	}

	@Override
	public void saveOrUpdateAssetAccount(AssetAccount assetAccount) {
		assetAccount.setUpdateAt(new Date());
		// 当前登录用户
        SysUserEntity currentLoginUser = ShiroUtils.getUserEntity();
        assetAccount.setUpdateBy(currentLoginUser.getId());
		this.saveOrUpdate(assetAccount);
	}

	@Override
	public List<AssetAccount> queryAccountsByAssetId(Integer assetId) {
		return assetAccountDao.selectAccountsByAssetId(assetId);
	}
}
