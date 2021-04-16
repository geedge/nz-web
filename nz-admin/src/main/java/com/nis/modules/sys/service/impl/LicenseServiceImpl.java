package com.nis.modules.sys.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.sys.entity.LicenseEntity;
import com.nis.modules.sys.entity.LicenseEntity.Product;
import com.nis.modules.sys.entity.LicenseEntity.Tokens;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;

@Service
public class LicenseServiceImpl implements LicenseService {
	private static final Log log = Log.get();
	/**
	 * 公钥缓存
	 */
	private static PublicKey publickeyCache;
	/**
	 * 缓存license ，避免重复计算sign
	 */
	private static LicenseEntity licenseCache;
	@Autowired
	private SysConfigService sysConfigService;

	@Autowired
	private AssetAssetService assetService;

	@Value("${license.config.key:license}")
	private String licenseConfigKey;
	@Value("${license.token.key:license_token}")
	private String licenseTokenKey;
	/**
	 * 系统版本sys_config 表key
	 */
	@Value("${system.version.key:system_version}")
	private String systemVersionKey;

	/**
	 * 签名算法
	 */
	@Value("${license.sign.algorithm:NONEwithRSA}")
	private String signAlgorithm;

	/**
	 * 公钥路径，首先在jar包外查找，未找到在jar包中查找
	 */
	@Value("${license.publickey.path:/license/license.pem}")
	public String publicKeyPath = "/license/license.pem";


	/**
	 * base 不校验版本
	 */
	@Value("${license.base.version:base}")
	private String baseVersion;

	/**
	 * 获取token 首先尝试获取 硬件token 硬件token获取失败从数据库读取 数据库不存在 通过uuid 生成，保存到数据库
	 * 
	 * @param key
	 * @return
	 */
	@Override
	public String getLicenseToken() {
		String token = Tool.LicenseUtil.getMachineID();
		if (Tool.StrUtil.isBlank(token)) {
			SysConfigEntity entity = sysConfigService
					.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", licenseTokenKey));
			if (entity == null || Tool.StrUtil.isBlank(entity.getParamValue())) {
				token = Tool.IdUtil.fastSimpleUUID();
				if (entity == null) {
					entity = new SysConfigEntity();
				}
				entity.setParamKey(licenseTokenKey);
				entity.setParamValue(token);
				entity.setRemark("Automatic generated");
				sysConfigService.saveOrUpdate(entity);
			}
			token = entity.getParamValue();
		}
		return token;
	}

	@Override
	public LicenseEntity getLicenseEntity() {
		SysConfigEntity entity = sysConfigService
				.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", licenseConfigKey));
		if (entity == null || Tool.StrUtil.isBlank(entity.getParamValue())) {
			return null;
		}
		// 证书由xml转为 map
		LicenseEntity licenseEntity = Tool.LicenseUtil.xmlToLicenseBean(entity.getParamValue());
		return licenseEntity;
	}

	/**
	 * 更新license
	 */
	@Override
	public void uploadLicenseEntity(LicenseEntity license) {
		SysConfigEntity entity = sysConfigService
				.getOne(new QueryWrapper<SysConfigEntity>().eq("param_key", licenseConfigKey));
		if (entity == null) {
			entity = new SysConfigEntity();
		}
		entity.setParamKey(licenseConfigKey);
		entity.setParamValue(Tool.LicenseUtil.licenseBeanToXml(license, false));
		entity.setRemark(license.getSignature().getDigestValue());
		sysConfigService.saveOrUpdate(entity);

	}

	/**
	 * 获取系统版本
	 * 
	 * @return
	 */
	@Override
	public String getSystemVersion() {
		return sysConfigService.getValue(systemVersionKey);
	}

