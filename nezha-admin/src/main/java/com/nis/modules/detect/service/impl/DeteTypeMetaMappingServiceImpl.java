package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.utils.*;
import com.nis.modules.detect.dao.DeteTypeMetaMappingDao;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMappingEntity;
import com.nis.modules.detect.entity.DeteTypeMetaMapppingVO;
import com.nis.modules.detect.service.DeteTypeMetaMappingService;
import com.nis.modules.detect.service.DeteTypeMetaService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("deteTypeMetaMappingService")
public class DeteTypeMetaMappingServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeMetaMappingDao, DeteTypeMetaMappingEntity> implements DeteTypeMetaMappingService{
    @Autowired
    private DeteTypeMetaService deteTypeMetaService;
    @Override
    public PageUtils queryMetaMappingPage(Map<String, Object> params) {
        String type = (String) params.get("type");

        IPage<DeteTypeMetaMappingEntity> page =this.page(
                new Query<DeteTypeMetaMappingEntity>().getPage(params),
                new QueryWrapper<DeteTypeMetaMappingEntity>()
                        .like(StringUtils.isNotBlank(type), "type", type)
        );
        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDicts(List<DeteTypeMetaMappingEntity> mappings) {
        this.saveBatch(mappings);
    }

    @Override
    public void updateDict(DeteTypeMetaMapppingVO deteTypeMetaMapppingVO) {
        Long userId = ShiroUtils.getUserId();
        Date date=new Date();

        List<DeteTypeMetaMappingEntity> mappings = deteTypeMetaMapppingVO.getMappings();

        for(DeteTypeMetaMappingEntity mapping:mappings){
            mapping.setType(deteTypeMetaMapppingVO.getType());
            mapping.setOperator(userId);
            mapping.setOpTime(date);

            if(mapping.getId()==null){
                this.save(mapping);
            }else{
                UpdateWrapper<DeteTypeMetaMappingEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.set("operator",ShiroUtils.getUserId());
                updateWrapper.set("op_time",new Date());
                updateWrapper.set(mapping.getDelFlag()!=null,"del_flag",mapping.getDelFlag());
                updateWrapper.eq("id",mapping.getId());
                this.update(mapping,updateWrapper);
            }
        }
    }

    @Override
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
                return R.ok();
            }

    }

    private boolean checkTypeUsed(String ... type) {
        List<DeteTypeMetaEntity> list = deteTypeMetaService.list(new QueryWrapper<DeteTypeMetaEntity>().in("dic_type", type));
        return list!=null&&list.size()>0;
    }
}
