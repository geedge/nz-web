package com.nis.modules.detectType.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.detectType.dao.DeteTypeInfoDao;
import com.nis.modules.detectType.entity.DeteTypeInfoEntity;
import com.nis.modules.detectType.entity.DeteTypeMetaEntity;
import com.nis.modules.detectType.entity.DeteTypeParamEntity;
import com.nis.modules.detectType.service.DeteTypeInfoService;
import com.nis.modules.detectType.service.DeteTypeMetaService;
import com.nis.modules.detectType.service.DeteTypeParamService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service("deteTypeInfoService")
public class DeteTypeInfoServiceImpl extends DeteTypeCommonServiceImpl<DeteTypeInfoDao, DeteTypeInfoEntity> implements DeteTypeInfoService, ApplicationRunner {
    @Autowired
    private DeteTypeMetaService deteTypeMetaService;
    @Autowired
    private  DeteTypeParamService deteTypeParamService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String deteTypeName=(String)params.get("name");

        IPage<DeteTypeInfoEntity> page = this.page(
                new Query<DeteTypeInfoEntity>().getPage(params),
                new QueryWrapper<DeteTypeInfoEntity>()
                        .like(StringUtils.isNotBlank(deteTypeName),"name", deteTypeName)
        );

        return new PageUtils(page);
    }

    @Override
    public DeteTypeInfoEntity queryDetail(Integer id) {
        DeteTypeInfoEntity deteTypeInfo = this.getById(id);
        if(deteTypeInfo==null||deteTypeInfo.getId()==null){
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
        ValidateUtils.is(deteTypeInfo).notNull(RCode.DETE_ERROR);
        Long userId = ShiroUtils.getUserId();//操作员
        deteTypeInfo.setOperator(userId);
        Date date = new Date(); //操作时间
        deteTypeInfo.setOpTime(date);
        ValidateUtils.is(deteTypeInfo.getName()).notNull(RCode.TYPE_NAME_ISNULL);
        boolean typeNameExists = this.checkFieldExists("name", deteTypeInfo.getName());
        if(typeNameExists){
            throw new NZException(RCode.TYPE_NAME_EXISTS);
        }
        String tableName="DI_"+deteTypeInfo.getName();
        //检查是否存在表
        boolean tableExists=checkTablesExists(tableName);
        if(tableExists){
            throw new NZException(RCode.TYPE_TABLE_EXISTS);
        }

        deteTypeInfo.setTableName(tableName.toUpperCase());
        this.save(deteTypeInfo);

        ValidateUtils.is(deteTypeInfo.getId()).notNull(RCode.TYPE_SAVE_IDISNULL);

        List<DeteTypeMetaEntity> deteTypeMetas = deteTypeInfo.getDeteTypeMetas();

        for(DeteTypeMetaEntity deteTypeMetaEntity:deteTypeMetas){
            ValidateUtils.is(deteTypeMetaEntity.getName()).notNull(RCode.META_NAME_ISNULL);
            boolean metaNameExists = deteTypeMetaService.checkFieldExists("name", deteTypeMetaEntity.getName(), deteTypeInfo.getId());
            if(metaNameExists){
                throw new NZException(RCode.META_NAME_EXISTS);
            }
            //field_name字段默认为元数据名称去除特殊字符
            String deteTypeMetaName = deteTypeMetaEntity.getName().replaceAll("[^0-9a-zA-Z_]", "").toUpperCase();
            //防止名称全为特殊字符，去除后为空 ps：经测试，字段名可以为单个_ 或以数字开头或以_开头
            ValidateUtils.is(deteTypeMetaName).notNull(RCode.META_NAME_INVAILD);
            //字段名称不能包含数据库关键字
            if(DB_KEYWORD_SET.contains(deteTypeMetaName)){
                throw new NZException(RCode.META_NAME_HAVEKEYWORD);
            }
            deteTypeMetaEntity.setFieldName(deteTypeMetaName);
            deteTypeMetaEntity.setTypeId(deteTypeInfo.getId());
            deteTypeMetaEntity.setOperator(userId);
            deteTypeMetaEntity.setOpTime(date);
            deteTypeMetaService.save(deteTypeMetaEntity);
        }
        List<DeteTypeParamEntity> deteTypeParams = deteTypeInfo.getDeteTypeParams();

        for(DeteTypeParamEntity deteTypeParamEntity:deteTypeParams){
            ValidateUtils.is(deteTypeParamEntity.getName()).notNull(RCode.PARAM_NAME_ISNULL);
            ValidateUtils.is(deteTypeParamEntity.getParamIndex()).notNull(RCode.PARAM_INDEX_ISNULL);
            boolean paramNameExists = deteTypeParamService.checkFieldExists("name", deteTypeParamEntity.getName(), deteTypeInfo.getId());
            if(paramNameExists){
                throw new NZException(RCode.PARAM_NAME_EXISTS);
            }

            deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
            deteTypeParamEntity.setOperator(userId);
            deteTypeParamEntity.setOpTime(date);
            deteTypeParamService.save(deteTypeParamEntity);
        }

        createTable(deteTypeInfo);

    }

    /**
    * @Description通过表名判断表是否存在
    * @Author  rui
    * @Date   2019/7/23
    * @Param
    * @Return
    * @Exception
    */
    private boolean checkTablesExists(String tableName) {
        Map<String,String> tables=this.getBaseMapper().queryTables(tableName);

        return tables!=null&&tables.size()>0;
    }

    private void createTable(DeteTypeInfoEntity detectTypeInfo){
        this.getBaseMapper().createTable(detectTypeInfo);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            String filePath=DeteTypeInfoServiceImpl.class.getResource("/").getPath()+"DbKeyword.txt";
            List<String> strings = FileUtils.readLines(new File(filePath),"UTF-8");
            for(String key:strings){
                DB_KEYWORD_SET.add(key.toLowerCase());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
