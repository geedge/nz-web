package com.nis.modules.mission.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.RCode;
import com.nis.modules.mission.dao.MissionInfoDao;
import com.nis.modules.mission.dao.MissionResultDao;
import com.nis.modules.mission.entity.Backup;
import com.nis.modules.mission.entity.MissionDto;
import com.nis.modules.mission.entity.MissionExecuteType;
import com.nis.modules.mission.entity.MissionUpgradeType;
import com.nis.modules.mission.entity.ParamForExecuteInstruct;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.mission.entity.MissionLoopEntity;
import com.nis.modules.mission.entity.MissionNodeRelEntity;
import com.nis.modules.mission.entity.MissionResultEntity;
import com.nis.modules.mission.service.MissionInfoService;
import com.nis.modules.mission.service.MissionNodeRelService;
import com.nis.modules.mission.utils.DESUtil;
import com.nis.modules.mission.utils.FileUtil;
import com.nis.modules.mission.utils.MD5Util;
import com.nis.modules.node.entity.NodeGroupInfoEntity;
import com.nis.modules.node.entity.NodeInfoEntity;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.dao.UserUsergroupDao;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.shiro.ShiroUtils;


import com.nis.modules.mission.entity.MissionType;
import com.nis.modules.mission.entity.ParamForFilePush;
import com.nis.modules.mission.entity.ParamForUpgrade;


@Service("missionInfoService")
public class MissionInfoServiceImpl extends ServiceImpl<MissionInfoDao, MissionInfoEntity> implements MissionInfoService {
	private final static Logger logger=LoggerFactory.getLogger(MissionInfoServiceImpl.class);
	@Autowired
	private MissionInfoDao missionInfoDao;
	@Autowired
	private MissionResultDao missionResultDao;
	@Autowired
	private SysUserDao sysUserDao;
	@Autowired
	private Environment en;
	@Autowired
	private MissionNodeRelService missionNodeRelService;
	@Autowired
	private PermissionInfoService permissionInfoService;
	@Autowired
	private UserUsergroupDao userUsergroupDao;
	
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageUtils queryPage(Page queryPage,MissionInfoEntity entity) {    	
    	//获取当前用户信息
    	SysUserEntity user = ShiroUtils.getUserEntity();
    	entity.setUser(user);
    	logger.info(" user info is "+user);
    	if(ShiroUtils.isGlobal()) {
    		entity.setIsGlobal(1);
    	}
    	IPage<MissionInfoEntity> page = baseMapper.selectMissionInfoList(queryPage,entity);
    	logger.info(" query page content :"+JSON.toJSONString(page));
        //获取MissionInfoEntity 列表
        List<MissionInfoEntity> list=page.getRecords();
        //遍历查询任务执行情况
        for(MissionInfoEntity missionInfoEntity:list) {
        	//任务id下对应的各个节点任务的执行情况
        	List<MissionResultEntity> missionResultList=missionResultDao.selectList(
        		new QueryWrapper<MissionResultEntity>()
        		.eq("mission_id", missionInfoEntity.getId())
        	);
        	
        	//遍历 missionResultList   进行任务执行情况统计
        	int success=0;
        	int fail=0;
        	int timeOut=0;
        	for(MissionResultEntity missionResult:missionResultList) {
        		if(Constant.MISSION_RESULT_SUCCESS.equals(missionResult.getState())) {
        			//成功
        			success++;
        		}else if(Constant.MISSION_RESULT_FAIL.equals(missionResult.getState())) {
        			fail++;
        		}else if(Constant.MISSION_RESULT_TIMEOUT.equals(missionResult.getState())) {
        			timeOut++;
        		}
        		
        	}
        	
        	//String res=success+","+fail+","+timeOut;
        	Integer[] res = new Integer[3];
        	res[0]=success;
        	res[1]=fail;
        	res[2]=timeOut;
        	missionInfoEntity.setStateDesc(res);
        	
        	//根据用户id获取用户名
        	missionInfoEntity.setOperator(ShiroUtils.getUserId());
        }
        page.setRecords(list);
        return new PageUtils(page);
    }
	    
    public MissionInfoEntity getMissionById(Integer id) {
    	
    	MissionInfoEntity missionInfoEntity=this.getById(id);
    	
    	//任务id下对应的各个节点任务的执行情况
    	List<MissionResultEntity> missionResultList=missionResultDao.selectList(
    		new QueryWrapper<MissionResultEntity>()
    		.eq(true, "mission_id", missionInfoEntity.getId())
    	);
    	
    	//进行任务执行情况统计
    	int success=0;
    	int fail=0;
    	int timeOut=0;
    	for(MissionResultEntity missionResult:missionResultList) {
    		if(Constant.MISSION_RESULT_SUCCESS.equals(missionResult.getState())) {
    			//成功
    			success++;
    		}else if(Constant.MISSION_RESULT_FAIL.equals(missionResult.getState())) {
    			fail++;
    		}else if(Constant.MISSION_RESULT_TIMEOUT.equals(missionResult.getState())) {
    			timeOut++;
    		}
    		
    	}
    	
    	//String res=success+","+fail+","+timeOut;
    	Integer[] res = new Integer[3];
    	res[0]=success;
    	res[1]=fail;
    	res[2]=timeOut;
    	missionInfoEntity.setStateDesc(res);
    	
    	//查询节点组
    	
    	//查询节点
    	
    	//根据用户id获取用户名
    	missionInfoEntity.setOperator(ShiroUtils.getUserId());
    	
    	return missionInfoEntity;
    }

	@Override
	public List<MissionInfoEntity> queryByPermission(PermissionInfoEntity entity) {
		List<MissionInfoEntity> queryByPermission = baseMapper.queryByPermission(entity);
		return queryByPermission;
	}

