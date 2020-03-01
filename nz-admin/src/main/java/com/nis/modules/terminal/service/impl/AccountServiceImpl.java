package com.nis.modules.terminal.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.terminal.dao.AccountDao;
import com.nis.modules.terminal.entity.Account;
import com.nis.modules.terminal.service.AccountService;

@Service("accountService")
public class AccountServiceImpl extends ServiceImpl<AccountDao,Account> implements AccountService {
    

}
