package com.nis.modules.romote.service;

import com.nis.common.utils.PageUtils;
import com.nis.modules.romote.entity.Account;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface AccountService {

    Account getById(Integer id);

    Account.AuthStatus auth(Account account);

    boolean exists(String userName, String host) throws Exception;

    PageUtils queryPage(Map<String, Object> params);


    void validateAccount(Account account, MultipartFile file);

    void saveOrUpdateAccount(Account account, MultipartFile file);

    void delete(String ids);
}
