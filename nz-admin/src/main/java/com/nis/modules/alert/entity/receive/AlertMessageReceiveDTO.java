package com.nis.modules.alert.entity.receive;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;

import com.google.gson.Gson;
import com.nis.modules.alert.entity.AlertMessageEntity;

import lombok.Data;
import org.springframework.util.DigestUtils;

/**
 * 用来接收告警信息的DTO类
 */
@Data
public class AlertMessageReceiveDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	private static Gson gson = new Gson();

	private Map labels;
	
	private Map annotations;
	
	private String startsAt;
	
	private String endsAt;
	
	private String generatorURL;
	
	public static List<AlertMessageEntity> toAlertMessageEntity(AlertMessageReceiveDTO[] entities) throws Exception {
		List<AlertMessageEntity> list = new ArrayList<>();
		for (AlertMessageReceiveDTO entity : entities) {
			list.add(toAlertMessageEntity(entity));
		}
		return list;
	}
	
	public static AlertMessageEntity toAlertMessageEntity(AlertMessageReceiveDTO dto) throws ParseException {
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
		entity.setEndAt(ea);
		entity.setState(1);
		return entity;
	}
}
