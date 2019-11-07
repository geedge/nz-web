package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.common.utils.SqlHelper.KeywordsType;
import com.nis.modules.detect.dao.DeteTypeInfoDao;
import com.nis.modules.detect.entity.DeteSetInfoEntity;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.entity.DeteTypeMetaEntity;
import com.nis.modules.detect.entity.DeteTypeParamEntity;
import com.nis.modules.detect.service.*;
import com.nis.modules.sys.shiro.ShiroUtils;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.auth.NTLMEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.channels.MulticastChannel;
import java.util.*;

@Service("deteTypeInfoService")
public class DeteTypeInfoServiceImpl extends DeteCommonServiceImpl<DeteTypeInfoDao, DeteTypeInfoEntity> implements DeteTypeInfoService, ApplicationRunner {
    private Logger log= LoggerFactory.getLogger(DeteTypeInfoServiceImpl.class);
    @Autowired
    private DeteMetaService deteTypeMetaService;
    @Autowired
    private DeteParamService deteTypeParamService;
    @Autowired
    private DeteMetaMappingService deteTypeMetaMappingService;
    @Autowired
    private DeteSetInfoService deteSetInfoService;
    @Autowired
    private DeteWarnInfoService deteWarnInfoService;

    private SQLUtils sqlUtils;
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
    public void saveEntity(DeteTypeInfoEntity deteTypeInfo,MultipartFile file) {
        Long userId = ShiroUtils.getUserId();//操作员
        deteTypeInfo.setOperator(userId);
        Date date = new Date(); //操作时间
        deteTypeInfo.setOpTime(date);
        deteTypeInfo.setBuildIn(Constant.SYSTEM_BUILDIN_NO);
        if(Constant.DETE_DETEMETHOD_SCRIPT.equals(deteTypeInfo.getMethod())){

            String content=this.getUploadFileJson(file);
            this.uploadFile(content,file);
            deteTypeInfo.setContent(content);
        }

        List<DeteTypeMetaEntity> deteTypeMetas = deteTypeInfo.getDeteTypeMetas();

        if(deteTypeMetas!=null&&deteTypeMetas.size()>0){
            Set<String> fieldNameFilter = new HashSet<String>();
            for (int i=0;i< deteTypeMetas.size();i++) {
                DeteTypeMetaEntity deteTypeMetaEntity =deteTypeMetas.get(i);
                //field_name字段默认为 f_元数据名称去除特殊字符
                String fieldName = ("f_" + this.formatString(deteTypeMetaEntity.getName())).toUpperCase();
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
                deteTypeMetaEntity.setSort(i);
            }
        }



        List<DeteTypeParamEntity> deteTypeParams = deteTypeInfo.getDeteTypeParams();
        if(deteTypeParams!=null&& deteTypeParams.size()>0){
            for (int i = 0; i < deteTypeParams.size(); i++) {
                DeteTypeParamEntity deteTypeParamEntity = deteTypeParams.get(i);
                deteTypeParamEntity.setParamIndex(i);
                deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
                deteTypeParamEntity.setOperator(userId);
                deteTypeParamEntity.setOpTime(date);
            }
        }



        createTable(deteTypeInfo);
        log.info("create table [{}]",deteTypeInfo.getTableName());
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
        log.info("save detectType success! typeId [{}]",deteTypeInfo.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEntity(DeteTypeInfoEntity deteTypeInfo,MultipartFile file) {
        DeteTypeInfoEntity old = this.getById(deteTypeInfo.getId());
        List<DeteTypeMetaEntity> deteTypeMetas = deteTypeInfo.getDeteTypeMetas();
        List<DeteTypeParamEntity> deteTypeParams = deteTypeInfo.getDeteTypeParams();
        Long userId = ShiroUtils.getUserId();//操作员
        deteTypeInfo.setOperator(userId);
        Date date = new Date(); //操作时间
        deteTypeInfo.setOpTime(date);
        deteTypeInfo.setTableName(old.getTableName());
        deteTypeInfo.setBuildIn(old.getBuildIn());//buildIn 字段不允许修改
        if(Constant.DETE_DETEMETHOD_SCRIPT.equals(deteTypeInfo.getMethod())&&file!=null&&!file.isEmpty()){
            this.delUploadFile(old.getContent());//删除旧文件
            String content=this.getUploadFileJson(file);
            this.uploadFile(content,file);
            deteTypeInfo.setContent(content);
        }
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
        if (deteTypeMetas != null) {
            for (DeteTypeMetaEntity deteTypeMetaEntity : deteTypeMetas) {
                deteTypeMetaEntity.setOperator(userId);
                deteTypeMetaEntity.setOpTime(date);
                //field_name字段默认为 f_元数据名称去除特殊字符
                String fieldName = ("f_" + this.formatString(deteTypeMetaEntity.getName())).toUpperCase();
                //防止处理后的字段名称重复
                if (fieldNameFilter.contains(fieldName)) {
                    throw new NZException(RCode.META_FIELDNAME_EXISTS);
                } else {

                    fieldNameFilter.add(fieldName);
                }

                if (deteTypeMetaEntity.getId() != null) {
                    if (Constant.DELFLAG_TRUE.equals(deteTypeMetaEntity.getDelFlag())) {
                        metaDelList.add(deteTypeMetaEntity);
                    } else {
                        metaUpdateList.add(deteTypeMetaEntity);
                    }
                } else {
                    //字段名称不允许修改，故只在此set
                    deteTypeMetaEntity.setFieldName(fieldName);
                    metaAddList.add(deteTypeMetaEntity);
                }
            }


        }

        this.judgeTableFields(metaUpdateList, metaAddList, metaDelList, indexAddList, indexDelList);
        log.info("modify field{},add field{},delete field{},add index{},delete index{}",metaUpdateList,metaAddList,metaDelList,indexAddList,indexDelList);
        //处理添加索引
        if (indexAddList.size() > 0) {
            Map<String, String[]> map = new HashMap<String, String[]>();
            for (DeteTypeMetaEntity meta : indexAddList) {
                String indexName = deteTypeInfo.getTableName().toUpperCase() + "_" + meta.getFieldName().toUpperCase() + "_INDEX";
                String fieldName = meta.getFieldName();
                map.put(indexName, new String[]{fieldName});
            }
            addIndexs(deteTypeInfo.getTableName(), map);
        }
        //处理删除索引
        if (indexDelList.size() > 0) {
            String[] indexs = new String[indexDelList.size()];
            for (int i = 0; i < indexDelList.size(); i++) {
                DeteTypeMetaEntity meta = indexDelList.get(i);
                String indexName = deteTypeInfo.getTableName().toUpperCase() + "_" + meta.getFieldName().toUpperCase() + "_INDEX";
                indexs[i] = indexName;
            }
            dropIndexs(deteTypeInfo.getTableName(), indexs);
        }
        //处理字段添加
        if (metaAddList.size() > 0) {
            Map<String, String> fields = new LinkedHashMap<String, String>();
            Map<String, String[]> indexs = new HashMap<String, String[]>();

            preFieldAdd(deteTypeInfo.getTableName(), fields, indexs, metaAddList);

            dropFields(deteTypeInfo.getTableName(),  fields.keySet().toArray(new String[fields.size()]));

            String sql = sqlUtils.addFields(deteTypeInfo.getTableName(), fields);
            this.getBaseMapper().addTableFields(sql);

            addIndexs(deteTypeInfo.getTableName(), indexs);
        }
        //处理字段删除
        if (metaDelList.size() > 0) {
            String[] fields = new String[metaDelList.size()];
            for (int i = 0; i < metaDelList.size(); i++) {
                fields[i] = metaDelList.get(i).getFieldName();
            }

            dropFields(deteTypeInfo.getTableName(), fields);
        }
        //处理字段修改
        //如果是已经创建了监测设置的，则不允许修改字段类型和长度
        if (!checkDetectSetConfigExists(deteTypeInfo.getId()) && metaUpdateList.size() > 0) {
            Map<String, String> fields = new LinkedHashMap<String, String>();
            preFieldAdd(deteTypeInfo.getTableName(), fields, null,metaUpdateList );

            String sql = sqlUtils.modifyFields(deteTypeInfo.getTableName(), fields);
            this.getBaseMapper().alterTableFields(sql);
        }

        this.updateById(deteTypeInfo);
        for (int i=0;i< deteTypeMetas.size();i++) {
            DeteTypeMetaEntity deteTypeMetaEntity=deteTypeMetas.get(i);
            deteTypeMetaEntity.setSort(i);
            if (deteTypeMetaEntity.getId() != null) {
                if (deteTypeMetaEntity.getDelFlag() != null && !Constant.DELFLAG_FALSE.equals(deteTypeMetaEntity.getDelFlag())) {
                    deteTypeMetaService.removeById(deteTypeMetaEntity.getId());
                } else {
                    if(deteTypeMetaService.checkNameExists(deteTypeMetaEntity)){
                        throw new NZException(RCode.META_NAME_EXISTS);
                    }
                    if(deteTypeMetaService.checkFieldNameExists(deteTypeMetaEntity)){
                        throw new NZException(RCode.META_FIELDNAME_EXISTS);
                    }
                    deteTypeMetaService.updateById(deteTypeMetaEntity);
                }
            } else {
                deteTypeMetaEntity.setTypeId(deteTypeInfo.getId());
                if(deteTypeMetaService.checkNameExists(deteTypeMetaEntity)){
                    throw new NZException(RCode.META_NAME_EXISTS);
                }
                if(deteTypeMetaService.checkFieldNameExists(deteTypeMetaEntity)){
                    throw new NZException(RCode.META_FIELDNAME_EXISTS);
                }
                deteTypeMetaService.save(deteTypeMetaEntity);
            }
        }

        if (deteTypeParams != null && deteTypeParams.size() > 0) {
            for (int i = 0; i < deteTypeParams.size(); i++) {
                DeteTypeParamEntity deteTypeParamEntity = deteTypeParams.get(i);

                deteTypeParamEntity.setOperator(userId);
                deteTypeParamEntity.setOpTime(date);
                deteTypeParamEntity.setParamIndex(i);
                if (deteTypeParamEntity.getId() == null) {

                    deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
                }
                if(deteTypeParamService.checkNameExists(deteTypeParamEntity)){
                    throw new NZException(RCode.PARAM_NAME_EXISTS);
                }
                if (deteTypeParamEntity.getId() != null) {
                    if (deteTypeParamEntity.getDelFlag() != null && !Constant.DELFLAG_FALSE.equals(deteTypeParamEntity.getDelFlag())) {
                        deteTypeParamService.removeById(deteTypeParamEntity.getId());
                    } else {
                        deteTypeParamService.updateById(deteTypeParamEntity);
                    }
                } else {
                    deteTypeParamEntity.setTypeId(deteTypeInfo.getId());
                    deteTypeParamService.save(deteTypeParamEntity);
                }
            }
        }
        log.info("deteType update success!");
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEntity(Long... ids) {
        Long userId = ShiroUtils.getUserId();
        Date date=new Date();

        for (Long id :ids){
            DeteTypeInfoEntity oldType = this.getById(id);
            if(oldType!=null&& StringUtils.isNotBlank(oldType.getTableName())&&checkTablesExists(oldType.getTableName())){
                //drop对应的表
                this.dropTable(oldType.getTableName());
                //如果是脚本监测，删除脚本
                if(Constant.DETE_DETEMETHOD_SCRIPT.equals(oldType.getMethod())){
                    this.delUploadFile(oldType.getContent());
                }
            }

        }
        //删除detectType
        this.update(new UpdateWrapper<DeteTypeInfoEntity>()
                .in("id", ids)
                .set("del_flag", Constant.DELFLAG_TRUE)
                .set("operator",userId)
                .set("op_time",date));
        //删除detectTypeMeta
        deteTypeMetaService.update(new UpdateWrapper<DeteTypeMetaEntity>()
                .in("type_id", ids)
                .set("del_flag", Constant.DELFLAG_TRUE)
                .set("operator",userId)
                .set("op_time",date));
        //删除detectTypeParam
        deteTypeParamService.update(new UpdateWrapper<DeteTypeParamEntity>()
                .in("type_id", ids)
                .set("del_flag", Constant.DELFLAG_TRUE)
                .set("operator",userId)
                .set("op_time",date));
        //删除detectSetInfo 相关
        for(Long id:ids){
            deteSetInfoService.deleteByTypeId(id);
        }
        //删除deteWarnInfo相关
        for(Long id:ids){
            deteWarnInfoService.deleteByTypeId(id);
        }

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
        String sql = sqlUtils.queryTables(tableName);
        Map<String, String> tables = this.getBaseMapper().queryTables(sql);

        return tables != null && tables.size() > 0;
    }

    public void createTable(DeteTypeInfoEntity detectTypeInfo) {
        String tableName = detectTypeInfo.getTableName();

        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("ID", sqlUtils.getRestrict(KeywordsType.PRIMARYKEY,null,null));
        fields.put("TIME", sqlUtils.getRestrict(KeywordsType.DATE,null,null));
        fields.put("TYPE_ID", sqlUtils.getRestrict(KeywordsType.NUMBER,20,null));
        fields.put("SET_ID", sqlUtils.getRestrict(KeywordsType.NUMBER,20,null));
        fields.put("DEVICE_ID", sqlUtils.getRestrict(KeywordsType.NUMBER,20,null));
        fields.put("TAG", sqlUtils.getRestrict(KeywordsType.NUMBER,20,null));

        Map<String, String[]> indexs = new HashMap<String, String[]>();
        indexs.put(tableName.toUpperCase() + "_TIME_INDEX", new String[]{"TIME"});
        indexs.put(tableName.toUpperCase() + "_TYPEID_INDEX", new String[]{"TYPE_ID"});
        indexs.put(tableName.toUpperCase() + "_SETID_INDEX", new String[]{"SET_ID"});
        indexs.put(tableName.toUpperCase() + "_DEVICEID_INDEX", new String[]{"DEVICE_ID"});
        indexs.put(tableName.toUpperCase() + "_TAG_INDEX", new String[]{"TAG"});

        List<DeteTypeMetaEntity> deteTypeMetas = detectTypeInfo.getDeteTypeMetas();
        if(deteTypeMetas!=null && deteTypeMetas.size()>0){
            preFieldAdd(tableName, fields, indexs, deteTypeMetas);

            String createTableSql = sqlUtils.createTable(tableName, fields);
            this.getBaseMapper().createTable(createTableSql);

            addIndexs(tableName, indexs);
        }


    }

    private void preFieldAdd(String tableName, Map<String, String> fields, Map<String, String[]> indexs, List<DeteTypeMetaEntity> deteTypeMetas) {
        for (DeteTypeMetaEntity meta : deteTypeMetas) {
            String fieldName = meta.getFieldName();
            String fieldType = meta.getFieldType();
            String dataType = meta.getDataType();
            Integer dataLen = meta.getDataLenth();
            //fieldType==3 临时字段不入库
            if ("3".equals(fieldType)) {
                continue;
            }

            if ("1".equals(fieldType) && StringUtils.isNotBlank(fieldName) && indexs != null) {
                String indexName = tableName.toUpperCase() + "_" + fieldName.toUpperCase() + "_INDEX";
                indexs.put(indexName, new String[]{fieldName});
            }
            if (fields != null) {
                if ("3".equals(dataType)) {

                    fields.put(fieldName, sqlUtils.getRestrict(KeywordsType.STRING,dataLen,null) );
                } else {

                    fields.put(fieldName, sqlUtils.getRestrict(KeywordsType.COMPUTE,dataLen,2));
                }
            }
        }
    }

    public void dropFields(String tableName, String... fields) {
        String sql = sqlUtils.delFields(tableName, fields);
        this.getBaseMapper().delTableFields(sql);
    }

    public void addIndexs(String tableName, Map<String, String[]> indexs) {
        String sql = sqlUtils.addIndexs(tableName, indexs);
        this.getBaseMapper().addTableIndexs(sql);
    }

    public void dropIndexs(String tableName, String[] indexNames) {

        String sql = sqlUtils.delIndexs(tableName, indexNames);

        this.getBaseMapper().delTableIndexs(sql);
    }

    public void dropTable(String tableName) {
        String sql = sqlUtils.dropTable(tableName);
        this.getBaseMapper().dropTable(sql);
    }


    /**
    * @Description 校验监测类别下是否已经存在监测设置
    * @Author  rui
    * @Date   2019/8/30
    * @Param
    * @Return
    * @Exception
    */
    public boolean checkDetectSetConfigExists(Long id) {
        int count = this.getBaseMapper().checkDetectSetConfigExists(id);
        return count > 0;
    }


    /**
    * @Description 校验监测方式合法性
    * @Author  rui
    * @Date   2019/8/30
    * @Param
    * @Return
    * @Exception
    */
    public boolean checkDeteMethodIsValide(DeteTypeInfoEntity deteTypeInfo){

        return checkFieldIsValide(DIC_DETEMETHOD,deteTypeInfo.getMethod());
    }

    /**
    * @Description 校验监测类别元数据字段类型合法性
    * @Author  rui
    * @Date   2019/8/30
    * @Param
    * @Return
    * @Exception
    */
    public boolean checkFieldTypeIsValide(DeteTypeInfoEntity deteTypeInfo){
        for (DeteTypeMetaEntity deteTypeMetaEntity:deteTypeInfo.getDeteTypeMetas()){
            if(!deteTypeMetaService.checkFieldTypeIsValide(deteTypeMetaEntity)){
                return false;
            }
        }
        return true;
    }

    /**
    * @Description 校验监测类别元数据数据类型合法性
    * @Author  rui
    * @Date   2019/8/30
    * @Param
    * @Return
    * @Exception
    */
    public boolean checkDataTypeIsValide(DeteTypeInfoEntity deteTypeInfo){
        for (DeteTypeMetaEntity deteTypeMetaEntity:deteTypeInfo.getDeteTypeMetas()){
            if(!deteTypeMetaService.checkDataTypeIsValide(deteTypeMetaEntity)){
                return false;
            }
        }
        return true;
    }

    public boolean isBuildIn(DeteTypeInfoEntity deteTypeInfoEntity){
        DeteTypeInfoEntity oldType = this.getById(deteTypeInfoEntity.getId());
        return Constant.SYSTEM_BUILDIN_YES.equals(oldType.getBuildIn());
    }
    /**
     * @Description校验前段传递的数据
     * @Author rui
     * @Date 2019/7/24
     * @Param
     * @Return
     * @Exception
     */
    public void validatePassDate(DeteTypeInfoEntity deteTypeInfo, MultipartFile file) {
        ValidateUtils.is(deteTypeInfo).notNull(RCode.DETE_ERROR);

        if(!checkDeteMethodIsValide(deteTypeInfo)){
            throw new NZException(RCode.TYPE_METHOD_INVALID);
        }
        if(Constant.DETE_DETEMETHOD_SCRIPT.equals(deteTypeInfo.getMethod())){
            if(!StringUtils.isNotBlank(deteTypeInfo.getContent()) && (file==null||file.isEmpty())){
                throw new NZException(RCode.DETE_UPLOAD_FILE_ISNULL);
            }
        }
        if(!checkFieldTypeIsValide(deteTypeInfo)){
            throw new NZException(RCode.META_FIELDTYPE_INVALID);
        }

        if(!checkDataTypeIsValide(deteTypeInfo)){
            throw new NZException(RCode.META_DATATYPE_INVALID);
        }

        if(!checkViewLevelValide(deteTypeInfo.getViewLevel())){
            throw new NZException(RCode.VIEWLEVEL_INVALID);
        }
        ValidateUtils.is(deteTypeInfo.getName()).notNull(RCode.TYPE_NAME_ISNULL);
        boolean typeNameExists = this.checkFieldExists(deteTypeInfo.getId(), "name", deteTypeInfo.getName());
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
        if(deteTypeMetas==null||deteTypeMetas.size()<1){
            throw new NZException(RCode.NOT_NULL_ERROR);
        }
        for (DeteTypeMetaEntity deteTypeMetaEntity : deteTypeMetas) {
            ValidateUtils.is(deteTypeMetaEntity.getName()).notNull(RCode.META_NAME_ISNULL);
            String name = deteTypeMetaEntity.getName();
            if (nameFilter.contains(name)) {
                throw new NZException(RCode.META_NAME_EXISTS);
            } else {
                if (deteTypeMetaEntity.getDelFlag() != null && Constant.DELFLAG_FALSE.equals(deteTypeMetaEntity.getDelFlag())) {
                    nameFilter.add(name);
                }
            }


        }
        nameFilter.clear();
        List<DeteTypeParamEntity> deteTypeParams = deteTypeInfo.getDeteTypeParams();
        if(deteTypeParams==null||deteTypeParams.size()<1){
            throw  new NZException(RCode.NOT_NULL_ERROR);
        }
        for (int i = 0; i < deteTypeParams.size(); i++) {
            DeteTypeParamEntity deteTypeParamEntity = deteTypeParams.get(i);
            if (deteTypeParamEntity.getId() == null) {
                ValidateUtils.is(deteTypeParamEntity.getName()).notNull(RCode.PARAM_NAME_ISNULL);
                deteTypeParamEntity.setParamIndex(i);
                String name = deteTypeParamEntity.getName();
                if (nameFilter.contains(name)) {
                    throw new NZException(RCode.PARAM_NAME_EXISTS);
                } else {
                    if (deteTypeParamEntity.getDelFlag() != null && Constant.DELFLAG_FALSE.equals(deteTypeParamEntity.getDelFlag())) {
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
        if (metaUpdateList.size() < 1) {
            return;
        }
        Iterator<DeteTypeMetaEntity> iterator = metaUpdateList.iterator();

        while (iterator.hasNext()) {
            DeteTypeMetaEntity deteTypeMetaEntity = iterator.next();
            DeteTypeMetaEntity oldMeta = deteTypeMetaService.getById(deteTypeMetaEntity.getId());
            //判断是否更改fieldType
            if (!oldMeta.getFieldType().equals(deteTypeMetaEntity.getFieldType())) {
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
            if (oldMeta.getDataType().equals(dataType)) {
                if (oldMeta.getDataLenth() == deteTypeMetaEntity.getDataLenth()) {
                    iterator.remove();
                    continue;
                }
            } else {
                //数据长度相等，且都为数值型，不做修改（都是decimal）
                if (oldMeta.getDataLenth() == deteTypeMetaEntity.getDataLenth() && !"3".equals(oldMeta.getDataType()) && !"3".equals(dataType)) {
                    iterator.remove();
                    continue;
                }
            }

        }

        //至此，metaUpdateList中的数据都是需要修改字段类型或字段长度的
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        sqlUtils=SpringContextUtils.getBean("getSqlUtils",SQLUtils.class);
    }
}
