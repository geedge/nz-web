package com.nis.modules.sys.controller;

import Aladdin.HaspStatus;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.sys.entity.LicenseEntity;
import com.nis.modules.sys.entity.Signature;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysComponentService;
import com.nis.modules.sys.service.SysConfigService;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sys/license")
@SuppressWarnings("all")
public class SysLicenseController extends AbstractController {
	private static final Log log = Log.get();

	@Autowired
	private LicenseService licenseService;
	@Autowired
	private SysComponentService sysComponentService;

	@Value("${vendorCode}")
	private String vendorCode;

	@Value("${licenseTimeout:30000}")
	private Integer licenseTimeout;

	@Value("${compileType:release}")
	private String compileType;
	
	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private SysConfigService sysConfigService;
	/**
	 * 获取license
	 */
	@GetMapping("/detail")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.LICENSE)
	public R getLicense() {
		Map<String, Object> license = licenseService.getLicenseDetail();
		return R.ok().putData("license", license);
	}

	@GetMapping("/token")
	public R getLicenseToken(@RequestParam(required = false) String scope, HttpServletResponse response)
			throws IOException, DocumentException {
		if (!Tool.StrUtil.equalsIgnoreCase("local", scope)) {
			List<SysComponent> comList = sysComponentService.list();
			List<CompletableFuture<Map<String, Object>>> futureList = Tool.ListUtil.list(false);
			for (int i = 0, j = comList.size(); i < j; i++) {
				SysComponent com = comList.get(i);
				String host = com.getProtocol() + "://" + com.getHost() + ":" + com.getPort();
				CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(() -> {
					String url = String.format("%s%s?%s", host, "/sys/license/token", "scope=local");
					ResponseEntity<Map> entity = restTemplate.getForEntity(url, Map.class);
					log.info("host: {},result: {}", host, Tool.JSONUtil.toJsonStr(entity));
					return entity.getBody();
				});
				futureList.add(future);
			}
			// 等待所有结果返回
			try {
				CompletableFuture.allOf((CompletableFuture<?>[]) futureList.toArray(new CompletableFuture[0]))
						.get(licenseTimeout, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.warn(e);
			}
			List<String> haspResult = new ArrayList<String>();
			futureList.forEach(future -> {
				Map<String, Object> result = future.getNow(null);
				if (Tool.MapUtil.isNotEmpty(result)) {
					Integer code = Tool.MapUtil.get(result, "code", Integer.class);
					String data = Tool.MapUtil.get(result, "data", String.class);
					if (Tool.NumberUtil.equals(code, 200)) {
						haspResult.add(data);
					}
				}
			});
			
			//排序组装hasps内容  计算摘要值
			List<String> sortHasp = haspResult.stream().sorted().collect(Collectors.toList());
			
			Document document = DocumentHelper.createDocument();
			Element licenseEle = document.addElement("license");
			Element hasps = licenseEle.addElement("hasps");
			
			for(String hasp : haspResult) {
				Document haspDoc = DocumentHelper.parseText(hasp);
				Element haspInfo = haspDoc.getRootElement();
				hasps.add(haspInfo);
			}
			String haspsStr = hasps.asXML();
			
			licenseEle.remove(hasps);
			String licenseToken = sysConfigService.getValue("license_token");
			LicenseEntity licenseEntity = new LicenseEntity();
			try {
				String licenseStr = licenseService.getLicense(); // 已签发 license 信息
				if(ToolUtil.isNotEmpty(licenseStr)) {
					LicenseEntity oldLicense = (LicenseEntity) LicenseUtil.convertXmlStrToObject(LicenseEntity.class, licenseStr);
					licenseEntity.setComment(oldLicense.getComment());
					licenseEntity.setType(oldLicense.getType());
					licenseEntity.setOrganization(oldLicense.getOrganization());
				}
			} catch (Exception e) {
				log.warn(e);
			}
			licenseEntity.setSupportID(licenseToken);
			licenseEntity.setHasps(haspsStr);
			String digest = licenseEntity.genLicenseDigest();
			
			Signature signature = new Signature();
			signature.setDigestMethod("sha1");
			signature.setDigestValue(digest);
			licenseEntity.setSignature(signature);
			
			String licenseXml = LicenseUtil.convertObjectToXmlStr(licenseEntity);
			
			Document licDoc = DocumentHelper.parseText(licenseXml);
			Element lic = licDoc.getRootElement();
			Element licHasps = lic.element("hasps");
			if(ToolUtil.isNotEmpty(licHasps)) {
				lic.remove(licHasps);
			}
			lic.add(hasps);
			
			// 多个 c2v组织成一个 license 文件
			String license = licDoc.asXML();
			String licenseProductName = sysConfigService.getValue("license_product_name");
			Tool.ResponseUtil.downloadFile(response, String.format("%s-%s-license-apply.xml", licenseProductName, licenseToken),
					license.getBytes(Constant.DEFAULT_CHARTSET_NAME));
			return null;
		} else {
			String c2v = licenseService.getLicenseC2V();
			return R.ok().put("data", c2v);
		}
	}

	/**
	 * 获取license state
	 */

	@GetMapping("/status")
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.LICENSE)
	public R getLicenseState(@RequestParam(required = false) String scope) {
		if(Tool.StrUtil.equalsIgnoreCase(compileType, "debug")) {
			return R.ok().putData("status", 0);
		}
		if (Tool.StrUtil.equalsIgnoreCase("all", scope)) {
			List<SysComponent> comList = sysComponentService.list();
			List<CompletableFuture<SysComponent>> futureList = Tool.ListUtil.list(false);
			for (int i = 0, j = comList.size(); i < j; i++) {
				SysComponent com = comList.get(i);
				String host = com.getProtocol() + "://" + com.getHost() + ":" + com.getPort();
				CompletableFuture<SysComponent> future = CompletableFuture.supplyAsync(() -> {
					String url = String.format("%s%s?%s", host, "/sys/license/status", "scope=local");
					ResponseEntity<Map> entity = restTemplate.getForEntity(url, Map.class);
					log.info("license status: {}", Tool.JSONUtil.toJsonStr(entity));
					Map data = Tool.MapUtil.get(entity.getBody(), "data", Map.class);
					Integer status = Tool.MapUtil.get(data, "status", Integer.class);
					com.setStatus(status == null ? -1 : status);
					return com;
				});
				futureList.add(future);
			}
			// 等待所有结果返回
			try {
				CompletableFuture.allOf((CompletableFuture<?>[]) futureList.toArray(new CompletableFuture[0]))
						.get(licenseTimeout, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.warn(e);
			}
			return R.ok().putData("list", comList);
		}

		Integer status = licenseService.verify();
		if(status != 0) {
			throw new NZException(RCode.LICENSE_ERROR_CODE.setParam(status));
		}
		return R.ok().putData("status", status);
	}

	/**
	 * @throws IOException 上传license @throws
	 */
	@PostMapping("/upload")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.LICENSE)
	public R uploadLicense(@RequestParam(name = "file", required = false) MultipartFile file,
			@RequestParam(required = false) String scope) throws Exception {
		if (!Tool.StrUtil.equalsIgnoreCase("local", scope)) {
			// 保存 license 到 数据库
			ValidateUtils.is(file).notNull(RCode.LICENSE_FILE_IS_NULL);
			byte[] bytes = file.getBytes();
			
			// 保存前先校验摘要值
			licenseService.validateDigestValue(Tool.StrUtil.str(bytes, "utf-8"));
			// 更新证书
			licenseService.uploadLicense(Tool.StrUtil.str(bytes, "utf-8"));
			// 调用所有 component 接口 update
			Map<String, R> result = Tool.MapUtil.newHashMap();
			List<SysComponent> comList = sysComponentService.list();
			List<CompletableFuture<SysComponent>> futureList = Tool.ListUtil.list(false);
			for (int i = 0, j = comList.size(); i < j; i++) {
				SysComponent com = comList.get(i);
				String host = com.getProtocol() + "://" + com.getHost() + ":" + com.getPort();
				CompletableFuture<SysComponent> future = CompletableFuture.supplyAsync(() -> {
					String url = String.format("%s%s?%s", host, "/sys/license/upload", "scope=local");
					ResponseEntity<R> entity = restTemplate.postForEntity(url, null, R.class);
					log.info("license status: {}", Tool.JSONUtil.toJsonStr(entity));
					Map data = Tool.MapUtil.get(entity.getBody(), "data", Map.class);
					Integer status = Tool.MapUtil.get(data, "status", Integer.class);
					com.setStatus(status == null ? -1 : status);
					return com;
				});
				futureList.add(future);
			}
			// 等待所有结果返回
			try {
				CompletableFuture.allOf((CompletableFuture<?>[]) futureList.toArray(new CompletableFuture[0]))
						.get(licenseTimeout, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.warn(e);
			}
			return R.ok().putData("list", comList);
		} else {
			boolean installLicense = licenseService.installLicense();
			if (installLicense) {
				return R.ok().putData("status", HaspStatus.HASP_STATUS_OK);
			}
			return R.error(RCode.LICENSE_ERROR_CODE.setParam(999));
		}
	}

	/**
	 * 下载license @throws IOException @throws UnsupportedEncodingException @throws
	 */
	@GetMapping("/download")
	public void download(HttpServletResponse response) throws IOException {
		String license = licenseService.getLicense();
		if (license == null) {
			throw new NZException(RCode.LICENSE_FILE_IS_EMPTY);
		}
		String licenseProductName = sysConfigService.getValue("license_product_name");
		String licenseToken = sysConfigService.getValue("license_token");
		Tool.ResponseUtil.downloadFile(response, String.format("%s-%s-license-issue.xml", licenseProductName, licenseToken), license.getBytes(Constant.DEFAULT_CHARTSET_NAME));
	}

	
	@PostMapping("/gen")
	public void gen(@RequestParam(name = "files", required = true) MultipartFile[] files,@RequestParam Map<String,Object> params,HttpServletResponse response) throws IORuntimeException, UnsupportedEncodingException, IOException {
		ValidateUtils.is(params.get("supportID")).notNull(RCode.LICENSE_SUPPORT_ID_ISNULL)
		.and(params.get("organization")).notNull(RCode.LICENSE_ORGANIZATION_ISNULL)
		.and(params.get("type")).notNull(RCode.LICENSE_TYPE_ISNULL);
		if(ToolUtil.isEmpty(files)) {
			throw new NZException(RCode.LICENSE_FILE_IS_NULL);
		}
		
		String license = licenseService.gen(files,params);
		
		if(StrUtil.isEmpty(license)) {
			return;
		}else {
			Tool.ResponseUtil.downloadFile(response, String.format("%s-%s-license-issue.xml",  sysConfigService.getValue("license_product_name"), params.get("supportID")),
					license.getBytes(Constant.DEFAULT_CHARTSET_NAME));
		}
	}

	@PostMapping("/qrdecode")
	public void qrdecode(@RequestParam(name = "files", required = true) MultipartFile[] files,HttpServletResponse response) throws IOException {
		String licenseInfo = licenseService.qrdecode(files);
		// 解析xml
		Map<String, Object> xmlToMap = Tool.XmlUtil.xmlToMap(licenseInfo);
		String supportID = ToolUtil.toStr(xmlToMap.get("supportID"));
		ResponseUtil.downloadFile(response, String.format("NZ-%s-license-apply.xml", supportID), licenseInfo.getBytes());
	}
}
