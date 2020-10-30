package com.nis.modules.alert.entity.receive;

import com.google.gson.Gson;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import lombok.Data;
import org.springframework.util.DigestUtils;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 用来接收告警信息的DTO类
 */
@Data
public class AlertMessageReceiveDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private static Gson gson = new Gson();

    private Map labels;

    private Map labelIds;

    private Map annotations;

    private String startsAt;

    private String endsAt;

    private String generatorURL;

    private Boolean snmptrap;

    /*public static List<AlertMessageEntity> toAlertMessageEntity(AlertMessageReceiveDTO[] entities) throws Exception {
        List<AlertMessageEntity> list = new ArrayList<>();
        for (AlertMessageReceiveDTO entity : entities) {
            list.add(toAlertMessageEntity(entity));
        }
        return list;
    }*/

    public static AlertMessageEntity toAlertMessageEntity(AlertMessageReceiveDTO dto, Integer buildRuleId) throws ParseException {
        AlertMessageEntity entity = new AlertMessageEntity();
        //取出labels，计算hash，查询redis看是否有重复
        Map receiveLabels = dto.getLabels();
        String md5DigestAsHex = DigestUtils.md5DigestAsHex(gson.toJson(receiveLabels).getBytes());
        entity.setHashKey(md5DigestAsHex);
        entity.setLabels(gson.toJson(receiveLabels));

        Map receiveAnnotations = dto.getAnnotations();
        entity.setDescription((String) receiveAnnotations.get("description"));
        entity.setSummary((String) receiveAnnotations.get("summary"));

        // 时区转换 按照数据库配置时区入库
        String startsAt = dto.getStartsAt();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date sa = sdf.parse(startsAt);

        // snmptrap 不需要做时间处理
        String alertname = (String) receiveLabels.get("alertname");
        if (!String.valueOf(buildRuleId).equals(alertname)) {
            boolean b = startsAt.endsWith("Z");
            if (!b) {
                // 其他时区 格式统一 为  2020-10-15T23:32:30.841162867（+12:00/+08:00/-05:00）
                int i = startsAt.lastIndexOf(":");
                String timezoneStr = startsAt.substring(i - 3, i);
                int timezone = Integer.parseInt(timezoneStr);
                sa = getUTCDateByTimeZone(sa, timezone);
            }
        }
        entity.setStartAt(sa);

        entity.setState(1);

        Map labelIds = dto.labelIds;

        if(ToolUtil.isNotEmpty(labelIds.get("endpoint"))){
            entity.setEndpointId(Integer.parseInt( labelIds.get("endpoint").toString()));
        }
        if(ToolUtil.isNotEmpty(labelIds.get("project"))){
            entity.setProjectId(Integer.parseInt(labelIds.get("project").toString()));
        }
        if(ToolUtil.isNotEmpty(labelIds.get("assetId"))){
            entity.setAssetId(Integer.parseInt(labelIds.get("assetId").toString()));
        }
        if(ToolUtil.isNotEmpty(labelIds.get("module"))){
            entity.setModuleId(Integer.parseInt(labelIds.get("module").toString()));
        }
        if(ToolUtil.isNotEmpty(labelIds.get("datacenter"))){
            entity.setDcId(Integer.parseInt( labelIds.get("datacenter").toString()));
        }

        entity.setRuleId(Integer.parseInt((String) receiveLabels.get("alertname")));
        entity.setSeverity((String) receiveLabels.get("severity"));
        return entity;
    }

    public static Date getUTCDateByTimeZone(Date sa,int timezone) {
        int tempTime = timezone * 3600000;
        long time = sa.getTime();

        if (timezone > 0) {
            time -= (tempTime > 0 ? tempTime : (tempTime *= -1));
        } else {
            time += tempTime > 0 ? tempTime : (tempTime *= -1);
        }
        sa = new Date(time);
        return sa;
    }
}
