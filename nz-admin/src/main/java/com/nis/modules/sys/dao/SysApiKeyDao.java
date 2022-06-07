package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysApiKeyDao extends BaseMapper<SysApiKey>{

	List<SysApiKey> queryList(IPage page,@Param("params") Map<String, Object> params);

	List<SysApiKey> querySelfApiKeys(IPage result, @Param("params") Map<String, Object> params);

	@Update("UPDATE sys_api_key SET role_id = #{roleId} where create_by = #{userId};")
    void updateRoleIdByCreateBy(@Param("userId") Integer userId, @Param("roleId") Integer roleId);
}
