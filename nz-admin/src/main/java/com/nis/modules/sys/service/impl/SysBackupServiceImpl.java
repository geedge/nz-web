package com.nis.modules.sys.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.nis.common.utils.Constant;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.dao.SysConfigDao;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysBackupService;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.log.Log;

@Service("sysBackupService")
public class SysBackupServiceImpl implements SysBackupService {

    private static Log log = Log.get();
    
    @Autowired
    private  SysConfigDao sysConfigDao;
    
	@Override
	public Object queryInfo() {
		Object result = null;
		SysConfigEntity config = sysConfigDao.queryByKey("backup_config");
		if(ToolUtil.isNotEmpty(config) && ToolUtil.isNotEmpty(config.getParamValue())) {
			String paramValue = config.getParamValue();
			result = JSON.parseObject(paramValue, Object.class);
		}
		return result;
	}

	@Override
	public void save(Map<String, Object> params) {
		SysConfigEntity config = sysConfigDao.queryByKey("backup_config");
		if(ToolUtil.isNotEmpty(config)) {
			sysConfigDao.updateValueByKey("backup_config", JSON.toJSONString(params));
		}else {
			config = new SysConfigEntity();
			config.setParamKey("backup_config");
			config.setParamValue(JSON.toJSONString(params));
			config.setStatus("1");
			sysConfigDao.insert(config);
		}
	}

	@Override
	public List<Object> queryBackupList() {
		List<Object> result = null;
		SysConfigEntity config = sysConfigDao.queryByKey("backup_file_path");
		String path = Constant.BACKUP_ROOT_PATH;
		if(ToolUtil.isNotEmpty(config) && ToolUtil.isNotEmpty(config.getParamValue())) {
			path = config.getParamValue();
		}
		boolean directory = FileUtil.isDirectory(path);
		if(directory) {
			List<File> loopFiles = FileUtil.loopFiles(path);
			if(ToolUtil.isNotEmpty(loopFiles)) {
				List<File> sortFile = loopFiles.stream().sorted(Comparator.comparing(File::lastModified).reversed()).collect(Collectors.toList());
				result = new ArrayList<Object>();
				for(File file : sortFile) {
					Map<String,Object> data =new HashMap<String,Object>();
					data.put("fileName", file.getName());
					data.put("time", DateUtil.date(file.lastModified()));
					data.put("size", file.length());
					result.add(data);
				}
			}
		}
		return result;
	}

    
    
}
