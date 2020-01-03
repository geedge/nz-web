package com.nis.modules.alert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.entity.receive.AlertMessageReceiveDTO;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * 
 *
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
public interface AlertMessageService extends IService<AlertMessageEntity> {

    PageUtils queryPage(Map<String, Object> params) throws Exception;

	boolean updateByIds(AlertMessageEntity alertMessage);

	void saveBatch(AlertMessageReceiveDTO[] receive) throws Exception;
	
	void removeByTypeIds(Integer typeId,List ids);
}

