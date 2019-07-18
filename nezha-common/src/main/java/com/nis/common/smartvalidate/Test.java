package com.nis.common.smartvalidate;

import java.util.HashMap;
import java.util.Map;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ann.NotNull;
import com.nis.common.utils.RCode;

public class Test {
	public static void main(String[] args) {
		Test t = new Test();
		t.setTt("");
		try {
			ValidateUtils check = ValidateUtils.check(t);
		} catch (NZException e) {
			System.out.println(e.getCode());
			System.out.println(e.getMsg());
		}
		
		
		try {
			ValidateUtils check = ValidateUtils.check("");
		} catch (NZException e) {
			System.out.println(e.getCode());
			System.out.println(e.getMsg());
		}
	}
	
	@NotNull(code = RCode.NOT_NULL_ERROR)
	private String tt;

	public String getTt() {
		return tt;
	}

	public void setTt(String tt) {
		this.tt = tt;
	}
	
}
