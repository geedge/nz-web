package com.nis.modules.endpoint.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nis.common.annotation.SysLog;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.endpoint.entity.Endpoint;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.project.entity.Project;
import com.nis.modules.romote.entity.Account;
import com.nis.modules.sys.controller.AbstractController;

@RestController
@RequestMapping("/endpoint")
public class EndpointController extends AbstractController {
	
	@Autowired
	private EndpointService endpointService;
	
	@GetMapping
	public R list(@RequestParam Map<String, Object> params){
		PageUtils page = endpointService.queryPage(params);
		return R.ok(page);
	}
	
	@PostMapping
	@SysLog("save endpoint")
	public R save(@RequestBody List<Endpoint> endpointEntitys) {
		for(Endpoint endpointEntity : endpointEntitys) {
			endpointService.validateParam(endpointEntity);
		}
		endpointService.saveOrUpdateBatch(endpointEntitys);
		return R.ok();
	}
	
	@PutMapping
	@SysLog("update endpoint")
	public R modify(@RequestBody List<Endpoint> endpointEntitys) {
		for(Endpoint endpointEntity : endpointEntitys) {
			ValidateUtils.is(endpointEntity.getId()).notNull(RCode.ENDPOINT_ID_ISNULL);
			endpointService.validateParam(endpointEntity);
		}
		endpointService.saveOrUpdateBatch(endpointEntitys);
		return R.ok();
	}
	
	@DeleteMapping
	@SysLog("delete endpoint")
	public R remove(String ids) {
		ValidateUtils.is(ids).notNull(RCode.ENDPOINT_ID_ISNULL);
		String[] split = ids.split(",");
		endpointService.removeByIds(Arrays.asList(split));
		return R.ok();
	}
	
	/**
	 * 批量导入endpoint数据 
	 * @param endpointExcelFile
	 * @return
	 */
	@PostMapping("/import")
	@SysLog("import endpoint")
	public R importEndpoint(MultipartFile endpointExcelFile) {
		ValidateUtils.is(endpointExcelFile).notNull(RCode.ENDPOINT_IMPORT_FILE_ISNULL);
        // 本次导入的 uuid
        String seqUUID = UUID.randomUUID().toString();
        Map map = endpointService.importEndpoint(endpointExcelFile, seqUUID);
        return R.ok(map);
	}
	
	/**
	 * 批量导出endpoint
	 * @param response
	 * @param params
	 */
	@GetMapping("/export")
	@SysLog("export endpoint")
    public void exportAssetsFromExcel(HttpServletResponse response, @RequestParam Map<String, Object> params) {
		PageUtils page = endpointService.queryPage(params);
        List<Endpoint> list = (List<Endpoint>) page.getList();

        Workbook workbook = new XSSFWorkbook();
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
            row1.createCell(3).setCellValue(module.getPort());
            // param
            row1.createCell(4).setCellValue(module.getParam());
            // path
            row1.createCell(5).setCellValue(module.getPath());
            // host
            row1.createCell(6).setCellValue(asset.getHost());
        }

        try {
            // 响应为流
            String fileName = "EndpointsExport.xlsx"; // 默认名称
            response.setContentType("application/excel");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	/**
	 * 撤销导入操作
	 * @param seq
	 * @return
	 */
	@DeleteMapping("/cancelImport")
	@SysLog("cancelImport endpoint")
    public R cancelImport(String seq) {
        ValidateUtils.is(seq).notNull(RCode.ENDPOINT_CANCELIMPORTSEQ_ISNULL);
        endpointService.cancelImport(seq);
        return R.ok();
    }
}

