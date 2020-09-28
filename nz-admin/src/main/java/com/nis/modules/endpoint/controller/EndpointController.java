package com.nis.modules.endpoint.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.sys.controller.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/endpoint")
public class EndpointController extends AbstractController {
	
	@Autowired
	private EndpointService endpointService;

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY,type = TypeEnum.ENDPOINT)
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = endpointService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog(operation = OperationEnum.ADD,type = TypeEnum.ENDPOINT)
	public R save(@RequestBody List<Endpoint> endpointEntitys) {
		for(Endpoint endpointEntity : endpointEntitys) {
			endpointService.validateParam(endpointEntity);
		}
		endpointService.saveOrUpdateBatch(endpointEntitys);
		return R.ok();
	}
	
	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE,type = TypeEnum.ENDPOINT)
	public R modify(@RequestBody List<Endpoint> endpointEntitys) {
		for(Endpoint endpointEntity : endpointEntitys) {
			ValidateUtils.is(endpointEntity.getId()).notNull(RCode.ENDPOINT_ID_ISNULL);
			endpointService.validateParam(endpointEntity);
		}
		endpointService.saveOrUpdateBatch(endpointEntitys);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE,type = TypeEnum.ENDPOINT)
	public R remove(@RequestParam Integer... ids) {
		ValidateUtils.is(ids).notNull(RCode.ENDPOINT_ID_ISNULL);

        endpointService.deleteEndpointByIds(Arrays.asList(ids));
		return R.ok();
	}
	

	@PostMapping("/import")
	@SysLog(operation = OperationEnum.IMPORT,type = TypeEnum.ENDPOINT)
	public R importEndpoint(MultipartFile excelFile) {
		ValidateUtils.is(excelFile).notNull(RCode.ENDPOINT_IMPORT_FILE_ISNULL);

        Map map = endpointService.importEndpoint(excelFile);
        return R.ok(map);
	}
	
	/**
	 * 批量导出endpoint
	 * @param response
	 * @param params
	 */
	@GetMapping("/export")
	@SysLog(operation = OperationEnum.EXPORT,type = TypeEnum.ENDPOINT)
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
		endpointService.export(response,params);
	}
	/**
	 * 撤销导入操作
	 * @param seq
	 * @return
	 */
	@DeleteMapping("/cancelImport")
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.ENDPOINT)
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ENDPOINT_CANCELIMPORTSEQ_ISNULL);
        endpointService.cancelImport(seq);
        return R.ok();
    }

	@GetMapping("/template")
	public void getExportTemplateByLanguage(HttpServletResponse response, String language) {
		if (ToolUtil.isEmpty(language)) throw new NZException(RCode.ENDPOINT_EXPORT_HEADER_LANGUAGE_ISNULL);

		endpointService.getExportTemplateByLanguage(response, language);
	}
}

