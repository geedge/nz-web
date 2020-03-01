package com.nis.modules.terminal.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.terminal.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
@Mapper
@Repository
public interface AccountDao extends BaseMapper<Account> {
   
}