package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.detect.dao.DeteWarnInfoDao;
import com.nis.modules.detect.dao.DeteWarnRelDao;
import com.nis.modules.detect.entity.*;
import com.nis.modules.detect.service.*;
import com.nis.modules.sys.service.PermissionInfoService;
import com.nis.modules.sys.service.UserUsergroupService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("deteWarnInfoService")
public class DeteWarnInfoServiceImpl extends DeteCommonServiceImpl<DeteWarnInfoDao, DeteWarnInfoEntity> implements DeteWarnInfoService {
    private Logger log= LoggerFactory.getLogger(DeteWarnInfoServiceImpl.class);
    @Autowired
    private DeteWarnRelService deteWarnRelService;
    @Autowired
    private DeteWarnRelDao deteWarnRelDao;
    @Autowired
    private PermissionInfoService permissionInfoService;
    @Autowired
    private DeteTypeInfoService deteTypeInfoService;
    @Autowired
    private DeteSetInfoService deteSetInfoService;
    @Autowired
    private DeteMetaService deteTypeMetaService;

    @Autowired
    private UserUsergroupService userUsergroupService;

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

    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        List<Long> queryAllowedSetIds = deteSetInfoService.queryAllowedSetIds();
        log.debug("allowed setInfo id{}",queryAllowedSetIds);
        Long[] allowedSetIds=new Long[queryAllowedSetIds.size()];
        allowedSetIds=queryAllowedSetIds.toArray(allowedSetIds);
        Long typeId = null;
        Long setId = null;
        try {
            typeId = Long.parseLong((String) params.get("typeId"));
            setId = Long.parseLong((String) params.get("setId"));
        } catch (Exception e) {
            //do nothing
        }
        String name = (String) params.get("name");
        IPage<DeteWarnInfoEntity> page = new Query<DeteWarnInfoEntity>().getPage(params);
        page=this.getBaseMapper().queryList(page,typeId,setId,name,allowedSetIds);


        List<DeteWarnInfoEntity> list = page.getRecords();
        for (DeteWarnInfoEntity warnInfo : list) {
            DeteWarnRelEntity deteWarnRelEntity = deteWarnRelDao.queryWarnRel(warnInfo.getId());
            if (Constant.WARN_REL_TYPE.equals(deteWarnRelEntity.getRelType())) {
                DeteTypeInfoEntity deteTypeInfoEntity = deteTypeInfoService.getById(deteWarnRelEntity.getRelIds());
                warnInfo.setDeteType(deteTypeInfoEntity);
            } else {
                String[] split = deteWarnRelEntity.getRelIds().split(",");
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

        return new PageUtils(page);
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

    @Override
    public void deleteByTypeId(Long typeId) {

        List<DeteWarnRelEntity> warnRels=deteWarnRelService.queryByTypeId(typeId);
        if(warnRels!=null&& warnRels.size()>0){
            Long[] warnIds=new Long[warnRels.size()];
            for(int i=0;i<warnRels.size();i++){
                DeteWarnRelEntity warnRel=warnRels.get(i);
                warnIds[i]=warnRel.getWarnId();
            }

            deleteEntity(warnIds);
        }

    }
}