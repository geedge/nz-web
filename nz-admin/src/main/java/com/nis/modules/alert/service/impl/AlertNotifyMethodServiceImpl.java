package com.nis.modules.alert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.dao.AlertNotifyMethodDao;
import com.nis.modules.alert.entity.AlertNotifyMethod;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class AlertNotifyMethodServiceImpl extends ServiceImpl<AlertNotifyMethodDao, AlertNotifyMethod> implements AlertNotifyMethodService {

    @Autowired
    private AlertNotifyMethodDao alertNotifyMethodDao;

    @Override
    public boolean isExist(String ids) {
        if(Tool.StrUtil.isEmpty(ids)){
            return false;
        }
        List<String> methodIds = Arrays.asList(Tool.StrUtil.split(ids,","));
        Collection<AlertNotifyMethod> methods = this.listByIds(methodIds);
        return methods != null && methods.size() == methodIds.size();
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage page = new Query(AlertNotifyMethod.class).getPage(params);

        List<AlertNotifyMethod> list = alertNotifyMethodDao.queryList(page,params);

        page.setRecords(list);

        return new PageUtils(page);
    }

    @Override
    public void validateNotifyMethod(AlertNotifyMethod alertNotifyMethod,Boolean isModify) {
        ValidateUtils validateUtils = ValidateUtils.is(alertNotifyMethod.getName()).notNull(RCode.ALERTNOTIFY_NAME_ISNULL)
                .and(alertNotifyMethod.getAccount()).notNull(RCode.ALERTNOTIFY_ACCOUNT_ISNULL)
                .and(alertNotifyMethod.getState()).notNull(RCode.ALERTNOTIFY_STATE_ISNULL)
                .and(alertNotifyMethod.getFilePath()).notNull(RCode.ALERTNOTIFY_FILEPATH_ISNULL);
        //校验文件是否存在
        if (Tool.StrUtil.isNotEmpty(alertNotifyMethod.getFilePath()) && !Tool.FileUtil.exist(alertNotifyMethod.getFilePath()))
            throw new NZException(RCode.ALERTNOTIFY_FILE_NOTEXISTS);
        //是否是修改
        if(isModify){
            validateUtils.and(alertNotifyMethod.getId()).notNull(RCode.ALERTNOTIFY_ID_ISNULL);
        }
        // name 不能重复
        List<AlertNotifyMethod> list = this.list(new LambdaQueryWrapper<AlertNotifyMethod>().eq(AlertNotifyMethod::getName, alertNotifyMethod.getName()));
        boolean flag = false;
        // 当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
        if ((list == null || list.size() == 0) || (list != null && list.size() == 1 && list.get(0).getId().equals(alertNotifyMethod.getId()))) {
            flag = true;
        }
        if (!flag) {
            throw new NZException(RCode.ALERTNOTIFY_NAME_DUPLICATE);
        }
    }
}
