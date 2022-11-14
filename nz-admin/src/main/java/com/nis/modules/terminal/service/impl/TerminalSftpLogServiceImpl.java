package com.nis.modules.terminal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.entity.AssetBrand;
import com.nis.modules.terminal.dao.TerminalSftpLogDao;
import com.nis.modules.terminal.entity.TerminalSftpLog;
import com.nis.modules.terminal.service.TerminalSftpLogService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TerminalSftpLogServiceImpl extends ServiceImpl<TerminalSftpLogDao, TerminalSftpLog> implements TerminalSftpLogService {


    @Override
    public PageUtils querySftpLogPage(Map<String, Object> params) {
        String uuid = Tool.StrUtil.nullToDefault((String) params.get("uuid"), "");
        String name = Tool.StrUtil.nullToDefault((String) params.get("name"), "");

        LambdaQueryWrapper<TerminalSftpLog> queryWrapper = new LambdaQueryWrapper<TerminalSftpLog>()
                .eq(ToolUtil.isNotEmpty(uuid), TerminalSftpLog::getUuid, uuid)
                .eq(ToolUtil.isNotEmpty(params.get("type")), TerminalSftpLog::getType, ToolUtil.isNotEmpty(params.get("type")) ? params.get("type").toString() : null)
                .like(ToolUtil.isNotEmpty(name), TerminalSftpLog::getName, Tool.StrUtil.likeEscape(name));

        IPage page = this.page(new Query(AssetBrand.class).getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    @Override
    public void saveSftpLog(String uuid, String path, Integer sftpOperType) {
        TerminalSftpLog sftpLog = new TerminalSftpLog(
                uuid,
                sftpOperType,
                path,
                System.currentTimeMillis());
        this.save(sftpLog);
    }
}