	@Override
	@Transactional(readOnly = false)
	public void saveMission(MissionInfoEntity missionInfoEntity) {
		logger.info("start save missionInfo");
		missionInfoEntity.setOperator(ShiroUtils.getUserId());
		missionInfoEntity.setOpTime(new Date());
		logger.info("save missionInfo content :"+JSON.toJSONString(missionInfoEntity));
		baseMapper.insert(missionInfoEntity);
		logger.info(" save missionInfo success");
		//添加任务与节点之间的关系
		List<MissionNodeRelEntity> missionNodeRelEntitys=new ArrayList<MissionNodeRelEntity>();
		List<Long> nodeIds = missionInfoEntity.getNodeIds();
		List<Long> nodeGroupIds = missionInfoEntity.getNodeGroupIds();
		//添加权限相关设置信息
		List<PermissionInfoEntity> permissionInfoEntitys = new ArrayList<PermissionInfoEntity>();
		Long relId = null;
		boolean permissionFlag=false;
		boolean systemFlag=false;
		logger.info("start save node missionInfo and permissionInfo data");
		if(Constant.VIEWLEVEL_UG.equals(missionInfoEntity.getViewLevel())) {
			permissionFlag=true;
		} else if(Constant.VIEWLEVEL_SYS.equals(missionInfoEntity.getViewLevel())) {
			relId = ShiroUtils.getCurrSystemId();
			systemFlag=true;
			permissionFlag=true;
		}
		if(null!=nodeIds&&nodeIds.size()>0) {
			for(Long nodeId : nodeIds) {
				MissionNodeRelEntity missionNodeRelEntity=new MissionNodeRelEntity();
				missionNodeRelEntity.setMissionId(missionInfoEntity.getId());
				missionNodeRelEntity.setType(Constant.MISSION_NODE_REL_TYPE_NODE);
				missionNodeRelEntity.setRelId(nodeId);
				missionNodeRelEntity.setOpTime(new Date());
				missionNodeRelEntitys.add(missionNodeRelEntity);
			}
		}
		if(null!=nodeGroupIds&&nodeGroupIds.size()>0) {
			for(Long nodeGroupId : nodeGroupIds) {
				MissionNodeRelEntity missionNodeRelEntity=new MissionNodeRelEntity();
				missionNodeRelEntity.setMissionId(missionInfoEntity.getId());
				missionNodeRelEntity.setType(Constant.MISSION_NODE_REL_TYPE_GROUP);
				missionNodeRelEntity.setRelId(nodeGroupId);
				missionNodeRelEntity.setOpTime(new Date());
				missionNodeRelEntitys.add(missionNodeRelEntity);
			}
		}
		
		if(permissionFlag) {
			if(null!=missionInfoEntity.getViewerId()&&missionInfoEntity.getViewerId().length>0) {
				//验证前台传递的用户组信息是否跟当前用户所在用户组信息一致
				Integer count = userUsergroupDao.checkUserGroupCorrect(ShiroUtils.getUserId(), missionInfoEntity.getViewerId());
				if(count!=missionInfoEntity.getViewerId().length) {
					throw new NZException(RCode.SYS_USERGROUP_MATCH_ERROR);
				}
				for(Long viewId : missionInfoEntity.getViewerId()) {
					PermissionInfoEntity permissionInfoEntity = new PermissionInfoEntity();
					permissionInfoEntity.setType(Constant.PM_TYPE_MISSION);
					permissionInfoEntity.setTypeId(missionInfoEntity.getId());
					permissionInfoEntity.setViewLevel(missionInfoEntity.getViewLevel());
					if(!systemFlag) {
						relId = viewId.longValue();
					}
					permissionInfoEntity.setRelId(relId);
					permissionInfoEntitys.add(permissionInfoEntity);
				}
			}else {
				PermissionInfoEntity permissionInfoEntity = new PermissionInfoEntity();
				permissionInfoEntity.setType(Constant.PM_TYPE_MISSION);
				permissionInfoEntity.setTypeId(missionInfoEntity.getId());
				permissionInfoEntity.setViewLevel(missionInfoEntity.getViewLevel());
				permissionInfoEntity.setRelId(relId);
				permissionInfoEntitys.add(permissionInfoEntity);
			}
		}
		logger.info("missionNodeRelEntitys :"+JSON.toJSONString(missionNodeRelEntitys));
		logger.info("permissionInfoEntitys :"+JSON.toJSONString(permissionInfoEntitys));
		missionNodeRelService.saveBatch(missionNodeRelEntitys);
		permissionInfoService.saveBatch(permissionInfoEntitys);
	}
	
	/**
	 * 传入节点组或者节点信息 验证是否有效
	 */
	@Override
	public void checkNodeGroupOrNodeCorrect(List<Long> nodeGroupIds,List<Long> nodeIds) {
		logger.info("check Node Or NodeGroup correct");
		if(null != nodeIds && nodeIds.size()>0) {
			logger.info("node info :"+JSON.toJSONString(nodeIds));
			//查询所有节点是否存在以及是否满足条件
			MissionDto nodeDto = baseMapper.checkNodeCorrect(nodeGroupIds,nodeIds);
			if(null != nodeDto.getTotal() && nodeDto.getTotal() != nodeIds.size()) {
				throw new NZException(RCode.MISSION_NODEINFO_ERROR);
			}else if(null != nodeDto.getNnc() && nodeDto.getNnc()>0 ) {
				// 所选节点组下存在网元
				throw new NZException(RCode.MISSION_NODE_NET_ERROR);
			}
		/*	//如果节点全部满足条件 去除节点所在节点组的信息
			MissionDto removeGroupDto = baseMapper.selectNodeGroupOrNodeForCheck(nodeGroupIds,nodeIds);
			List<Long> queryNodeGroupIds = removeGroupDto.getNodeGroupIds();
			if(null != queryNodeGroupIds && queryNodeGroupIds.size()>0) {
				nodeGroupIds.removeAll(queryNodeGroupIds);
			}*/
		}
		if(null != nodeGroupIds && nodeGroupIds.size()>0) {
			logger.info("nodeGroup info :"+JSON.toJSONString(nodeGroupIds));
			MissionDto groupDto = baseMapper.checkNodeGroupCorrect(nodeGroupIds);
			if(null != groupDto.getDgnic() || groupDto.getDgnic() != nodeGroupIds.size()) {
				// 总数不对应 存在无效的节点组
				throw new NZException(RCode.MISSION_NODEINFO_ERROR);
			}else if(null != groupDto.getNgnnc() && groupDto.getNgnnc()>0) {
				// 存在节点组下无节点
				throw new NZException(RCode.MISSION_NODEINFO_ERROR);
			}else if(null != groupDto.getNnc() && groupDto.getNnc()>0 ) {
				// 所选节点组下存在网元
				throw new NZException(RCode.MISSION_NODE_NET_ERROR);
			}
		}
	}
	
