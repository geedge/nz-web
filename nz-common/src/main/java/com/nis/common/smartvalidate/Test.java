package com.nis.common.smartvalidate;

import java.util.HashMap;
import java.util.Map;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ann.NotNull;
import com.nis.common.utils.RCode;

public class Test {
	public static void main(String[] args) {
		try {
			ValidateUtils check = ValidateUtils.check("");
		} catch (NZException e) {
			System.out.println(e.getCode());
			System.out.println(e.getMsg());
		}
	}
	
	private String tt;

	public String getTt() {
		return tt;
	}

	public void setTt(String tt) {
		this.tt = tt;
	}
	
}
