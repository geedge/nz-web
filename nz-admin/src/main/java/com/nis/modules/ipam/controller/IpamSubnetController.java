package com.nis.modules.ipam.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mysql.cj.x.protobuf.MysqlxDatatypes.Array;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.panel.entity.VisualPanel;

import inet.ipaddr.AddressStringException;
import inet.ipaddr.IncompatibleAddressException;

@RestController
@RequestMapping("/ipam/subnet")
public class IpamSubnetController {

    @Autowired
    private IpamSubnetService ipamSubnetService;
    
    @GetMapping("/{id}")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.IPAMSUBNET)
	public R ipamSubnetInfo(@PathVariable Integer id) {
    	IpamSubnet ipamSubnet = ipamSubnetService.queryInfo(id);
		return R.ok(ipamSubnet);
	}

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.IPAMSUBNET)
	public R ipamSubnetList(@RequestParam Map<String, Object> params) {
		PageUtils page = ipamSubnetService.queryPage(params);
		return R.ok(page);
	}

	@RequestMapping(method = { RequestMethod.POST })
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.IPAMSUBNET)
	public R ipamSubnetSave(@RequestBody IpamSubnet ipamSubnet) throws AddressStringException, IncompatibleAddressException {
		Integer id = ipamSubnetService.saveOrModifyIpamSubnet(ipamSubnet);
		return R.ok().putData("id", id);
	}

	@RequestMapping(method = { RequestMethod.PUT })
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.IPAMSUBNET)
	public R ipamSubnetModify(@RequestBody IpamSubnet ipamSubnet) throws AddressStringException, IncompatibleAddressException {
		ValidateUtils.is(ipamSubnet.getId()).notNull(RCode.IPAM_SUBNET_ID_ISNULL);
		Integer id = ipamSubnetService.saveOrModifyIpamSubnet(ipamSubnet);
		return R.ok().putData("id", id);
	}

	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.IPAMSUBNET)
	public R ipamSubnetDelete(@RequestParam(required = false, defaultValue = "0") Integer force, Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.IPAM_SUBNET_ID_ISNULL);

		List<Map> delRelObjectMsgList = ipamSubnetService.delIpamSubnets(force, Arrays.asList(ids));
		switch (force) {
			case 0: {// 0：存在关联数据时，删除失败
				if (Tool.CollUtil.isNotEmpty(delRelObjectMsgList)) {
					return R.error(RCode.IPAM_SUBNET_RELOBJ_CAN_NOT_REMOVE).putData("list", delRelObjectMsgList);
				}
			}
			case 1: // 1：存在关联数据时，同时删除关联数据
			default:
				return R.ok().putData("list", delRelObjectMsgList);
		}
	}
	
	@GetMapping("/template")
	public void getTemplateByLanguage(HttpServletResponse response,HttpServletRequest request) throws IOException {
		String language = request.getHeader(Constant.LANGUAGE);
		String format = request.getHeader("format");
		format = Tool.StrUtil.emptyToDefault(format, "1");

		// 校验 LANGUAGE
		if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
			throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
		}
		// 校验 FORMAT
		if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(format)) {
			throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
		}

		ipamSubnetService.getTemplateByLanguage(response,format);
	}

	@PostMapping("/import")
    @SysLog(operation = OperationEnum.IMPORT, type = TypeEnum.CHART)
	public R importChartData(MultipartFile excelFile, HttpServletRequest request) throws IOException, AddressStringException, IncompatibleAddressException {
		String language = request.getHeader(Constant.LANGUAGE);
		ValidateUtils.is(excelFile).notNull(RCode.EXCELFILE_IMPORT_FILE_ISNULL);
		// 校验 LANGUAGE
		if (!Arrays.asList(Constant.LANGUAGE_VALUES).contains(language)) {
			throw new NZException(RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
		}
		Map info = ipamSubnetService.importIpamSubnetData(excelFile);
		return R.ok(info);
	}

    @GetMapping("/export")
    @SysLog(operation = OperationEnum.EXPORT, type = TypeEnum.CHART)
    public void export(HttpServletResponse response, @RequestParam Map<String, Object> params) throws IOException {
	    Object format = params.get("format");
	    String formatStr = format == null ? "1" : format.toString();
	    // 校验 FORMAT
	    if (!Arrays.asList(Constant.IMPORTFILE_TYPE_INTVALUES).contains(formatStr)) {
		    throw new NZException(RCode.EXCELFILE_TYPE_ERROR);
	    }
	    params.put("format", formatStr);

	    ipamSubnetService.export(response, params);
    }

    @DeleteMapping("/cancelImport")
    @SysLog(operation = OperationEnum.DELETE, type = TypeEnum.CHART)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.UNDO_IMPORT_SEQ_ISNULL);
        ipamSubnetService.cancelImport(seq);
        return R.ok();
    }
    
}
