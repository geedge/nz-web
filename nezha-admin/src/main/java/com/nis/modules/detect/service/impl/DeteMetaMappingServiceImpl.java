package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.utils.*;
import com.nis.modules.detect.dao.DeteTypeMetaMappingDao;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMapppingVO;
import com.nis.modules.detect.service.DeteMetaMappingService;
import com.nis.modules.detect.service.DeteMetaService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service("deteTypeMetaMappingService")
public class DeteMetaMappingServiceImpl extends DeteCommonServiceImpl<DeteTypeMetaMappingDao, DeteTypeMetaMappingEntity> implements DeteMetaMappingService {
    private Logger log= LoggerFactory.getLogger(DeteMetaMappingServiceImpl.class);
    @Autowired
    private DeteMetaService deteTypeMetaService;
    @Override
    public PageUtils queryMetaMappingPage(Map<String, Object> params) {
        String type = (String) params.get("type");

        IPage<DeteTypeMetaMappingEntity> page =this.page(
                new Query<DeteTypeMetaMappingEntity>().getPage(params),
                new QueryWrapper<DeteTypeMetaMappingEntity>()
                        .like(StringUtils.isNotBlank(type), "type", type)
        );

        List<DeteTypeMetaMappingEntity> records = page.getRecords();
        HashMap<String, DeteTypeMetaMapppingVO> metaMappingFilter = new HashMap<>();
        List<DeteTypeMetaMapppingVO> mappingInfos=new ArrayList<>();
        for(DeteTypeMetaMappingEntity metaMappingEntity:records){
            String typeName = metaMappingEntity.getType();
            if(metaMappingFilter.containsKey(typeName)){
                DeteTypeMetaMapppingVO deteTypeMetaMapppingVO = metaMappingFilter.get(typeName);
                List<DeteTypeMetaMappingEntity> mappings = deteTypeMetaMapppingVO.getMappings();
                mappings.add(metaMappingEntity);
            }else{
                DeteTypeMetaMapppingVO deteTypeMetaMapppingVO=new DeteTypeMetaMapppingVO();
                deteTypeMetaMapppingVO.setType(typeName);
                List<DeteTypeMetaMappingEntity> list=new ArrayList<DeteTypeMetaMappingEntity>();
                list.add(metaMappingEntity);
                deteTypeMetaMapppingVO.setMappings(list);
                metaMappingFilter.put(typeName,deteTypeMetaMapppingVO);
                mappingInfos.add(deteTypeMetaMapppingVO);
            }
        }


        return new PageUtils(mappingInfos,(int)page.getTotal(),(int)page.getSize(),(int)page.getCurrent());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDicts(List<DeteTypeMetaMappingEntity> mappings) {
        this.saveBatch(mappings);
        log.info("save detect type meta mapping info success! size[{}]",mappings.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDict(DeteTypeMetaMapppingVO deteTypeMetaMapppingVO) {
        String oldTypeName=null;
        List<DeteTypeMetaMappingEntity> mappings = deteTypeMetaMapppingVO.getMappings();
        for(DeteTypeMetaMappingEntity mapping:mappings){
            if(mapping.getId()!=null){
                DeteTypeMetaMappingEntity oldMapping = this.getById(mapping.getId());
                oldTypeName=oldMapping.getType();
            }
            break;
        }
        int saveCount=0;
        int updateCount=0;
        for(DeteTypeMetaMappingEntity mapping:mappings){
            mapping.setType(oldTypeName); //type不允许修改

            if(mapping.getId()==null){
                this.save(mapping);
                saveCount++;
            }else{
                UpdateWrapper<DeteTypeMetaMappingEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.set("operator",ShiroUtils.getUserId());
                updateWrapper.set("op_time",new Date());
                updateWrapper.set(mapping.getDelFlag()!=null,"del_flag",mapping.getDelFlag());
                updateWrapper.eq("id",mapping.getId());
                this.update(mapping,updateWrapper);
                updateCount++;
            }
        }
        log.info("update deteTypeMetaMapping success! new[{}] , update[{}]"+saveCount,updateCount);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R deleteDic(String ... types) {
            if(checkTypeUsed(types)){
                return R.error(RCode.DICT_TYPE_USED);
            }else{
                UpdateWrapper<DeteTypeMetaMappingEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.set("del_flag", Constant.DELFLAG_TRUE);
                updateWrapper.set("operator",ShiroUtils.getUserId());
                updateWrapper.set("op_time",new Date());
                updateWrapper.in("type",types);
                this.update(updateWrapper);
                log.info("delete deteTypeMetaMapping {} success!",Arrays.asList(types));
                return R.ok();
            }

    }

    private boolean checkTypeUsed(String ... type) {
        List<DeteTypeMetaEntity> list = deteTypeMetaService.list(new QueryWrapper<DeteTypeMetaEntity>().in("dic_type", type));
        return list!=null&&list.size()>0;
    }

    @Override
    public boolean checkMetaMappingTypeExists(String type) {
        return checkFieldExists(null,"type",type);
    }
}
