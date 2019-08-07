package com.nis.modules.mission.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.mission.entity.MissionInfoEntity;
import com.nis.modules.sys.entity.PermissionInfoEntity;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

/**
 * 任务信息表
 *
 * @author th
 * @email ${email}
 * @date 2019-07-18 10:37:19
 */
public interface MissionInfoService extends IService<MissionInfoEntity> {

    PageUtils queryPage(Page page,MissionInfoEntity missionInfoEntity);
    
    void validateMissionInfoData(MissionInfoEntity missionInfoEntity,MultipartFile[] files);
    
    //根据任务id获取任务详情
    MissionInfoEntity getMissionById(Integer id);
    
    List<MissionInfoEntity> queryByPermission(PermissionInfoEntity entity);
    
    void saveMission(MissionInfoEntity missionInfoEntity);
    
    List<MissionInfoEntity> validateLoopMissionInfoData(Long[] missionIds,String state);
    
    void updateMission(MissionInfoEntity missionInfoEntity);
}

