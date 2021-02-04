package com.nis.modules.terminal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.modules.terminal.dao.TerminalCmdDao;
import com.nis.modules.terminal.entity.TerminalCmd;
import com.nis.modules.terminal.service.TerminalCmdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("terminalCmdService")
public class TerminalCmdServiceImpl extends ServiceImpl<TerminalCmdDao, TerminalCmd> implements TerminalCmdService {

    @Override
    public Map queryCmdInfos(Map<String, Object> params) {
        // uuid 不能为空
        ValidateUtils.is(params.get("uuid")).notNull(RCode.TERMINAL_UUID_ISNULL);

        Integer time = (params.get("time") != null && StringUtils.isNotEmpty(params.get("time").toString())) ? Integer.valueOf(params.get("time").toString()) : 0;
        Integer size = (params.get("size") != null && StringUtils.isNotEmpty(params.get("size").toString())) ? Integer.valueOf(params.get("size").toString()) : 200;

        if (size < 1) {
            throw new NZException(RCode.TERMINAL_QUERYSIZE_INVALIDED);
        }


        String cmd = "";
        if (params.get("cmd") != null && StringUtils.isNotEmpty(params.get("cmd").toString())) {
            cmd = params.get("cmd").toString();
        }


        List<TerminalCmd> list = this.list(new LambdaQueryWrapper<TerminalCmd>()
                .eq(TerminalCmd::getUuid, params.get("uuid"))
                .like(TerminalCmd::getCmd, cmd)
                .gt(TerminalCmd::getTime, time)
                .orderByAsc(TerminalCmd::getId));

        Map map = new HashMap(8);
        map.put("total", list.size());

        if (CollectionUtils.isNotEmpty(list)) {

            int hashNext = 0;
            if (list.size() > size) {
                list = list.subList(0, size);
                hashNext = 1;
            }

            map.put("list", list);
            map.put("hasNext", hashNext);
            map.put("time", list.get(list.size() - 1).getTime());
            return map;
        }

        map.put("list", list);
        map.put("hasNext", 0);
        map.put("time", null);
        return map;
    }
}