package com.nis.modules.endpoint.controller;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.entity.Project;
import com.nis.modules.sys.controller.AbstractController;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/endpoint")
public class EndpointController extends AbstractController {
	
	@Autowired
	private EndpointService endpointService;

	@Autowired
	private RedisTemplate redisTemplate;

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
	public R remove(String ids) {
		ValidateUtils.is(ids).notNull(RCode.ENDPOINT_ID_ISNULL);
		String[] split = ids.split(",");
		endpointService.removeByIds(Arrays.asList(split));

        try {
            // 同时删除 redis  endpoint_prom 中 endpointId 对应的内容
            redisTemplate.opsForHash().delete(Constant.REDIS_KEY_ENDPOINT_PROM, split);
        } catch (Exception e) {
            logger.debug("endpoint删除时，同步删除redis信息失败，key:endpoint_prom，失败参数：{}", split);
        }
		return R.ok();
	}
	
	/**
	 * 批量导入endpoint数据 
	 * @param endpointExcelFile
	 * @return
	 */
	@PostMapping("/import")
	@SysLog(operation = OperationEnum.IMPORT,type = TypeEnum.ENDPOINT)
	public R importEndpoint(MultipartFile excelFile) {
		ValidateUtils.is(excelFile).notNull(RCode.ENDPOINT_IMPORT_FILE_ISNULL);
        // 本次导入的 uuid
        String seqUUID = UUID.randomUUID().toString();
        Map map = endpointService.importEndpoint(excelFile, seqUUID);
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
		PageUtils page = endpointService.queryPage(params);
        List<Endpoint> list = (List<Endpoint>) page.getList();
        
        Workbook workbook = null;
        try {
	        workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Endpoints");
	
	        int rowNum = 0;
	        // 添加标题行
	        String[] columnNames = {"Project", "Module", "Sn", "Port", "Param", "Path", "Host"};
	        Row row = sheet.createRow(rowNum);
	        Cell cell;
	
	        // 创建样式对象
	        CellStyle style = workbook.createCellStyle();
	        style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);// 垂直
	        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);// 水平
	
	        for (int i = 0; i < columnNames.length; i++) {
	            // 创建列、单元格
	            cell = row.createCell(i);
	            cell.setCellValue(columnNames[i]);
	            cell.setCellStyle(style);
	        }
	
	        // 添加数据
	        Row row1;        
	        for (Endpoint endpoint : list) {
	            row1 = sheet.createRow(++rowNum);
	
	            Project project = endpoint.getProject();
	            Module module = endpoint.getModule();
	            Asset asset = endpoint.getAsset();
	            
	            // project name 
	            row1.createCell(0).setCellValue(project.getName());
	            // module name 
	            row1.createCell(1).setCellValue(module.getName());
	            // sn
	            row1.createCell(2).setCellValue(asset.getSn());
	            // port
	            row1.createCell(3).setCellValue(endpoint.getPort());
	            // param
	            row1.createCell(4).setCellValue(endpoint.getParam());
	            // path
	            row1.createCell(5).setCellValue(endpoint.getPath());
	            // host
	            row1.createCell(6).setCellValue(asset.getHost());
	        }

        
            // 响应为流
            String fileName = "EndpointsExport.xlsx"; // 默认名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        	if(workbook!=null) {
        		try {
					workbook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
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
}

