package com.nis.setup.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.Tool;

@RestController
@RequestMapping("/ctl")
public class SetupCtlController {
	
	@GetMapping("/aes")
	public R ase(@RequestParam String content){
		Map<String,String> result =new HashMap<String,String>();
		String encrypt = Tool.AesUtil.encrypt(content, Constant.AES_SECRET_KEY);
		result.put("content", content);
		result.put("encrypt", encrypt);
		return R.ok(result);
	}
}
