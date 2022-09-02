package com.nis.modules.alert.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.protobuf.Types.TimeSeries;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.RecordRuleDao;
import com.nis.modules.alert.entity.RecordRuleEntity;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;

@Service
public class RecordRuleServiceImpl extends ServiceImpl<RecordRuleDao, RecordRuleEntity> implements RecordRuleService {
	private static final Log log = Log.get();

	@Autowired
	private SysUserStarredService starredService;

	@Autowired
	private SysUserDao sysUserDao;

	@Autowired
	private RecordRuleDao recordRuleDao;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	/**
	 * record list max size
	 */
	@Value("${redis.maxRecordDataSize:100000}")
	private Long maxRecordSize;

	@Override
	public RecordRuleEntity queryRecordRule(Integer id) {
		RecordRuleEntity recordRuleEntity = this.getById(id);

		if (recordRuleEntity.getCid() != null) {
			SysUserEntity userEntity = sysUserDao.selectById(recordRuleEntity.getCid());
			recordRuleEntity.setCreateUser(userEntity);
		}

		if (recordRuleEntity.getUid() != null) {
			SysUserEntity userEntity = sysUserDao.selectById(recordRuleEntity.getUid());
			recordRuleEntity.setUpdateUser(userEntity);
		}

		/// 收藏标识 0：未收藏 1：已收藏
		int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
				.eq(SysUserStarredEntity::getTid, recordRuleEntity.getId())
				.eq(SysUserStarredEntity::getType, "recordRule")
				.eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId()));
		recordRuleEntity.setStarred(count == 0 ? 0 : 1);
		return recordRuleEntity;
	}

	@Override
	public PageUtils list(Map<String, Object> params) {

		String ids = (String) params.get("ids");
		if (StrUtil.isNotBlank(ids)) {
			List<String> listIds = Arrays.asList(ids.split(","));
			params.put("ids", listIds);
		}
		params.put("userId", ShiroUtils.getUserId());
		IPage page = new Query(RecordRuleEntity.class).getPage(params);
		List<RecordRuleEntity> recordRules = recordRuleDao.queryList(page, params);

		page.setRecords(recordRules);
		return new PageUtils(page);
	}

	private void validateParam(RecordRuleEntity recordRule) {

		ValidateUtils.is(recordRule.getName()).notNull(RCode.RECORD_RULE_NAME_ISNULL).and(recordRule.getType())
				.notNull(RCode.RECORD_RULE_TYPE_ISNULL).and(recordRule.getExpr()).notNull(RCode.RECORD_RULE_EXPR_ISNULL)
				.and(recordRule.getLabels()).notNull(RCode.RECORD_RULE_LABELS_ISNULL).and(recordRule.getState())
				.notNull(RCode.RECORD_RULE_STATE_ISNULL);
		// 校验type
		List<Integer> type = Arrays.asList(new Integer[] { 1, 2 });
		if (!type.contains(recordRule.getType())) {
			throw new NZException(RCode.RECORD_RULE_TYPE_ERROR);
		}
		// 校验状态
		List<Integer> status = Arrays.asList(new Integer[] { 0, 1 });
		if (!status.contains(recordRule.getState())) {
			throw new NZException(RCode.RECORD_RULE_STATE_ERROR);
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int saveRecordRule(RecordRuleEntity recordRule) {

		// 校验参数
		this.validateParam(recordRule);
		// 检查name
		List<RecordRuleEntity> list = this
				.list(new LambdaQueryWrapper<RecordRuleEntity>().eq(RecordRuleEntity::getName, recordRule.getName()));
		if (Tool.ObjectUtil.isNotEmpty(list) || list.size() > 0) {
			throw new NZException(RCode.RECORD_RULE_NAME_EXIST);
		}
		Long id = ShiroUtils.getUserEntity().getId();
		recordRule.setCid(Integer.parseInt(id.toString()));
		recordRule.setUid(Integer.parseInt(id.toString()));
		recordRule.setCts(new Date().getTime());
		recordRule.setUts(new Date().getTime());
		this.save(recordRule);
		return recordRule.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateRecordRule(RecordRuleEntity recordRule) {
		// 校验参数
		this.validateParam(recordRule);

		List<RecordRuleEntity> list = this.list();
		Integer recordRuleId = recordRule.getId();
		// name 唯一
		Map<String, Integer> map = list.stream()
				.collect(Collectors.toMap(RecordRuleEntity::getName, RecordRuleEntity::getId));
		Integer id = map.get(recordRule.getName());
		if (id != null && !id.equals(recordRuleId)) {
			throw new NZException(RCode.RECORD_RULE_NAME_EXIST);
		}
		Long uid = ShiroUtils.getUserEntity().getId();
		recordRule.setUid(Integer.parseInt(uid.toString()));
		recordRule.setUts(new Date().getTime());
		this.updateById(recordRule);
		return recordRule.getId();
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deleteRecordRule(Integer[] ids) {
		List<RecordRuleEntity> list = this
				.list(new LambdaQueryWrapper<RecordRuleEntity>().in(RecordRuleEntity::getId, Arrays.asList(ids)));

		// 是否内置
		List<Integer> id = list.stream().filter(recordRule -> recordRule.getBuildIn() == 1).map(RecordRuleEntity::getId)
				.collect(Collectors.toList());
		if (Tool.ObjectUtil.isNotEmpty(id) || id.size() > 0) {
			throw new NZException(RCode.RECORD_RULE_BUILD_IN_EXIST);
		}
		this.removeByIds(Arrays.asList(ids));
		// 删除对应的收藏记录
		starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
				.eq(SysUserStarredEntity::getType, "recordRule").in(SysUserStarredEntity::getTid, Arrays.asList(ids)));
	}

	/**
	 * 存放 record data 到 redis 需要额外线程消费队列中的内容
	 * 
	 * @param recordData
	 */
	@Override
	@SuppressWarnings("all")
	public void rightPushRecordToRedis(List<TimeSeries> timeSeriesList) {
		if (Tool.CollUtil.isEmpty(timeSeriesList)) {
			return;
		}
		redisTemplate.executePipelined(new SessionCallback() {
			@Override
			public Object execute(RedisOperations operations) throws DataAccessException {
				ListOperations opsForList = operations.opsForList();
				timeSeriesList.forEach(ts -> {
					opsForList.rightPush(Constant.RECORD_DATA_CACHE, Tool.HexUtil.encodeHexStr(ts.toByteArray()));
				});
				return null;
			}
		});
		ListOperations opsForList = redisTemplate.opsForList();
		Long size = opsForList.size(Constant.RECORD_DATA_CACHE);
		// 判断是否超出所设置的队列最大长度
		if (size > maxRecordSize) {
			// 删除较老的数据
			opsForList.trim(Constant.RECORD_DATA_CACHE, -maxRecordSize, -1);
			log.warn("trim record_data_cache size : {}", size - maxRecordSize);
		}
		if(log.isDebugEnabled()) {
			log.debug("add record data cache size : {}", timeSeriesList.size());
		}
	}
	
	@Override
	@SuppressWarnings("all")
	public void rightPushRecordToRedis(TimeSeries timeSeries) {
		if (timeSeries == null) {
			return;
		}
		ListOperations opsForList = redisTemplate.opsForList();
		opsForList.rightPush(Constant.RECORD_DATA_CACHE, Tool.HexUtil.encodeHexStr(timeSeries.toByteArray()));
		Long size = opsForList.size(Constant.RECORD_DATA_CACHE);
		// 判断是否超出所设置的队列最大长度
		if (size > maxRecordSize) {
			// 删除较老的数据
			opsForList.trim(Constant.RECORD_DATA_CACHE, -maxRecordSize, -1);
			log.warn("trim record_data_cache size : {}", size - maxRecordSize);
		}
	}
}
