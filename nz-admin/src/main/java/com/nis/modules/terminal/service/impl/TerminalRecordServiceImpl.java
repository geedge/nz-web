package com.nis.modules.terminal.service.impl;

import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.modules.terminal.dao.TerminalRecordDao;
import com.nis.modules.terminal.entity.TerminalRecord;
import com.nis.modules.terminal.service.TerminalRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                .orderByAsc(TerminalRecord::getStartTime));

        Map map = new HashMap(8);
        map.put("uuid", params.get("uuid"));

        if (CollectionUtils.isNotEmpty(list)) {
            int hashNext = 0;

            StringBuilder builder = new StringBuilder();
            builder.append("[");
            for (TerminalRecord record : list) {
                builder.append(new String(record.getContent(), CharsetUtil.charset("UTF-8")));
            }
            String str = builder.substring(0, builder.length() - 1);
            List<Map> maps = JSONArray.parseArray(str += "]", Map.class);
            maps = maps.stream().filter(tempEntity -> {
                Integer t = (Integer) tempEntity.get("t");
                if (t > time) {
                    return true;
                }
                return false;
            }).collect(Collectors.toList());


            // size 为 1 表示不限制条数
            if (!size.equals(1) && maps.size() > size) {
                maps = maps.subList(0, size);
                hashNext = 1;
            }

            Map endEntity = maps.get(maps.size() - 1);
            map.put("endTime", endEntity.get("t"));
            map.put("hasNext", hashNext);

            map.put("list", maps);
        } else {
            map.put("endTime", null);
            map.put("hasNext", 0);
            map.put("list", Collections.emptyList());
        }
        map.put("startTime", time);
        return map;
    }
}