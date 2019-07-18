package com.nis;

import java.util.HashMap;
import java.util.Map;

import com.fetech.smart.validate.ValidateUtils;
import com.fetech.smart.validate.exception.ParamsException;
import com.nis.common.exception.NZException;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.entity.SystemInfoEntity;

public class SmartValidTest {
	public static void main(String[] args) {
		//校验对象
		SystemInfoEntity s = new SystemInfoEntity();
		s.setName("1");
		try {
			ValidateUtils.check(s);
			System.out.println("success");
		} catch (ParamsException e) {
			System.out.println(R.error(e.getMessage()));
		}
		
		//单独校验
		try {
			ValidateUtils.check(s.getId()).notNull(RCode.ERROR);
			System.out.println("success");
		} catch (ParamsException e) {
			System.out.println(R.error(e.getMessage()));
		}
		//单独校验
		Map<String, Object> params = new HashMap<>();
		params.put("b", 121);
		params.put("a", "");
		try {
			ValidateUtils.check(params.get("b")).max(99, RCode.ERROR);
		} catch (ParamsException e) {
			System.out.println(R.error(e.getMessage()));
		}
	}
}
