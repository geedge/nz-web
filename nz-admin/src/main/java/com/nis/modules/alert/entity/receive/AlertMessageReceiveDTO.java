package com.nis.modules.alert.entity.receive;

import com.google.gson.Gson;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import lombok.Data;
import org.springframework.util.DigestUtils;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    public static List<AlertMessageEntity> toAlertMessageEntity(AlertMessageReceiveDTO[] entities) throws Exception {
        List<AlertMessageEntity> list = new ArrayList<>();
        for (AlertMessageReceiveDTO entity : entities) {
            // list.add(toAlertMessageEntity(entity));
            list.add(toAlertMessageEntity(entity));
        }
        return list;
    }

    /*public static AlertMessageEntity toAlertMessageEntity(AlertMessageReceiveDTO dto) throws ParseException {
        AlertMessageEntity entity = new AlertMessageEntity();
        //取出labels，计算hash，查询redis看是否有重复
        Map receiveLabels = dto.getLabels();
        String md5DigestAsHex = DigestUtils.md5DigestAsHex(gson.toJson(receiveLabels).getBytes());
        entity.setHashKey(md5DigestAsHex);
        entity.setLabels(gson.toJson(receiveLabels));
        entity.setRuleId(Integer.parseInt((String)receiveLabels.get("alertname")));
        entity.setSeverity((String)receiveLabels.get("severity"));
        entity.setHost(((String)receiveLabels.get("instance")).split(":")[0]);
        entity.setType(Integer.parseInt((String)receiveLabels.get("type")));
        entity.setLinkId(Integer.parseInt((String)receiveLabels.get("linkId")));

        Map receiveAnnotations = dto.getAnnotations();
        entity.setDescription((String)receiveAnnotations.get("description"));
        entity.setSummary((String)receiveAnnotations.get("summary"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date sa = sdf.parse(dto.getStartsAt());
        Date ea = sdf.parse(dto.getEndsAt());
        entity.setStartAt(sa);
        // 新接收的告警信息 state 默认为1 ， 过期会更改为2 ， 此处不应设置 end_at
        // entity.setEndAt(ea);
        entity.setState(1);
        return entity;
    }*/

    public static AlertMessageEntity toAlertMessageEntity(AlertMessageReceiveDTO dto) throws ParseException {
        AlertMessageEntity entity = new AlertMessageEntity();
        //取出labels，计算hash，查询redis看是否有重复
        Map receiveLabels = dto.getLabels();
        String md5DigestAsHex = DigestUtils.md5DigestAsHex(gson.toJson(receiveLabels).getBytes());
        entity.setHashKey(md5DigestAsHex);
        entity.setLabels(gson.toJson(receiveLabels));

        Map receiveAnnotations = dto.getAnnotations();
        entity.setDescription((String) receiveAnnotations.get("description"));
        entity.setSummary((String) receiveAnnotations.get("summary"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date sa = sdf.parse(dto.getStartsAt());
        // Date ea = sdf.parse(dto.getEndsAt());
        entity.setStartAt(sa);
        // 新接收的告警信息 state 默认为1 ， 过期会更改为2 ， 此处不应设置 end_at
        // entity.setEndAt(ea);
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
        // snmptrap 处理
        if (dto.getSnmptrap() != null && dto.getSnmptrap().equals(true)) {

            // 内置
            entity.setRuleId(Integer.parseInt((String) receiveLabels.get("alertname")));
            entity.setSeverity((String) receiveLabels.get("severity"));
            //entity.setHost(receiveLabels.get("asset").toString().trim());
            //entity.setType(Constant.AlertType.ALERT_TYPE_ASSET.getCode());
            //entity.setLinkId(assetIpWithId.get(receiveLabels.get("asset").toString().trim()));
        } else {

            // 非内置
            entity.setRuleId(Integer.parseInt((String) receiveLabels.get("alertname")));
            entity.setSeverity((String) receiveLabels.get("severity"));
            //entity.setHost(((String) receiveLabels.get("asset")));
            //entity.setType(Integer.parseInt((String) receiveLabels.get("type")));
            //entity.setLinkId(Integer.parseInt((String) receiveLabels.get("linkId")));
        }
        return entity;
    }
}
