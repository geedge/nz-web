package com.nis.common.update.v3_2_to_3_3;

import com.nis.common.update.VersionUpdateInterface;
import com.nis.common.update.v3_2_to_3_3.service.V3_3_DataFormatConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class V3_3Update implements VersionUpdateInterface {

	@Autowired
	private V3_3_DataFormatConversionService service;
	
	@Override
	public boolean preUpdate() {
		
		return VersionUpdateInterface.super.preUpdate();
	}
	
	@Override
	public void excute() {
		service.assetModelDataFormatConversion();
	}

}
