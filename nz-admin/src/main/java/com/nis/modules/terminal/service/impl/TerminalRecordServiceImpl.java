package com.nis.modules.terminal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.modules.terminal.dao.TerminalRecordDao;
import com.nis.modules.terminal.entity.TerminalRecord;
import com.nis.modules.terminal.service.TerminalRecordService;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service("terminalRecordService")
public class TerminalRecordServiceImpl extends ServiceImpl<TerminalRecordDao, TerminalRecord> implements TerminalRecordService {

    @Override
    public Map queryTerminalRecordInfos(Map<String, Object> params) {
        // uuid 不能为空
        ValidateUtils.is(params.get("uuid")).notNull(RCode.TERMINAL_UUID_ISNULL);

        Integer time = (params.get("time") != null && StringUtils.isNotEmpty(params.get("time").toString())) ? Integer.valueOf(params.get("time").toString()) : 0;
        Integer size = (params.get("size") != null && StringUtils.isNotEmpty(params.get("size").toString())) ? Integer.valueOf(params.get("size").toString()) : 1;

        if (size < 1) {
            throw new NZException(RCode.TERMINAL_QUERYSIZE_INVALIDED);
        }

        List<TerminalRecord> list = this.list(new LambdaQueryWrapper<TerminalRecord>()
                .eq(TerminalRecord::getUuid, params.get("uuid"))
                .ge(TerminalRecord::getStartTime, time)
                .orderByAsc(TerminalRecord::getId));

        Map map = new HashMap(8);
        map.put("uuid", params.get("uuid"));

        if (CollectionUtils.isNotEmpty(list)) {
            int hashNext = 0;

            // size 为 1 表示不限制条数
            if (!size.equals(1) && list.size() > size) {
                list = list.subList(0, size);
                hashNext = 1;
            }

            TerminalRecord terminalRecord = list.get(list.size() - 1);
            map.put("endTime", terminalRecord.getEndTime());
            map.put("hasNext", hashNext);
            List<Map<String, Object>> resuleList = new ArrayList<>(list.size());

            list.stream().forEach(record -> {
                Map<String, Object> tempMap = new HashMap<>(4);
                tempMap.put("t", record.getStartTime());
                tempMap.put("c", new String(record.getContent(), CharsetUtil.UTF_8));
                resuleList.add(tempMap);
            });
            map.put("list", resuleList);
        } else {
            map.put("endTime", null);
            map.put("hasNext", 0);
            map.put("list", Collections.emptyList());
        }
        map.put("startTime", time);
        return map;
    }
}