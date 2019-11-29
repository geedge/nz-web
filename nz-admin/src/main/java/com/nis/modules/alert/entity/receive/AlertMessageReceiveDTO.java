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
	
	public static AlertMessageEntity toAlertMessageEntity(AlertMessageReceiveDTO dto) throws Exception {
		AlertMessageEntity entity = new AlertMessageEntity();
		
		Map receiveLabels = dto.getLabels();
		entity.setLabels(gson.toJson(receiveLabels));
		entity.setAlertName(receiveLabels.get("alertname").toString());
		entity.setSeverity(receiveLabels.get("severity").toString());
		entity.setHost(receiveLabels.get("instance").toString().split(":")[0]);
		
		Map receiveAnnotations = dto.getAnnotations();
		entity.setDescription(receiveAnnotations.get("description").toString());
		entity.setSummary(receiveAnnotations.get("summary").toString());
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date sa = sdf.parse(dto.getStartsAt());
		Date ea = sdf.parse(dto.getEndsAt());
		entity.setStartAt(sa);
		entity.setEndAt(ea);
		entity.setState(1);
		
		return entity;
	}
}
