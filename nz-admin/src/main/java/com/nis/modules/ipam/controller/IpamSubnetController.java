package com.nis.modules.ipam.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamSubnetService;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IncompatibleAddressException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ipam/subnet")
public class IpamSubnetController {

    @Autowired
    private IpamSubnetService ipamSubnetService;
    
    @GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.IPAM_SUBNET)
	public R ipamSubnetInfo(@PathVariable Integer id) {
    	IpamSubnet ipamSubnet = ipamSubnetService.queryInfo(id);
		return R.ok(ipamSubnet);
	}

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.IPAM_SUBNET)
	public R ipamSubnetList(@RequestParam Map<String, Object> params) {
		PageUtils page = ipamSubnetService.queryPage(params);
		return R.ok(page);
	}

	@RequestMapping(method = { RequestMethod.POST })
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.IPAM_SUBNET)
	public R ipamSubnetSave(@RequestBody IpamSubnet ipamSubnet) throws AddressStringException, IncompatibleAddressException {
		Integer id = ipamSubnetService.saveOrModifyIpamSubnet(ipamSubnet);
		return R.ok().putData("id", id);
	}

	@RequestMapping(method = { RequestMethod.PUT })
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.IPAM_SUBNET)
	public R ipamSubnetModify(@RequestBody IpamSubnet ipamSubnet) throws AddressStringException, IncompatibleAddressException {
		ValidateUtils.is(ipamSubnet.getId()).notNull(RCode.IPAM_SUBNET_ID_ISNULL);
		Integer id = ipamSubnetService.saveOrModifyIpamSubnet(ipamSubnet);
		return R.ok().putData("id", id);
	}

	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.IPAM_SUBNET)
	public R ipamSubnetDelete(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.IPAM_SUBNET_ID_ISNULL);

		return ipamSubnetService.delIpamSubnets(force, Arrays.asList(ids));
	}

	@GetMapping("/template")
	public void getTemplateByLanguage(HttpServletResponse response,
	                                  @RequestParam(required = false, defaultValue = "en") String language,
	                                  @RequestParam(required = false, defaultValue = "1") String format) throws IOException {

		// 校验 LANGUAGE
		if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
			throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
		}
		// 校验 FORMAT
		List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
		if (!supportImportingFileTypes.contains(format)) {
			throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
		}

		ipamSubnetService.getTemplateByLanguage(response,format);
	}

	@PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.IPAM_SUBNET)
	public R importIpamSubnetData(@RequestParam(value = "file") MultipartFile excelFile, HttpServletRequest request, @RequestParam Map<String, Object> params) throws IOException, AddressStringException, IncompatibleAddressException {
		String language = request.getHeader(Constant.LANGUAGE);
		ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
		// 校验 LANGUAGE
		if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
			throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
		}
		Map info = ipamSubnetService.importIpamSubnetData(excelFile, params);
		return R.ok(info);
	}

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.IPAM_SUBNET)
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
	    Object format = params.get("format");
	    String formatStr = format == null ? "1" : format.toString();
	    // 校验 FORMAT
	    List<String> supportImportingFileTypes = Arrays.asList(Constant.ImportFileType.values()).stream().map(Constant.ImportFileType::getValue).collect(Collectors.toList());
	    if (!supportImportingFileTypes.contains(formatStr)) {
		    throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
	    }
	    params.put("format", formatStr);

	    ipamSubnetService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.CANCEL, type = TypeEnum.IPAM_SUBNET)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);
        ipamSubnetService.cancelImport(seq);
        return R.ok();
    }
    
}
