package com.nis.modules.sys.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysComponentService;

import Aladdin.HaspStatus;
import cn.hutool.log.Log;

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

	@Autowired
	private RestTemplate restTemplate;

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
			throws IOException {
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
			Document document = Tool.XmlUtil.parseXml("<license></license>");
			Element rootEle = document.getDocumentElement();
			futureList.forEach(future -> {
				Map<String, Object> result = future.getNow(null);
				if (Tool.MapUtil.isNotEmpty(result)) {
					Integer code = Tool.MapUtil.get(result, "code", Integer.class);
					String data = Tool.MapUtil.get(result, "data", String.class);
					if (Tool.NumberUtil.equals(code, 200)) {
						Document c2v = Tool.XmlUtil.parseXml(data);
						Node importNode = document.importNode(c2v.getDocumentElement(), true);
						rootEle.appendChild(importNode);
					}
				}
			});
			// 多个 c2v组织成一个 license 文件
			String license = Tool.XmlUtil.toStr(document);
			Tool.ResponseUtil.downloadFile(response, "license-request.txt",
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
		return R.ok().putData("status", status);
	}

	/**
	 * @throws IOException 上传license @throws
	 */
	@PostMapping("/upload")
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.LICENSE)
	public R uploadLicense(@RequestParam(name = "file", required = false) MultipartFile file,
			@RequestParam(required = false) String scope) throws IOException {
		if (!Tool.StrUtil.equalsIgnoreCase("local", scope)) {
			// 保存 license 到 数据库
			ValidateUtils.is(file).notNull(RCode.LICENSE_FILE_IS_NULL);
			byte[] bytes = file.getBytes();
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
		Tool.ResponseUtil.downloadFile(response, "license.txt", license.getBytes(Constant.DEFAULT_CHARTSET_NAME));
	}

}
