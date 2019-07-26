package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.detect.dao.DeteTypeInfoDao;
import com.nis.modules.detect.entity.*;
import com.nis.modules.detect.service.DeteTypeInfoService;
import com.nis.modules.detect.service.DeteTypeMetaMappingService;
import com.nis.modules.detect.service.DeteTypeMetaService;
import com.nis.modules.detect.service.DeteTypeParamService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service("deteTypeInfoService")
public class DeteTypeInfoServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeInfoDao, DeteTypeInfoEntity> implements DeteTypeInfoService {
    @Autowired
    private DeteTypeMetaService deteTypeMetaService;
    @Autowired
    private DeteTypeParamService deteTypeParamService;
    @Autowired
    private DeteTypeMetaMappingService deteTypeMetaMappingService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String deteTypeName = (String) params.get("name");

        IPage<DeteTypeInfoEntity> page = this.page(
                new Query<DeteTypeInfoEntity>().getPage(params),
                new QueryWrapper<DeteTypeInfoEntity>()
                        .like(StringUtils.isNotBlank(deteTypeName), "name", deteTypeName)
        );

        return new PageUtils(page);
    }

    @Override
    public DeteTypeInfoEntity queryDetail(Long id) {
        DeteTypeInfoEntity deteTypeInfo = this.getById(id);
        if (deteTypeInfo == null || deteTypeInfo.getId() == null) {
            return null;
        }
        List<DeteTypeMetaEntity> deteTypeMetas = deteTypeMetaService.list(new QueryWrapper<DeteTypeMetaEntity>().eq("type_id", deteTypeInfo.getId()));
        deteTypeInfo.setDeteTypeMetas(deteTypeMetas);

        List<DeteTypeParamEntity> deteTypeParams = deteTypeParamService.list(new QueryWrapper<DeteTypeParamEntity>().eq("type_id", deteTypeInfo.getId()));
        deteTypeInfo.setDeteTypeParams(deteTypeParams);

        return deteTypeInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEntity(DeteTypeInfoEntity deteTypeInfo) {
        Long userId = ShiroUtils.getUserId();//操作员
        deteTypeInfo.setOperator(userId);
        Date date = new Date(); //操作时间
        deteTypeInfo.setOpTime(date);


        List<DeteTypeMetaEntity> deteTypeMetas = deteTypeInfo.getDeteTypeMetas();

        Set<String> fieldNameFilter = new HashSet<String>();
        for (DeteTypeMetaEntity deteTypeMetaEntity : deteTypeMetas) {
            //field_name字段默认为 f_元数据名称去除特殊字符
            String fieldName = "f_" + this.formatString(deteTypeMetaEntity.getName());
            //防止处理后的字段名称重复
            if (fieldNameFilter.contains(fieldName)) {
                throw new NZException(RCode.META_FIELDNAME_EXISTS);
            } else {
                deteTypeMetaEntity.setFieldName(fieldName);
                fieldNameFilter.add(fieldName);
            }
            deteTypeMetaEntity.setTypeId(deteTypeInfo.getId());
            deteTypeMetaEntity.setOperator(userId);
            deteTypeMetaEntity.setOpTime(date);
        }


        List<DeteTypeParamEntity> deteTypeParams = deteTypeInfo.getDeteTypeParams();

        for (int i = 0; i < deteTypeParams.size(); i++) {
            DeteTypeParamEntity deteTypeParamEntity = deteTypeParams.get(i);
            deteTypeParamEntity.setParamIndex(i);
            deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
            deteTypeParamEntity.setOperator(userId);
            deteTypeParamEntity.setOpTime(date);
        }


        createTable(deteTypeInfo);
        //为避免在DDL语句发出时会自动commit，故将所有的数据操作放在DDL后
        this.save(deteTypeInfo);

        for (DeteTypeMetaEntity deteTypeMetaEntity : deteTypeMetas) {
            deteTypeMetaEntity.setTypeId(deteTypeInfo.getId());
        }
        deteTypeMetaService.saveBatch(deteTypeMetas);
        for (int i = 0; i < deteTypeParams.size(); i++) {
            DeteTypeParamEntity deteTypeParamEntity = deteTypeParams.get(i);
            deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
        }
        deteTypeParamService.saveBatch(deteTypeParams);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEntity(DeteTypeInfoEntity deteTypeInfo) {
        DeteTypeInfoEntity old = this.getById(deteTypeInfo.getId());
        List<DeteTypeMetaEntity> deteTypeMetas = deteTypeInfo.getDeteTypeMetas();
        List<DeteTypeParamEntity> deteTypeParams = deteTypeInfo.getDeteTypeParams();
        Long userId = ShiroUtils.getUserId();//操作员
        deteTypeInfo.setOperator(userId);
        Date date = new Date(); //操作时间
        deteTypeInfo.setOpTime(date);
        deteTypeInfo.setTableName(old.getTableName());

        //需要修改表字段
        List<DeteTypeMetaEntity> metaUpdateList = new ArrayList<DeteTypeMetaEntity>();
        //需要添加表字段
        List<DeteTypeMetaEntity> metaAddList = new ArrayList<DeteTypeMetaEntity>();
        //需要删除表字段
        List<DeteTypeMetaEntity> metaDelList = new ArrayList<DeteTypeMetaEntity>();
        //需要添加索引
        List<DeteTypeMetaEntity> indexAddList = new ArrayList<DeteTypeMetaEntity>();
        //需要删除索引
        List<DeteTypeMetaEntity> indexDelList = new ArrayList<DeteTypeMetaEntity>();
        Set<String> fieldNameFilter = new HashSet<String>();
        if(deteTypeMetas!=null){
            for (DeteTypeMetaEntity deteTypeMetaEntity : deteTypeMetas) {
                deteTypeMetaEntity.setOperator(userId);
                deteTypeMetaEntity.setOpTime(date);
                //field_name字段默认为 f_元数据名称去除特殊字符
                String fieldName = "f_" + this.formatString(deteTypeMetaEntity.getName());
                //防止处理后的字段名称重复
                if (fieldNameFilter.contains(fieldName)) {
                    throw new NZException(RCode.META_FIELDNAME_EXISTS);
                } else {

                    fieldNameFilter.add(fieldName);
                }

                if (deteTypeMetaEntity.getId() != null) {
                    if("1".equals(deteTypeMetaEntity.getDelFlag())){
                        metaDelList.add(deteTypeMetaEntity);
                    }else{
                        metaUpdateList.add(deteTypeMetaEntity);
                    }
                } else {
                    //字段名称不允许修改，故只在此set
                    deteTypeMetaEntity.setFieldName(fieldName);
                    metaAddList.add(deteTypeMetaEntity);
                }
            }


        }

        this.judgeTableFields(metaUpdateList,metaAddList,metaDelList,indexAddList,indexDelList);

        //处理添加索引
        if(indexAddList.size()>0){
            this.getBaseMapper().addTableIndexs(deteTypeInfo.getTableName(), indexAddList);
        }
        //处理删除索引
        if(indexDelList.size()>0){
            this.getBaseMapper().delTableIndexs(deteTypeInfo.getTableName(), indexDelList);
        }
        //处理字段添加
        if(metaAddList.size()>0){
            this.getBaseMapper().addTableFields(deteTypeInfo.getTableName(), metaAddList);
        }
        //处理字段删除
        if(metaDelList.size()>0){
            this.getBaseMapper().delTableFields(deteTypeInfo.getTableName(),metaDelList);
        }
        //处理字段修改
        //如果是已经创建了监测设置的，则不允许修改字段类型和长度
        if(!checkDetectSetConfigExists(deteTypeInfo.getId())&&metaUpdateList.size()>0){
            this.getBaseMapper().alterTableFields(deteTypeInfo.getTableName(),metaUpdateList);
        }

        this.updateById(deteTypeInfo);
        for (DeteTypeMetaEntity deteTypeMetaEntity : deteTypeMetas) {
            if(deteTypeMetaEntity.getId()!=null){
                if(deteTypeMetaEntity.getDelFlag()!=null&&!Constant.DELFLAG_FALSE.equals(deteTypeMetaEntity.getDelFlag())){
                    deteTypeMetaService.removeById(deteTypeMetaEntity.getId());
                }else{
                    deteTypeMetaService.updateById(deteTypeMetaEntity);
                }
            }else{
                deteTypeMetaEntity.setTypeId(deteTypeInfo.getId());
                deteTypeMetaService.save(deteTypeMetaEntity);
            }
        }

        if(deteTypeParams!=null&&deteTypeParams.size()>0){
            for (int i = 0; i < deteTypeParams.size(); i++) {
                DeteTypeParamEntity deteTypeParamEntity = deteTypeParams.get(i);
                deteTypeParamEntity.setOperator(userId);
                deteTypeParamEntity.setOpTime(date);
                deteTypeParamEntity.setParamIndex(i);
                if(deteTypeParamEntity.getId() == null){

                    deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
                }
                if(deteTypeParamEntity.getId()!=null){
                    if(deteTypeParamEntity.getDelFlag()!=null&&!Constant.DELFLAG_FALSE.equals(deteTypeParamEntity.getDelFlag())){
                        deteTypeMetaService.removeById(deteTypeParamEntity.getId());
                    }else{
                        deteTypeParamService.updateById(deteTypeParamEntity);
                    }
                }else{
                    deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
                    deteTypeParamService.save(deteTypeParamEntity);
                }
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEntity(DeteTypeInfoEntity deteTypeInfoEntity) {
        DeteTypeInfoEntity oldType = this.getById(deteTypeInfoEntity.getId());
        //删除detectType
        this.update(new UpdateWrapper<DeteTypeInfoEntity>()
                .eq("id",deteTypeInfoEntity.getId())
                .set("del_flag", Constant.DELFLAG_TRUE));
        //删除detectTypeMeta
        deteTypeMetaService.update(new UpdateWrapper<DeteTypeMetaEntity>()
                .eq("type_id",deteTypeInfoEntity.getId())
                .set("del_flag",Constant.DELFLAG_TRUE));
        //删除detectTypeParam
        deteTypeParamService.update(new UpdateWrapper<DeteTypeParamEntity>()
                .eq("type_id",deteTypeInfoEntity.getId())
                .set("del_flag",Constant.DELFLAG_TRUE));
        //删除detectSetInfo 相关

        //drop对应的表
        this.getBaseMapper().dropTable(oldType.getTableName());
    }


    /**
     * @Description通过表名判断表是否存在
     * @Author rui
     * @Date 2019/7/23
     * @Param
     * @Return
     * @Exception
     */
    public boolean checkTablesExists(String tableName) {
        Map<String, String> tables = this.getBaseMapper().queryTables(tableName);

        return tables != null && tables.size() > 0;
    }

    public void createTable(DeteTypeInfoEntity detectTypeInfo) {

        //fieldType==3表示该字段为临时字段，不入库
        List<DeteTypeMetaEntity> deteTypeMetas = detectTypeInfo.getDeteTypeMetas();
        Iterator<DeteTypeMetaEntity> iterator = deteTypeMetas.iterator();
        while(iterator.hasNext()){
            DeteTypeMetaEntity next = iterator.next();
            if(next.getFieldType()==null||"3".equals(next.getFieldType())){
                iterator.remove();
            }
        }
        detectTypeInfo.setDeteTypeMetas(deteTypeMetas);
        this.getBaseMapper().createTable(detectTypeInfo);
    }

    public boolean checkDetectSetConfigExists(Long id) {
        int count = this.getBaseMapper().checkDetectSetConfigExists(id);
        return count > 0;
    }



    /**
     * @Description校验前段传递的数据
     * @Author rui
     * @Date 2019/7/24
     * @Param
     * @Return
     * @Exception
     */
    public void validatePassDate(DeteTypeInfoEntity deteTypeInfo) {
        ValidateUtils.is(deteTypeInfo).notNull(RCode.DETE_ERROR);

        ValidateUtils.is(deteTypeInfo.getName()).notNull(RCode.TYPE_NAME_ISNULL);
        boolean typeNameExists = this.checkFieldExists(deteTypeInfo.getId(),"name", deteTypeInfo.getName());
        if (typeNameExists) {
            throw new NZException(RCode.TYPE_NAME_EXISTS);
        }
        if (deteTypeInfo.getId() == null) {//说明更新操作，表已经存在，表名不允许修改
            String tableName = "DI_" + deteTypeInfo.getName();
            tableName = this.formatString(tableName);
            //检查是否存在表
            boolean tableExists = this.checkTablesExists(tableName);
            if (tableExists) {
                throw new NZException(RCode.TYPE_TABLE_EXISTS);
            }

            deteTypeInfo.setTableName(tableName.toUpperCase());
        }
        Set<String> nameFilter = new HashSet<String>();
        List<DeteTypeMetaEntity> deteTypeMetas = deteTypeInfo.getDeteTypeMetas();
        for (DeteTypeMetaEntity deteTypeMetaEntity : deteTypeMetas) {
            ValidateUtils.is(deteTypeMetaEntity.getName()).notNull(RCode.META_NAME_ISNULL);
            String name = deteTypeMetaEntity.getName();
            if (nameFilter.contains(name)) {
                throw new NZException(RCode.META_NAME_EXISTS);
            } else {
                if(deteTypeMetaEntity.getDelFlag()!=null&&Constant.DELFLAG_FALSE.equals(deteTypeMetaEntity.getDelFlag())){
                    nameFilter.add(name);
                }
            }


        }
        nameFilter.clear();
        List<DeteTypeParamEntity> deteTypeParams = deteTypeInfo.getDeteTypeParams();
        for (int i = 0; i < deteTypeParams.size(); i++) {
            DeteTypeParamEntity deteTypeParamEntity = deteTypeParams.get(i);
            if (deteTypeParamEntity.getId() == null) {
                ValidateUtils.is(deteTypeParamEntity.getName()).notNull(RCode.PARAM_NAME_ISNULL);
                deteTypeParamEntity.setParamIndex(i);
                String name = deteTypeParamEntity.getName();
                if (nameFilter.contains(name)) {
                    throw new NZException(RCode.PARAM_NAME_EXISTS);
                } else {
                    if(deteTypeParamEntity.getDelFlag()!=null&&Constant.DELFLAG_FALSE.equals(deteTypeParamEntity.getDelFlag())){
                        nameFilter.add(name);
                    }
                }
            }
        }
        nameFilter = null;
    }
    private void judgeTableFields(List<DeteTypeMetaEntity> metaUpdateList,
                                  List<DeteTypeMetaEntity> metaAddList, List<DeteTypeMetaEntity> metaDelList,
                                  List<DeteTypeMetaEntity> indexAddList, List<DeteTypeMetaEntity> indexDelList) {
        if(metaUpdateList.size()<1){
            return ;
        }
        Iterator<DeteTypeMetaEntity> iterator = metaUpdateList.iterator();

        while(iterator.hasNext()){
            DeteTypeMetaEntity deteTypeMetaEntity = iterator.next();
            DeteTypeMetaEntity oldMeta = deteTypeMetaService.getById(deteTypeMetaEntity.getId());
            //判断是否更改fieldType
            if(!oldMeta.getFieldType().equals(deteTypeMetaEntity.getFieldType())) {
                if ("3".equals(oldMeta.getFieldType())) {//临时字段-->入库字段
                    iterator.remove();
                    metaAddList.add(deteTypeMetaEntity);
                    continue;
                }
                if ("3".equals(deteTypeMetaEntity.getFieldType())) {//入库字段-->临时字段
                    iterator.remove();
                    metaDelList.add(deteTypeMetaEntity);
                    continue;
                }

                if ("1".equals(oldMeta.getFieldType())) {//有索引-->无索引
                    indexDelList.add(deteTypeMetaEntity);
                }

                if ("1".equals(deteTypeMetaEntity.getFieldType())) {//无索引-->有索引
                    indexAddList.add(deteTypeMetaEntity);
                }
            }

            //判断dataTpye
            String dataType = deteTypeMetaEntity.getDataType();
            if(oldMeta.getDataType().equals(dataType)){
                if(oldMeta.getDataLenth()==deteTypeMetaEntity.getDataLenth()){
                    iterator.remove();
                    continue;
                }
            }else{
                //数据长度相等，且都为数值型，不做修改（都是decimal）
                if(oldMeta.getDataLenth()==deteTypeMetaEntity.getDataLenth()&&!"3".equals(oldMeta.getDataType())&&!"3".equals(dataType)){
                    iterator.remove();
                    continue;
                }
            }

        }

        //至此，metaUpdateList中的数据都是需要修改字段类型或字段长度的
    }
}
