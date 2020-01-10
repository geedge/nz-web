package com.nis.modules.romote.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.romote.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
@Mapper
@Repository
public interface AccountDao extends BaseMapper<Account> {
    public int getCount(@Param(value="user") String user,@Param(value="host") String host);
    public Account getAccountById(@Param(value="id") Integer id);

    void updateForPrivateKey(@Param(value="account") Account account);
}