	/**
	 * 开始验证
	 * 
	 * @param license
	 * @return
	 */
	@Override
	public boolean verify(LicenseEntity license) {
		// 当前时间
		Date now = new DateTime(TimeZone.getTimeZone("UTC"));
		// license 开始时间
		String startDateStr = license.getStartDate();
		// license 结束时间
		String endDateStr = license.getEndDate();
		// license 授权token
		Tokens tokens = license.getTokens();
		Product product = license.getProduct();
		// 开始时间不为空 并且大于当前时间
		if (Tool.StrUtil.isNotBlank(startDateStr)) {
			Date startDate = Tool.DateUtil.parse(startDateStr, DatePattern.UTC_PATTERN)
					.setTimeZone(TimeZone.getTimeZone("UTC"));
			if (startDate != null && Tool.DateUtil.compare(startDate, now) > 0) {
				throw new NZException(RCode.LICENSE_FILE_INVALID);
			}
		}
		// 验证结束时间
		if (Tool.StrUtil.isNotBlank(endDateStr)) {
			Date endDate = Tool.DateUtil.parse(endDateStr, DatePattern.UTC_PATTERN)
					.setTimeZone(TimeZone.getTimeZone("UTC"));
			if (endDate != null && Tool.DateUtil.compare(now, endDate) > 0) {
				throw new NZException(RCode.LICENSE_FILE_INVALID);
			}
		}
		// 验证 tokens 是否有效
		if (tokens != null && tokens.getToken() != null) {
			String[] token = tokens.getToken();
			String currentToken = this.getLicenseToken();
			// license中不包含 token
			if (!Tool.ArrayUtil.contains(token, currentToken)) {
				throw new NZException(RCode.LICENSE_TOKEN_INVALID);
			}
		}

		if (product != null) {
			// 验证asset数量
			String version = product.getVersion();
			// 校验版本
			if (Tool.StrUtil.isNotBlank(version) && !Tool.StrUtil.equalsIgnoreCase(version, baseVersion)) {
				// 当前版本
				String systemVersion = this.getSystemVersion();
				if (Tool.StrUtil.isNotBlank(systemVersion) && !Tool.StrUtil.equalsIgnoreCase(version, systemVersion)) {
					throw new NZException(RCode.LICENSE_VERSION_INVALID);
				}
			}
			// 验证asset数量
			if (product.getMaximumAssets() != null) {
				// 当前asset 总数
				int count = assetService.count();
				if (product.getMaximumAssets() < count) {
					throw new NZException(RCode.LICENSE_MAXIMUMASSETS_INVALID);
				}
			}
		}
		return true;
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	@Override
	public LicenseEntity getAndSetLicenseCache() {
		// 从本地缓存获取
		LicenseEntity license = LicenseServiceImpl.licenseCache;
		// 数据库获取license
		LicenseEntity licenseDB = this.getLicenseEntity();
		ValidateUtils.is(licenseDB).notNull(RCode.LICENSE_FILE_IS_NULL);
		// 获取公钥证书
		PublicKey pk = this.getPublicKey();
		if (license != null) {
			// 通过digest验证数据库中内容 是否和 缓存一致
			String digestValueDB = licenseDB.getSignature().getDigestValue();
			String digestValueCache = license.getSignature().getDigestValue();
			// verify 耗时较长，通过比较digest判断是否变更
			if (!Tool.StrUtil.equals(digestValueDB, digestValueCache)) {
				log.info("license change,db:{},cache:{}", digestValueDB, digestValueCache);
				// 验证签名，digest不一致说明数据库中内容已变更，需要重新校验并修改 LicenseCache
				boolean verifyLicense = Tool.LicenseUtil.verifyLicenseSign(licenseDB, pk, signAlgorithm);
				log.info("license verify result: {}", verifyLicense);
				if (!verifyLicense) {// 验证失败，证书无效
					throw new NZException(RCode.LICENSE_FILE_INVALID);
				}
				// 更新缓存
				LicenseServiceImpl.licenseCache = licenseDB;
			}
		} else {
			// 更新缓存
			LicenseServiceImpl.licenseCache = licenseDB;
		}
		return LicenseServiceImpl.licenseCache;
	}

	/**
	 * 获取 publickey ，首先查找内存中查找，否则本地路径查找
	 * 
	 * @return @throws IOException @throws
	 */
	@Override
	public PublicKey getPublicKey() {
		PublicKey cache = LicenseServiceImpl.publickeyCache;
		if (cache == null) {
			File file = Tool.FileUtil.newFile(publicKeyPath);
			log.debug("pubk path : {} ,exist : {}", file.getAbsolutePath(),file.exists());
			// 通过 getResourceAsStream 确保jar包中也能够读取
			try (InputStream is = file.exists() ? Tool.FileUtil.getInputStream(file)
					: this.getClass().getResourceAsStream(publicKeyPath)) {
				cache = Tool.PemUtil.readPemPublicKey(is);
				log.info("license public key load success");
				if (log.isDebugEnabled()) {
					log.debug("public key content: {}", cache);
				}
				LicenseServiceImpl.publickeyCache = cache;
			} catch (IOException e) {
				throw new NZException("license public key load error", 999);
			}
		}
		return cache;
	}

	@Override
	public String getSignAlgorithm() {
		return this.signAlgorithm;
	}
}
