package com.nis.modules.terminal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.terminal.entity.Account;

import java.util.Map;

public interface AccountService extends IService<Account> {

    PageUtils queryPage(Map<String, Object> params);

    void validateAccount(Account account);

    void saveOrUpdateAccount(Account account);

    void delete(Integer... ids);
}