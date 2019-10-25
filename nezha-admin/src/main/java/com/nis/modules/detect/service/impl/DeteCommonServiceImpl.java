package com.nis.modules.detect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.detect.entity.DeteTypeInfoEntity;
import com.nis.modules.detect.service.DeteCommonService;
import com.nis.modules.detect.service.DeteTypeInfoService;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.entity.UserUsergroupEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.UserUsergroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.*;

public class DeteCommonServiceImpl<M extends BaseMapper<T>,T> extends ServiceImpl<M ,T> implements DeteCommonService<T> , ApplicationRunner {

    @Autowired
    private UserUsergroupService userUsergroupService;
    @Autowired
    private SysDictService sysDictService;
    @Autowired
    private DeteTypeInfoService deteTypeInfoService;

    public static Map<String,Set<String>> sysDic=new HashMap<String, Set<String>>();

    protected final String DIC_VIEWLEVEL="viewLevel";
    protected final String DIC_DETEMETHOD="dtmethond";
    protected final String DIC_FIELDTYPE="fieldType";
    protected final String DIC_DATATYPE="dataType";
    protected final String DIC_TRIGGERTYPE="triggerType";
    protected final String DIC_WARNMODE="warnMode";
    protected final String DIC_STATTYPE="statType";
    protected final String DIC_SYMBOL="symbol";
    protected final String DIC_WARNLEVEL="warnLevel";
    protected final String DIC_CALLBACKTYPE="callbackType";
    /**
    * @Description 查询表字段是否存在相同的值 不检查自身（update时）
    * @Author  rui
    * @Date   2019/8/16
    * @Param
    * @Return
    * @Exception
    */
    public boolean checkFieldExists(Long id,String field,String value){
        if(StringUtils.isEmpty(field)||StringUtils.isEmpty(value)){
            throw new NZException(RCode.DETE_ERROR);
        }
        List<T> list = this.list(new QueryWrapper<T>()
                .ne(id!=null,"id",id)
                .eq(field, value));

        return list!=null&&list.size()>0;
    }
    /**
     * @Description去除字符串中的特殊字符
     * @Author  rui
     * @Date   2019/7/26
     * @Param
     * @Return
     * @Exception
     */
    public String formatString(String str) {

        return str.replaceAll("\\W", "").toUpperCase();
    }

    /**
     * @Description 返回viewLevel=1的queryWrapper
     * @Author  rui
     * @Date   2019/8/16
     * @Param type -->  Constant.PM_TYPE_DETESET = "2";Constant.PM_TYPE_DETEWARN = "3";
     * @Return
     * @Exception
     */
    public QueryWrapper<PermissionInfoEntity> getPermissionInfoVL1QueryWrapper(String type,Long userId){
        QueryWrapper<PermissionInfoEntity> permissionInfoVL1QueryWrapper = new QueryWrapper<>();
        permissionInfoVL1QueryWrapper.eq("type",type);
        permissionInfoVL1QueryWrapper.eq("view_level",Constant.VIEWLEVEL_UG);

        //查询当前用户所在用户组
        QueryWrapper<UserUsergroupEntity> userUsergroupQueryWrapper = new QueryWrapper<>();
        userUsergroupQueryWrapper.eq("user_id",userId);
        List<UserUsergroupEntity> userUserGroupList = userUsergroupService.list(userUsergroupQueryWrapper);
        Long[] groupIds=new Long[userUserGroupList.size()];
        for(int i=0;i<userUserGroupList.size();i++){
            groupIds[i]=userUserGroupList.get(i).getGroupId();
        }

        permissionInfoVL1QueryWrapper.in("rel_id",groupIds);
        return permissionInfoVL1QueryWrapper;
    }

    /**
    * @Description 返回viewLevel=2的queryWrapper
    * @Author  rui
    * @Date   2019/8/16
    * @Param
    * @Return
    * @Exception
    */
    public QueryWrapper<PermissionInfoEntity> getPermissionInfoVL2QueryWrapper(String type,Long systemId){
        QueryWrapper<PermissionInfoEntity> permissionInfoVL2QueryWrapper = new QueryWrapper<>();
        permissionInfoVL2QueryWrapper.eq("type", type);
        permissionInfoVL2QueryWrapper.eq("view_level",Constant.VIEWLEVEL_SYS);
        permissionInfoVL2QueryWrapper.eq("rel_id",systemId);
        return permissionInfoVL2QueryWrapper;
    }

    public boolean checkNameExists(String name,Long id){
        List<T> list = this.list(new QueryWrapper<T>()
                .ne(id!=null,"id",id)
                .eq("name", name)
                .ne("state",'1'));

        return list!=null&&list.size()>0;
    }

    public boolean checkFieldIsValide(String type,String code){

        return sysDic.get(type).contains(code);
    }

    /**
    * @Description 校验viewLevel合法性
    * @Author  rui
    * @Date
    * @Param  [code]
    * @Return boolean
    * @Exception
    */
    public boolean checkViewLevelValide(String code){
        return checkFieldIsValide(DIC_VIEWLEVEL,code);
    }

    @Override
    public boolean checkTypeExists(Long typeId) {
        DeteTypeInfoEntity typeInfo = deteTypeInfoService.getById(typeId);
        return typeInfo!=null;
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if(sysDic.size()!=0){
            return ;
        }
        List<SysDictEntity> sysDics = sysDictService.list();
        for (SysDictEntity sysDicEntity:sysDics){
            String type=sysDicEntity.getType();
            String code=sysDicEntity.getCode();
            Set<String> codes = sysDic.get(type);
            if(codes==null){
                codes=new HashSet<String>();
            }
            codes.add(code);
            sysDic.put(type,codes);
        }
    }

}
