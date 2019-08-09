package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.modules.detect.dao.DeteWarnInfoDao;
import com.nis.modules.detect.entity.*;
import com.nis.modules.detect.service.*;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.UserGroupEntity;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("deteWarnInfoService")
public class DeteWarnInfoServiceImpl extends ServiceImpl<DeteWarnInfoDao, DeteWarnInfoEntity> implements DeteWarnInfoService {
    @Autowired
    private DeteWarnRelService deteWarnRelService;
    @Autowired
    private PermissionInfoService permissionInfoService;
    @Autowired
    private DeteTypeInfoService deteTypeInfoService;
    @Autowired
    private DeteSetInfoService deteSetInfoService;
    @Autowired
    private DeteTypeMetaService deteTypeMetaService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEntity(DeteWarnInfoEntity deteWarnInfoEntity) {
        Long userId = ShiroUtils.getUserId();
        Date date = new Date();
        deteWarnInfoEntity.setOperator(userId);
        deteWarnInfoEntity.setOpTime(date);
        this.save(deteWarnInfoEntity);
        Long deteTypeId = deteWarnInfoEntity.getDeteTypeId();
        Long[] deteSetIds = deteWarnInfoEntity.getDeteSetIds();

        List<DeteWarnRelEntity> deteWarnRelEntities = new ArrayList<DeteWarnRelEntity>();
        //当type和set均不为空时，保存set关系，set为空时保存type关系
        if (deteSetIds != null && deteSetIds.length > 0) {
            for (Long setId : deteSetIds) {
                DeteWarnRelEntity deteWarnRelEntity = new DeteWarnRelEntity();
                deteWarnRelEntity.setOperator(userId);
                deteWarnRelEntity.setOpTime(date);
                deteWarnRelEntity.setRelType(Constant.WARN_REL_SET);//关联方式，1、type，2、set
                deteWarnRelEntity.setRelId(setId);
                deteWarnRelEntity.setWarnId(deteWarnInfoEntity.getId());

                deteWarnRelEntities.add(deteWarnRelEntity);
            }
        } else {
            DeteWarnRelEntity deteWarnRelEntity = new DeteWarnRelEntity();
            deteWarnRelEntity.setOperator(userId);
            deteWarnRelEntity.setOpTime(date);
            deteWarnRelEntity.setRelType(Constant.WARN_REL_TYPE);//关联方式，1、type，2、set
            deteWarnRelEntity.setRelId(deteTypeId);
            deteWarnRelEntity.setWarnId(deteWarnInfoEntity.getId());

            deteWarnRelEntities.add(deteWarnRelEntity);
        }

        deteWarnRelService.saveBatch(deteWarnRelEntities);
        //维护权限表 无限制则不维护权限表
        List<PermissionInfoEntity> permissionInfos = new ArrayList<PermissionInfoEntity>();
        switch (deteWarnInfoEntity.getViewLevel()) {//1:用户组，2：系统内全部人员，3：无限制，默认2
            case Constant.VIEWLEVEL_UG:
                List<UserGroupEntity> usergroups = ShiroUtils.getUserEntity().getUsergroups();
                for (UserGroupEntity userGroup : usergroups) {
                    PermissionInfoEntity permissionInfoEntity = new PermissionInfoEntity();
                    permissionInfoEntity.setType(Constant.PM_TYPE_DETEWARN);//'1、dete_type,2、dete_set,3、dete_warn,4、mission'
                    permissionInfoEntity.setTypeId(deteWarnInfoEntity.getId());
                    permissionInfoEntity.setViewLevel(deteWarnInfoEntity.getViewLevel());
                    permissionInfoEntity.setRelId(userGroup.getId());
                    permissionInfoEntity.setOperator(userId);
                    permissionInfoEntity.setOpTime(date);

                    permissionInfos.add(permissionInfoEntity);
                }
                break;
            case Constant.VIEWLEVEL_SYS:
                Long currSystemId = ShiroUtils.getCurrSystemId();
                PermissionInfoEntity permissionInfoEntity = new PermissionInfoEntity();
                permissionInfoEntity.setType(Constant.PM_TYPE_DETEWARN);//'1、dete_type,2、dete_set,3、dete_warn,4、mission'
                permissionInfoEntity.setTypeId(deteWarnInfoEntity.getId());
                permissionInfoEntity.setViewLevel(deteWarnInfoEntity.getViewLevel());
                permissionInfoEntity.setRelId(currSystemId);
                permissionInfoEntity.setOperator(userId);
                permissionInfoEntity.setOpTime(date);

                permissionInfos.add(permissionInfoEntity);
                break;
            case Constant.VIEWLEVEL_ALL:

                break;
        }
        if (permissionInfos.size() > 0) {
            permissionInfoService.saveBatch(permissionInfos);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEntity(DeteWarnInfoEntity deteWarnInfoEntity) {
        Long userId = ShiroUtils.getUserId();
        Date date = new Date();
        deteWarnInfoEntity.setOperator(userId);
        deteWarnInfoEntity.setOpTime(date);
        this.updateById(deteWarnInfoEntity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEntity(Long... ids) {
        Long userId = ShiroUtils.getUserId();
        Date date = new Date();

        UpdateWrapper<DeteWarnInfoEntity> warnInfoUpdateWrapper = new UpdateWrapper<>();
        warnInfoUpdateWrapper.set("operator", userId);
        warnInfoUpdateWrapper.set("op_time", date);
        warnInfoUpdateWrapper.set("del_flag", Constant.DELFLAG_TRUE);
        warnInfoUpdateWrapper.in("id", ids);
        this.update(warnInfoUpdateWrapper);

        //处理权限表
        UpdateWrapper<PermissionInfoEntity> permissionUpdateWrapper = new UpdateWrapper<>();
        permissionUpdateWrapper.set("operator", userId);
        permissionUpdateWrapper.set("op_time", date);
        permissionUpdateWrapper.set("del_flag", Constant.DELFLAG_TRUE);
        permissionUpdateWrapper.eq("type", Constant.PM_TYPE_DETEWARN);
        permissionUpdateWrapper.in("type_id", ids);
    }

    @Override
    public boolean checkNameExists(String name, Long id) {
        QueryWrapper<DeteWarnInfoEntity> warnInfoQueryWrapper = new QueryWrapper<>();
        warnInfoQueryWrapper.ne(id != null, "id", id);
        warnInfoQueryWrapper.eq("name", name);
        List<DeteWarnInfoEntity> list = this.list(warnInfoQueryWrapper);
        return list != null && list.size() > 0;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Integer curpage = 1;
        Integer pageSize = 10;
        Long typeId = null;
        Long setId = null;
        try {
            curpage = Integer.parseInt((String) params.get("page"));
            pageSize = Integer.parseInt((String) params.get("limit"));
            typeId = Long.parseLong((String) params.get("typeId"));
            setId = Long.parseLong((String) params.get("setId"));
        } catch (Exception e) {
            //do nothing
        }
        String name = (String) params.get("name");
        String sidx = (String) params.get("sidx");
        String order = (String) params.get("order");
        int start = (curpage - 1) * pageSize;
        Long userId = ShiroUtils.getUserId();
        Long currSystemId = ShiroUtils.getCurrSystemId();

        List<DeteWarnInfoEntity> list = this.baseMapper.queryList(typeId, setId, name, userId, currSystemId, sidx, order, start, pageSize);
        for (DeteWarnInfoEntity warnInfo : list) {
            if (Constant.WARN_REL_TYPE.equals(warnInfo.getRelType())) {
                DeteTypeInfoEntity deteTypeInfoEntity = deteTypeInfoService.getById(Long.parseLong(warnInfo.getRelIds()));
                warnInfo.setDeteType(deteTypeInfoEntity);
            } else {
                String[] split = warnInfo.getRelIds().split(",");
                Long[] ids = new Long[split.length];
                for (int i = 0; i < split.length; i++) {
                    ids[i] = Long.parseLong(split[i]);
                }
                QueryWrapper<DeteSetInfoEntity> setInfoQueryWrapper = new QueryWrapper<>();
                setInfoQueryWrapper.in("id", ids);
                List<DeteSetInfoEntity> setInfos = deteSetInfoService.list(setInfoQueryWrapper);
                warnInfo.setDeteSet(setInfos);
                DeteTypeInfoEntity deteTypeInfoEntity = deteTypeInfoService.getById(setInfos.get(0).getTypeId());
                warnInfo.setDeteType(deteTypeInfoEntity);
            }
        }

        int count = this.baseMapper.queryCount(typeId, setId, name, userId, currSystemId);
        return new PageUtils(list, count, pageSize, curpage);
    }

    @Override
    public DeteWarnInfoEntity queryDetail(Long id) {
        DeteWarnInfoEntity warnInfo = this.getById(id);
        QueryWrapper<DeteWarnRelEntity> warnRelQueryWrapper = new QueryWrapper<DeteWarnRelEntity>();
        warnRelQueryWrapper.eq("warn_id", id);

        List<DeteWarnRelEntity> warnRelInfos = deteWarnRelService.list(warnRelQueryWrapper);
        if (warnRelInfos != null && warnRelInfos.size() > 0) {
            if (warnRelInfos.size() == 1 && Constant.WARN_REL_TYPE.equals(warnRelInfos.get(0).getRelType())) {
                DeteWarnRelEntity deteWarnRelEntity = warnRelInfos.get(0);
                DeteTypeInfoEntity deteTypeInfoEntity = deteTypeInfoService.getById(deteWarnRelEntity.getRelId());
                warnInfo.setDeteType(deteTypeInfoEntity);
            } else {
                Long[] setIds = new Long[warnRelInfos.size()];
                for (int i = 0; i < warnRelInfos.size(); i++) {
                    setIds[i] = warnRelInfos.get(i).getRelId();
                }
                QueryWrapper<DeteSetInfoEntity> setInfoQueryWrapper = new QueryWrapper<>();
                setInfoQueryWrapper.in("id",setIds);
                List<DeteSetInfoEntity> list = deteSetInfoService.list(setInfoQueryWrapper);
                warnInfo.setDeteSet(list);
                DeteTypeInfoEntity deteTypeInfoEntity = deteTypeInfoService.getById(list.get(0).getTypeId());
                warnInfo.setDeteType(deteTypeInfoEntity);
            }
        }

        DeteTypeMetaEntity meta = deteTypeMetaService.getById(warnInfo.getMetaId());
        warnInfo.setMeta(meta);

        return warnInfo;
    }
}