	/**
	 * 处理任务相关参数 校验参数正确性
	 */
	@Override
	public void validateMissionInfoData(MissionInfoEntity missionInfoEntity,MultipartFile[] files) {
		logger.info("validate missionInfo content :"+JSON.toJSONString(missionInfoEntity));
		ValidateUtils.is(missionInfoEntity.getType()).notNull(RCode.MISSION_TYPE_ISNULL);
		ValidateUtils.is(missionInfoEntity.getName()).notNull(RCode.MISSION_NAME_ISNULL);
		ValidateUtils.is(missionInfoEntity.getParam()).notNull(RCode.MISSION_PARAM_ISNULL);
		if(null == missionInfoEntity.getNodeGroupIds() && missionInfoEntity.getNodeGroupIds().size()<=0&&
				null==missionInfoEntity.getNodeIds() && missionInfoEntity.getNodeIds().size()<=0) {
			throw new NZException(RCode.MISSION_GROUPNODE_OR_NODE_ISNULL);
		}
		//验证保存节点 节点组是否有效
		checkNodeGroupOrNodeCorrect(missionInfoEntity.getNodeGroupIds(),missionInfoEntity.getNodeIds());
		switch(MissionType.getMissionNameByTypeId(Integer.valueOf(missionInfoEntity.getType()))) {
		case FILE_PUSH:
			List<ParamForFilePush> paramForFilePushs = JSON.parseArray(missionInfoEntity.getParam(), ParamForFilePush.class);
			if(null==files || files.length<=0) {
				throw new NZException(RCode.MISSION_FILEPUSH_FILE_ISNULL);
			}
			if(null!=paramForFilePushs&&paramForFilePushs.size()>0) {
				logger.info("mission_file_push start file upload");
				// 记录文件单个大小 记录日志  记录总大小 判断是否超过限制
				long onefile = 0l;
				long sum = 0l;
				File[] removeFiles=new File[paramForFilePushs.size()];
				boolean outSizeFlag=false;
				fu:for(int i=0;i<paramForFilePushs.size();i++) {
					ParamForFilePush paramForFilePush = paramForFilePushs.get(i);
					MultipartFile uploadFile = files[i];
					ValidateUtils.is(uploadFile.getOriginalFilename()).notNull(RCode.MISSION_FILEPUSH_FILE_ISNULL);
					ValidateUtils.is(paramForFilePush.getDestPath()).notNull(RCode.MISSION_FILEPUSH_DESTPATH_ISNULL);
					if(CommonUtil.isNull(paramForFilePush.getIsCover())) {
						paramForFilePush.setIsCover("0");
					}
					//处理文件推送中密码 
					if (paramForFilePush.getPw() != null && paramForFilePush.getPw().trim().length() > 0) {// 密码加密保存
						try {
							paramForFilePush.setPw(DESUtil.desEncrypt(paramForFilePush.getPw().trim()));
						} catch (Exception e) {
							//加密密码出现错误
							e.printStackTrace();
							throw new NZException(RCode.MISSION_FILEPUSH_PASSWORD_HANDLE_ERROR);
						}
					}
					//获取配置文件内的上传路径
					File rootPath=null;
					try {
						rootPath = new File(ResourceUtils.getURL("classpath:").getPath());
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					String uploadPath = en.getProperty("upload.path");
				    File upload = new File(rootPath.getAbsolutePath(), uploadPath);
					// -- 上传目录不存在则创建
					if (!upload.exists()) {
						upload.mkdirs();
					}
					String path=upload+File.separator;
					//处理文件名称
					String upFileName=FileUtil.addTimeTagForUploadFileName(uploadFile.getOriginalFilename());
					//上传文件 
					File dest = new File(path + upFileName);
					 try {
						    uploadFile.transferTo(dest);
						    onefile=uploadFile.getSize();
						    sum=sum+onefile;
						    removeFiles[i]=dest;
				        } catch (Exception e) {
				        	e.printStackTrace();
				        	throw new NZException(RCode.MISSION_FILEPUSH_FILE_UPLOAD_ERROR);
				        }
					//判断上传文件的总大小是否超出
					 if(sum>104857600) {
						 outSizeFlag=true;
						 break fu;
					 }
					String fileMd5=null;
					try {
						fileMd5 = MD5Util.getFileMD5String(dest);
					} catch (IOException e) {
						e.printStackTrace();
						throw new NZException(RCode.MISSION_FILEPUSH_MD5_ERROR);
					}
					paramForFilePush.setMd5Value(fileMd5);
					paramForFilePush.setUpFileName(upFileName);
				}
				logger.info("mission_file_push finish file upload");
				//如果超出100M文件大小 则将已上传文件删除
				if(outSizeFlag) {
					logger.info("mission_file_push outsize start delete file");
					for(int j = 0; j<removeFiles.length;j++) {
						File file = removeFiles[j];
						if(file.exists()) {
							file.delete();
						}
					}
					throw new NZException(RCode.MISSION_FILEPUSH_OUTSIZE_ERROR);
				}else {
					String param = JSON.toJSONString(paramForFilePushs);
					missionInfoEntity.setParam(param);
				}
			}else {
				throw new NZException(RCode.MISSION_PARAM_ERROR);
			}
			break;
		case EXECUTE_INSTRUCT:
			ValidateUtils.is(missionInfoEntity.getCommandEnname()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_COMMANDENNAME_ISNULL);
			MissionExecuteType missionExecuteType = MissionExecuteType.getMissionExecuteType(missionInfoEntity.getCommandEnname());
			switch(missionExecuteType) {
				case UPGRADE_KILL_PROCESS:
					MissionDto missionDto =JSON.parseObject(missionInfoEntity.getParam(), MissionDto.class);
					ParamForExecuteInstruct paramForExecuteInstruct = missionDto.getParamForExecuteInstruct(); 
					String[] pidFiles = paramForExecuteInstruct.getPidFiles();
					if(null == pidFiles || pidFiles.length<=0) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					for(String pidFile : pidFiles) {
						ValidateUtils.is(pidFile).notNull(RCode.MISSION_EXECUTE_INSTRUCT_KILLPROCESS_PIDFILE_ISNULL);
					}
					missionDto.setCommandEnname(missionInfoEntity.getCommandEnname());
					missionDto.setCommandType(missionInfoEntity.getCommandType());
					// 判断停用任务是否为周期任务
					if((Constant.MISSION_LOOP_TRUE).equals(missionInfoEntity.getIsLoop())) {
						MissionLoopEntity missionLoopEntity = missionDto.getMissionLoopEntity();
						//判断周期任务的结束时间不能小于当前时间
						Date endTime = missionLoopEntity.getEndTime();
						if(endTime.getTime()<new Date().getTime()) {
							throw new NZException(RCode.MISSION_EXECUTE_INSTRUCT_KILLPROCESS_LOOPTIME_ERROR);
						}
					}
					missionInfoEntity.setParam(JSON.toJSONString(missionDto));
					break;
				// 可执行命令与 启动命令逻辑一致 使用case switch穿透特性	
				case UPGRADE_EXEC:
				case UPGRADE_START:
					MissionDto missionDto2 =JSON.parseObject(missionInfoEntity.getParam(), MissionDto.class);
					List<ParamForExecuteInstruct> paramForStart = missionDto2.getParamForExecuteInstructs();
					if(null==paramForStart || paramForStart.size()<=0) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					for(ParamForExecuteInstruct paramForExecuteInstruct2 : paramForStart) {
						ValidateUtils.is(paramForExecuteInstruct2.getExecCmd()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_START_EXECCMD_ISNULL);
						ValidateUtils.is(paramForExecuteInstruct2.getExecResult()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_START_EXECRESULT_ISNULL);
						if(CommonUtil.isNull(paramForExecuteInstruct2.getForceExec())) {
							paramForExecuteInstruct2.setForceExec("0");
						}
						if(CommonUtil.isNull(paramForExecuteInstruct2.getResidentFlag())) {
							paramForExecuteInstruct2.setResidentFlag("0");
						}
						if(CommonUtil.isNull(paramForExecuteInstruct2.getReturnFlag())) {
							paramForExecuteInstruct2.setReturnFlag("0");
						}else {
							ValidateUtils.is(paramForExecuteInstruct2.getReturnPath()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_START_RETURNPATH_ISNULL);
						}
						if(CommonUtil.isNull(paramForExecuteInstruct2.getMaxWaitTime())) {
							paramForExecuteInstruct2.setMaxWaitTime("3");
						}
						if (paramForExecuteInstruct2.getExecParamsTmpStr() != null && paramForExecuteInstruct2.getExecParamsTmpStr().trim().length() > 0) {
							// 将\r,\n,\t字符替换为空
							String execParamsTmpStr = paramForExecuteInstruct2.getExecParamsTmpStr().trim();
							execParamsTmpStr = execParamsTmpStr.replaceAll("[\n,\r,\t]", "");
							if (execParamsTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
								paramForExecuteInstruct2.setExecParams(execParamsTmpStr.split(Constant.STR_PARAM_SEPRATOR));
							} else {
								paramForExecuteInstruct2.setExecParams(new String[] { execParamsTmpStr });
							}
							paramForExecuteInstruct2.setExecParamsTmpStr(null);
						}
					}
					missionDto2.setCommandEnname(missionInfoEntity.getCommandEnname());
					missionDto2.setCommandType(missionInfoEntity.getCommandType());
					// 判断停用任务是否为周期任务
					if((Constant.MISSION_LOOP_TRUE).equals(missionInfoEntity.getIsLoop())) {
						MissionLoopEntity missionLoopEntity = missionDto2.getMissionLoopEntity();
						//判断周期任务的结束时间不能小于当前时间
						Date endTime = missionLoopEntity.getEndTime();
						if(endTime.getTime()<new Date().getTime()) {
							throw new NZException(RCode.MISSION_EXECUTE_INSTRUCT_START_LOOPTIME_ERROR);
						}
					}
					missionInfoEntity.setParam(JSON.toJSONString(missionDto2));
					break;
				case UPGRADE_BACKUP:
					MissionDto missionDto3 =JSON.parseObject(missionInfoEntity.getParam(), MissionDto.class);
					List<ParamForExecuteInstruct> paramForBackup = missionDto3.getParamForExecuteInstructs();
					if(null==paramForBackup || paramForBackup.size()<=0) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					for(ParamForExecuteInstruct paramForExecuteInstruct3 : paramForBackup) {
						ValidateUtils.is(paramForExecuteInstruct3.getBackup()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_BACKUP_FILE_ISNULL);
						ValidateUtils.is(paramForExecuteInstruct3.getBackupTo()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_BACKUP_PATH_ISNULL);
						if(CommonUtil.isNull(paramForExecuteInstruct3.getIsAbs())) {
							paramForExecuteInstruct3.setIsAbs("0");
						}
						if (paramForExecuteInstruct3.getExceptTmpStr() != null && paramForExecuteInstruct3.getExceptTmpStr().trim().length() > 0) {
							// 将\r,\n,\t字符替换为空
							String exceptTmpStr = paramForExecuteInstruct3.getExceptTmpStr().trim();
							exceptTmpStr = exceptTmpStr.replaceAll("[\n,\r,\t]", "");
							if (exceptTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
								paramForExecuteInstruct3.setExcept(exceptTmpStr.split(Constant.STR_PARAM_SEPRATOR));
							} else {
								paramForExecuteInstruct3.setExcept(new String[] { exceptTmpStr });
							}
							paramForExecuteInstruct3.setExceptTmpStr(null);
						}
					}
					missionDto3.setCommandEnname(missionInfoEntity.getCommandEnname());
					missionDto3.setCommandType(missionInfoEntity.getCommandType());
					// 判断停用任务是否为周期任务
					if((Constant.MISSION_LOOP_TRUE).equals(missionInfoEntity.getIsLoop())) {
						MissionLoopEntity missionLoopEntity = missionDto3.getMissionLoopEntity();
						//判断周期任务的结束时间不能小于当前时间
						Date endTime = missionLoopEntity.getEndTime();
						if(endTime.getTime()<new Date().getTime()) {
							throw new NZException(RCode.MISSION_EXECUTE_INSTRUCT_BACKUP_LOOPTIME_ERROR);
						}
					}
					missionInfoEntity.setParam(JSON.toJSONString(missionDto3));
					break;
				case UPGRADE_UPDATE:
					MissionDto missionDto4 =JSON.parseObject(missionInfoEntity.getParam(), MissionDto.class);
					List<ParamForExecuteInstruct> paramForUpdate = missionDto4.getParamForExecuteInstructs();
					if(null==paramForUpdate || paramForUpdate.size()<=0) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					for(ParamForExecuteInstruct paramForExecuteInstruct4 : paramForUpdate) {
						ValidateUtils.is(paramForExecuteInstruct4.getSource()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_UPDATE_SOURCE_ISNULL);
						ValidateUtils.is(paramForExecuteInstruct4.getCover()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_UPDATE_COVER_ISNULL);
						if(CommonUtil.isNull(paramForExecuteInstruct4.getIsAbs())) {
							paramForExecuteInstruct4.setIsAbs("0");
						}
						if(CommonUtil.isNull(paramForExecuteInstruct4.getIsCreateCover())) {
							paramForExecuteInstruct4.setIsCreateCover("0");
						}
						if (paramForExecuteInstruct4.getDeleteTmpStr() != null && paramForExecuteInstruct4.getDeleteTmpStr().trim().length() > 0) {
							// 将\r,\n,\t字符替换为空
							String deleteTmpStr = paramForExecuteInstruct4.getDeleteTmpStr().trim();
							deleteTmpStr = deleteTmpStr.replaceAll("[\n,\r,\t]", "");
							if (deleteTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
								paramForExecuteInstruct4.setDelete(deleteTmpStr.split(Constant.STR_PARAM_SEPRATOR));
							} else {
								paramForExecuteInstruct4.setDelete(new String[] { deleteTmpStr });
							}
							paramForExecuteInstruct4.setDeleteTmpStr(null);
						}
					}
					missionDto4.setCommandEnname(missionInfoEntity.getCommandEnname());
					missionDto4.setCommandType(missionInfoEntity.getCommandType());
					// 判断停用任务是否为周期任务
					if((Constant.MISSION_LOOP_TRUE).equals(missionInfoEntity.getIsLoop())) {
						MissionLoopEntity missionLoopEntity = missionDto4.getMissionLoopEntity();
						//判断周期任务的结束时间不能小于当前时间
						Date endTime = missionLoopEntity.getEndTime();
						if(endTime.getTime()<new Date().getTime()) {
							throw new NZException(RCode.MISSION_EXECUTE_INSTRUCT_UPDATE_LOOPTIME_ERROR);
						}
					}
					missionInfoEntity.setParam(JSON.toJSONString(missionDto4));
					break;
				case UPGRADE_RECOVER:
					MissionDto missionDto5 =JSON.parseObject(missionInfoEntity.getParam(), MissionDto.class);
					List<ParamForExecuteInstruct> paramForRecover = missionDto5.getParamForExecuteInstructs();
					if(null==paramForRecover || paramForRecover.size()<=0) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					for(ParamForExecuteInstruct paramForExecuteInstruct5 : paramForRecover) {
						ValidateUtils.is(paramForExecuteInstruct5.getSource()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_RECOVER_SOURCE_ISNULL);
						ValidateUtils.is(paramForExecuteInstruct5.getCover()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_RECOVER_COVER_ISNULL);
						if(CommonUtil.isNull(paramForExecuteInstruct5.getIsAbs())) {
							paramForExecuteInstruct5.setIsAbs("0");
						}
						if (paramForExecuteInstruct5.getDeleteTmpStr() != null && paramForExecuteInstruct5.getDeleteTmpStr().trim().length() > 0) {
							// 将\r,\n,\t字符替换为空
							String deleteTmpStr = paramForExecuteInstruct5.getDeleteTmpStr().trim();
							deleteTmpStr = deleteTmpStr.replaceAll("[\n,\r,\t]", "");
							if (deleteTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
								paramForExecuteInstruct5.setDelete(deleteTmpStr.split(Constant.STR_PARAM_SEPRATOR));
							} else {
								paramForExecuteInstruct5.setDelete(new String[] { deleteTmpStr });
							}
							paramForExecuteInstruct5.setDeleteTmpStr(null);
						}
					}
					missionDto5.setCommandEnname(missionInfoEntity.getCommandEnname());
					missionDto5.setCommandType(missionInfoEntity.getCommandType());
					// 判断停用任务是否为周期任务
					if((Constant.MISSION_LOOP_TRUE).equals(missionInfoEntity.getIsLoop())) {
						MissionLoopEntity missionLoopEntity = missionDto5.getMissionLoopEntity();
						//判断周期任务的结束时间不能小于当前时间
						Date endTime = missionLoopEntity.getEndTime();
						if(endTime.getTime()<new Date().getTime()) {
							throw new NZException(RCode.MISSION_EXECUTE_INSTRUCT_RECOVER_LOOPTIME_ERROR);
						}
					}
					missionInfoEntity.setParam(JSON.toJSONString(missionDto5));
					break;
				case SINGLE_EXEC:
					MissionDto missionDto6 =JSON.parseObject(missionInfoEntity.getParam(), MissionDto.class);
					List<ParamForExecuteInstruct> paramForSingleExec = missionDto6.getParamForExecuteInstructs();
					if(null==paramForSingleExec || paramForSingleExec.size()<=0) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					for(ParamForExecuteInstruct paramForExecuteInstruct6 : paramForSingleExec) {
						ValidateUtils.is(paramForExecuteInstruct6.getExecCmd()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_SINGLEEXEC_EXECCMD_ISNULL);
						if(CommonUtil.isNull(paramForExecuteInstruct6.getReturnFlag())) {
							paramForExecuteInstruct6.setReturnFlag("0");
						}else {
							ValidateUtils.is(paramForExecuteInstruct6.getReturnPath()).notNull(RCode.MISSION_EXECUTE_INSTRUCT_SINGLEEXEC_RETURNPATH_ISNULL);
						}
						if(CommonUtil.isNull(paramForExecuteInstruct6.getMaxWaitTime())) {
							paramForExecuteInstruct6.setMaxWaitTime("3");
						}
					}
					missionDto6.setCommandEnname(missionInfoEntity.getCommandEnname());
					missionDto6.setCommandType(missionInfoEntity.getCommandType());
					// 判断停用任务是否为周期任务
					if((Constant.MISSION_LOOP_TRUE).equals(missionInfoEntity.getIsLoop())) {
						MissionLoopEntity missionLoopEntity = missionDto6.getMissionLoopEntity();
						//判断周期任务的结束时间不能小于当前时间
						Date endTime = missionLoopEntity.getEndTime();
						if(endTime.getTime()<new Date().getTime()) {
							throw new NZException(RCode.MISSION_EXECUTE_INSTRUCT_SINGLEEXEC_LOOPTIME_ERROR);
						}
					}
					missionInfoEntity.setParam(JSON.toJSONString(missionDto6));
					break;
			}
			break;
		case UGPRADE:
			MissionUpgradeType missionUpgradeType = MissionUpgradeType.getMissionUpgradeType(missionInfoEntity.getCommandEnname());
			switch(missionUpgradeType) {
				case UPGRADE_AGENT:
					MissionDto upgradeMissionDto = JSON.parseObject(missionInfoEntity.getParam(),MissionDto.class);
					ParamForUpgrade upgradeAgent = upgradeMissionDto.getParamForUpgrade();
					if(null==upgradeAgent) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					ValidateUtils.is(upgradeAgent.getCover()).notNull(RCode.MISSION_UPGRADE_COVER_ISNULL);
					if(null==files || files.length<=0) {
						throw new NZException(RCode.MISSION_UPGRADE_UPLOADFILE_ISNULL);
					}
					logger.info("UPGRADE_AGENT file upload start");
					MultipartFile uploadFile = files[0];
					long size = uploadFile.getSize();
					//判断上传文件的总大小是否超出
					if(size>104857600) {
						throw new NZException(RCode.MISSION_UPGRADE_UPLOADFILE_OUTSIZE);
					}
					ValidateUtils.is(uploadFile.getOriginalFilename()).notNull(RCode.MISSION_UPGRADE_UPLOADFILE_ISNULL);
					//处理文件推送中密码 
					if (upgradeAgent.getPw() != null && upgradeAgent.getPw().trim().length() > 0) {// 密码加密保存
						try {
							upgradeAgent.setPw(DESUtil.desEncrypt(upgradeAgent.getPw().trim()));
						} catch (Exception e) {
							//加密密码出现错误
							e.printStackTrace();
							throw new NZException(RCode.MISSION_UPGRADE_PASSWORD_HANDLE_ERROR);
						}
					}
					//获取配置文件内的上传路径
					File rootPath=null;
					try {
						rootPath = new File(ResourceUtils.getURL("classpath:").getPath());
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					String uploadPath = en.getProperty("upload.path");
				    File upload = new File(rootPath.getAbsolutePath(), uploadPath);
					// -- 上传目录不存在则创建
					if (!upload.exists()) {
						upload.mkdirs();
					}
					String path=upload+File.separator;
					//处理文件名称
					String upFileName=FileUtil.addTimeTagForUploadFileName(uploadFile.getOriginalFilename());
					//上传文件 
					File dest = new File(path + upFileName);
					 try {
						    uploadFile.transferTo(dest);
				        } catch (Exception e) {
				        	e.printStackTrace();
				        	throw new NZException(RCode.MISSION_UPGRADE_FILE_UPLOAD_ERROR);
				        }
					 logger.info("UPGRADE_AGENT file upload finish");
					String fileMd5=null;
					try {
						fileMd5 = MD5Util.getFileMD5String(dest);
					} catch (IOException e) {
						e.printStackTrace();
						throw new NZException(RCode.MISSION_UPGRADE_MD5_ERROR);
					}
					if(CommonUtil.isNull(upgradeAgent.getIsAbs())) {
						upgradeAgent.setIsAbs("0");
					}
					if(CommonUtil.isNull(upgradeAgent.getIsCreateCover())) {
						upgradeAgent.setIsCreateCover("0");
					}
					upgradeAgent.setMd5Value(fileMd5);
					upgradeAgent.setUpFileName(upFileName);
					if (upgradeAgent.getDeleteTmpStr() != null && upgradeAgent.getDeleteTmpStr().trim().length() > 0) {
						// 将\r,\n,\t字符替换为空
						String deleteTmpStr = upgradeAgent.getDeleteTmpStr().trim();
						deleteTmpStr = deleteTmpStr.replaceAll("[\n,\r,\t]", "");
						if (deleteTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
							upgradeAgent.setDelete(deleteTmpStr.split(Constant.STR_PARAM_SEPRATOR));
						} else {
							upgradeAgent.setDelete(new String[] { deleteTmpStr });
						}
						upgradeAgent.setDeleteTmpStr(null);
					}
					List<Backup> backups = upgradeAgent.getBackups();
					if(null!=backups&&backups.size()>0) {
						for(Backup backUp : backups) {
							ValidateUtils.is(backUp.getBackup()).notNull(RCode.MISSION_UPGRADE_BACKUP_PATH_ISNULL);
							ValidateUtils.is(backUp.getBackupTo()).notNull(RCode.MISSION_UPGRADE_BACKUPTO_ISNULL);
							if(CommonUtil.isNull(backUp.getIsAbs())) {
								backUp.setIsAbs("0");
							}
							if (backUp.getExceptTmpStr() != null && backUp.getExceptTmpStr().trim().length() > 0) {
								// 将\r,\n,\t字符替换为空
								String exceptTmpStr = backUp.getExceptTmpStr().trim();
								exceptTmpStr = exceptTmpStr.replaceAll("[\n,\r,\t]", "");
								if (exceptTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
									backUp.setExcept(exceptTmpStr.split(Constant.STR_PARAM_SEPRATOR));
								} else {
									backUp.setExcept(new String[] { exceptTmpStr });
								}
								backUp.setExceptTmpStr(null);
							}
							
						}
					}
					upgradeMissionDto.setCommandEnname(missionInfoEntity.getCommandEnname());
					missionInfoEntity.setParam(JSON.toJSONString(upgradeMissionDto));
					break;
				// nc升级与第三方升级逻辑一致 使用case switch穿透特性	
				case UPGRADE_OTHER:
				case UPGRADE_SERVER:
					MissionDto upgradeDto = JSON.parseObject(missionInfoEntity.getParam(),MissionDto.class);
					List<ParamForUpgrade> paramForUpgrades = upgradeDto.getParamForUpgrades();
					if(null==paramForUpgrades || paramForUpgrades.size()<=0) {
						throw new NZException(RCode.MISSION_PARAM_ERROR);
					}
					if(null==files || files.length<=0) {
						throw new NZException(RCode.MISSION_UPGRADE_UPLOADFILE_ISNULL);
					}
					long onefile = 0l;
					long sum = 0l;
					File[] removeFiles=new File[paramForUpgrades.size()];
					boolean outSizeFlag=false;
					uu:for(int i=0;i<paramForUpgrades.size();i++) {
						logger.info("UPGRADE_SERVER or UPGRADE_OTHER file upload start");
						ParamForUpgrade paramForUpgrade = paramForUpgrades.get(i);
						MultipartFile file = files[i];
						ValidateUtils.is(file.getOriginalFilename()).notNull(RCode.MISSION_UPGRADE_UPLOADFILE_ISNULL);
						//处理文件推送中密码 
						if (paramForUpgrade.getPw() != null && paramForUpgrade.getPw().trim().length() > 0) {// 密码加密保存
							try {
								paramForUpgrade.setPw(DESUtil.desEncrypt(paramForUpgrade.getPw().trim()));
							} catch (Exception e) {
								//加密密码出现错误
								e.printStackTrace();
								throw new NZException(RCode.MISSION_UPGRADE_PASSWORD_HANDLE_ERROR);
							}
						}
						//获取配置文件内的上传路径
						File upgradeRootPath=null;
						try {
							upgradeRootPath = new File(ResourceUtils.getURL("classpath:").getPath());
						} catch (FileNotFoundException e1) {
							e1.printStackTrace();
						}
						String upgradeUploadPath = en.getProperty("upload.path");
					    File upgradeUpload = new File(upgradeRootPath.getAbsolutePath(), upgradeUploadPath);
						// -- 上传目录不存在则创建
						if (!upgradeUpload.exists()) {
							upgradeUpload.mkdirs();
						}
						String upgradePath=upgradeUpload+File.separator;
						//处理文件名称
						String upgradeUpFileName=FileUtil.addTimeTagForUploadFileName(file.getOriginalFilename());
						//上传文件 
						File upgradeDest = new File(upgradePath + upgradeUpFileName);
						 try {
							    file.transferTo(upgradeDest);
					        } catch (Exception e) {
					        	e.printStackTrace();
					        	throw new NZException(RCode.MISSION_UPGRADE_FILE_UPLOAD_ERROR);
					        }
						 logger.info("UPGRADE_SERVER or UPGRADE_OTHER file upload finish");
						String upgradeFileMd5=null;
						try {
							upgradeFileMd5 = MD5Util.getFileMD5String(upgradeDest);
						} catch (IOException e) {
							e.printStackTrace();
							throw new NZException(RCode.MISSION_UPGRADE_MD5_ERROR);
						}
						paramForUpgrade.setMd5Value(upgradeFileMd5);
						paramForUpgrade.setUpFileName(upgradeUpFileName);
						
						ValidateUtils.is(paramForUpgrade.getCover()).notNull(RCode.MISSION_UPGRADE_COVER_ISNULL);
						ValidateUtils.is(paramForUpgrade.getStartupFile()).notNull(RCode.MISSION_UPGRADE_STARTUPFILE_ISNULL);
						ValidateUtils.is(paramForUpgrade.getPidFile()).notNull(RCode.MISSION_UPGRADE_PIDFILE_ISNULL);
						
						if(CommonUtil.isNull(paramForUpgrade.getIsAbs())) {
							paramForUpgrade.setIsAbs("0");
						}
						if(CommonUtil.isNull(paramForUpgrade.getIsCreateCover())) {
							paramForUpgrade.setIsCreateCover("0");
						}
						if(CommonUtil.isNull(paramForUpgrade.getForceExec())) {
							paramForUpgrade.setForceExec("0");
						}
						if(CommonUtil.isNull(paramForUpgrade.getMaxWaitTime())) {
							paramForUpgrade.setMaxWaitTime("3");
						}
						if (paramForUpgrade.getExecParamsTmpStr() != null && paramForUpgrade.getExecParamsTmpStr().trim().length() > 0) {
							// 将\r,\n,\t字符替换为空
							String execParamsTmpStr = paramForUpgrade.getExecParamsTmpStr().trim();
							execParamsTmpStr = execParamsTmpStr.replaceAll("[\n,\r,\t]", "");
							if (execParamsTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
								paramForUpgrade.setExecParams(execParamsTmpStr.split(Constant.STR_PARAM_SEPRATOR));
							} else {
								paramForUpgrade.setExecParams(new String[] { execParamsTmpStr });
							}
							paramForUpgrade.setExecParamsTmpStr(null);
						}
						if (paramForUpgrade.getDeleteTmpStr() != null && paramForUpgrade.getDeleteTmpStr().trim().length() > 0) {
							// 将\r,\n,\t字符替换为空
							String deleteTmpStr = paramForUpgrade.getDeleteTmpStr().trim();
							deleteTmpStr = deleteTmpStr.replaceAll("[\n,\r,\t]", "");
							if (deleteTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
								paramForUpgrade.setDelete(deleteTmpStr.split(Constant.STR_PARAM_SEPRATOR));
							} else {
								paramForUpgrade.setDelete(new String[] { deleteTmpStr });
							}
							paramForUpgrade.setDeleteTmpStr(null);
						}
						List<Backup> upgradeBackups = paramForUpgrade.getBackups();
						if(null!=upgradeBackups&&upgradeBackups.size()>0) {
							for(Backup backUp : upgradeBackups) {
								ValidateUtils.is(backUp.getBackup()).notNull(RCode.MISSION_UPGRADE_BACKUP_PATH_ISNULL);
								ValidateUtils.is(backUp.getBackupTo()).notNull(RCode.MISSION_UPGRADE_BACKUPTO_ISNULL);
								if(CommonUtil.isNull(backUp.getIsAbs())) {
									backUp.setIsAbs("0");
								}
								if (backUp.getExceptTmpStr() != null && backUp.getExceptTmpStr().trim().length() > 0) {
									// 将\r,\n,\t字符替换为空
									String exceptTmpStr = backUp.getExceptTmpStr().trim();
									exceptTmpStr = exceptTmpStr.replaceAll("[\n,\r,\t]", "");
									if (exceptTmpStr.contains(Constant.STR_PARAM_SEPRATOR)) {
										backUp.setExcept(exceptTmpStr.split(Constant.STR_PARAM_SEPRATOR));
									} else {
										backUp.setExcept(new String[] { exceptTmpStr });
									}
									backUp.setExceptTmpStr(null);
								}
								
							}
						}
					}
					upgradeDto.setCommandEnname(missionInfoEntity.getCommandEnname());
					//如果超出100M文件大小 则将已上传文件删除
					if(outSizeFlag) {
						logger.info("UPGRADE_SERVER or UPGRADE_OTHER outsize delete file");
						for(int j = 0; j<removeFiles.length;j++) {
							File file = removeFiles[j];
							if(file.exists()) {
								file.delete();
							}
						}
						throw new NZException(RCode.MISSION_UPGRADE_UPLOADFILE_OUTSIZE);
					}else {
						missionInfoEntity.setParam(JSON.toJSONString(upgradeDto));
					}
					break;
			}
			break;
		default:
			break;
		}
	}

	@Override
	public List<MissionInfoEntity> validateLoopMissionInfoData(Long[] missionIds, String state) {
		logger.info("validateLoopMissionInfoData content missionId:"+missionIds);
		List<MissionInfoEntity> loopMissions =new ArrayList<MissionInfoEntity>();
		for(Long missionId : missionIds) {
			MissionInfoEntity mission = baseMapper.selectById(missionId);
			// 判断任务是否为周期任务
			if(mission.getIsLoop().equals(Constant.MISSION_LOOP_FALSE)) {
				throw new NZException(RCode.MISSION_UNLOOP_MISSION_ERROR);
			}
			//已经终止的任务不可修改为运行状态
			if(state.equals(Constant.MISSION_LOOP_RUN)) {
				if(mission.getState().equals(Constant.MISSION_LOOP_STOP)) {
					throw new NZException(RCode.MISSION_LOOP_START_ERROR);
				}
			}else {
				throw new NZException(RCode.MISSION_LOOP_START_ERROR);
			}
			mission.setState(state);
			loopMissions.add(mission);
		}
		logger.info("result loopMissions :"+JSON.toJSONString(loopMissions));
		return loopMissions;
	}

	@Override
	@Transactional(readOnly = false)
	public void updateMission(MissionInfoEntity missionInfoEntity) {
		logger.info("updateMission info :"+JSON.toJSONString(missionInfoEntity));
		MissionInfoEntity updateMission =new MissionInfoEntity();
		String state = missionInfoEntity.getState();
		if(!CommonUtil.isNull(state)) {
			MissionInfoEntity mission = baseMapper.selectById(missionInfoEntity.getId());
			if(mission.getIsLoop().equals(Constant.MISSION_LOOP_TRUE)) {
				if(!(state.equals(Constant.MISSION_LOOP_RUN)||state.equals(Constant.MISSION_LOOP_SUSPEND))) {
					throw new NZException(RCode.MISSION_LOOP_STATE_ERROR);
				}
				updateMission.setState(state);
			}else {
				throw new NZException(RCode.MISSION_UNLOOP_MISSION_ERROR);
			}
		}
		updateMission.setName(missionInfoEntity.getName());
		updateMission.setId(missionInfoEntity.getId());
		updateMission.setRemark(missionInfoEntity.getRemark());
		this.updateById(updateMission);
	}

	/**
	 * 检查选中的任务是否符合逆向任务的条件：是否升级部署+非逆向任务+是否任务完成+是否有备份（从大到小判断，先任务类型，再完成，再备份）
	 */
	@Override
	public MissionInfoEntity getConverseMissionById(Long id) {
		logger.info("ConverseMissionById is :"+id);
		MissionInfoEntity mission = baseMapper.selectById(id);
		Long missionId = mission.getId();
		if(mission.getType().equals(MissionType.UGPRADE.getMissionTypeId())) {
			//当前任务类型为升级部署
			ParamForUpgrade paramForUpgrade = JSON.parseObject(mission.getParam(), ParamForUpgrade.class);
			Long version = paramForUpgrade.getVersion();
			if(null != version) {
				MissionResultEntity missionResult = missionResultDao.selectById(missionId);
				if(null != missionResult) {
					List<Backup> backups = paramForUpgrade.getBackups();
					if(null == backups || backups.size()<=0) {
						throw new NZException(RCode.MISSION_UPGRADE_BACKUP_NULL);
					}
				}else {
					throw new NZException(RCode.MISSION_UPGRADE_NOT_FINISH);
				}
			}else {
				throw new NZException(RCode.MISSION_UPGRADE_NOT_REVERSED_ERROR);
			}
		}else {
			throw new NZException(RCode.MISSION_UPGRADE_TYPE_ERROR);
		}
		
		// 通过以上校验 任务可以新建逆向任务 查询任务信息 用于界面展示
		// 校验节点组有效性  补全系统名称、节点组名称信息   补全用户组信息
		List<NodeGroupInfoEntity> nodeGroupInfoEntitys = baseMapper.selectNodeGroupByMissionid(missionId);
		logger.info("query nodeGroups content :"+JSON.toJSONString(nodeGroupInfoEntitys));
		List<NodeInfoEntity> nodeInfoEntitys = baseMapper.selectNodeByMissionid(missionId);
		logger.info("query nodes content :"+JSON.toJSONString(nodeInfoEntitys));
		// 查询任务的具体属性
		MissionInfoEntity missionInfoEntity = baseMapper.selectById(missionId);
		logger.info("query mission info :"+JSON.toJSONString(missionInfoEntity));
		//没有备份的任务指令要移除
		List<ParamForUpgrade> paramForUpgrades = JSON.parseArray(missionInfoEntity.getParam(), ParamForUpgrade.class);
		for(int i=0; i<paramForUpgrades.size();i++) {
			List<Backup> backups = paramForUpgrades.get(i).getBackups();
			if(null == backups ||CommonUtil.isNull(backups)) {
				paramForUpgrades.remove(i);
			}
		}
		missionInfoEntity.setNodeGroups(nodeGroupInfoEntitys);
		missionInfoEntity.setNodes(nodeInfoEntitys);
		logger.info("result mission content :"+missionInfoEntity);
		return missionInfoEntity;
	}
}