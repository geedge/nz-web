package com.nis.modules.agent.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TemplateUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@RestController
@RequestMapping("/agent")
public class AgentController {
	private Log log = Log.get();

	@Value("${nezha.installPath:install/}")
	private String installPath;

	@Autowired
	private PromserverService promserverService;

	@Autowired
	private SysConfigService sysConfigService;

	/**
	 * @Description 下载 install.sh脚本
	 * @Author rui
	 * @Date 2021/4/2
	 */
	@GetMapping("/{dcId}/{type}/install.sh")
	public String pushInstallScript(HttpServletRequest request, @PathVariable("dcId") Integer dcId,
			@PathVariable("type") Integer type) {
		ValidateUtils.is(dcId).notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(type)
				.notNull(RCode.AGENT_INSTALL_PARAM_MISS);
		// 读取sh脚本内容
		File shFile = CommonUtils.getResourceFile(installPath + "install.sh.template");
		if (shFile != null && shFile.exists()) {
			// 组织变量内容
			String scriptContent = Tool.FileUtil.readUtf8String(shFile);
			String localhost = request.getLocalAddr(); // web component ip
			Integer localPort = request.getLocalPort();
			String remoteHost = request.getRemoteHost(); // agent component ip
			String webToken = request.getHeader(Constant.AUTH_TOKEN_CODE);
			Map<String, String> map = new HashMap<>();
			map.put("remoteIP", localhost); // to install.sh is remote
			map.put("remotePort", String.valueOf(localPort));
			map.put("localhost", remoteHost); // to install.sh is local
			map.put("webToken", webToken);
			map.put("datacenterID", String.valueOf(dcId));
			map.put("type", String.valueOf(type));
			try {
				// 根据模板文件生成 sh文件
				Template template = TemplateUtil.stringToTemplate(scriptContent, "installTemplate");
				scriptContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
			} catch (IOException | TemplateException e) {
				log.error("install template parse error", e);
				return "echo 'Internal server error'";
			}
			return scriptContent;
		} else {
			return "load script failed !";
		}
	}

	/**
	 * @Description 下载confagent 的安装包
	 * @Author rui
	 * @Date 2021/4/2
	 */
	@GetMapping("/download")
	public void downloadAgent(HttpServletResponse response,
			@RequestParam(defaultValue = "centos", required = false) String os) {
		log.info("download agent package to {}", os);
		String json = sysConfigService.getValue("confagent_name");
		JSONObject confagentNames = Tool.JSONUtil.parseObj(json);
		// 配置的正则
		String confagentName = (String) confagentNames.get(os);
		Pattern confagentPattern = Pattern.compile(confagentName);
		try {
			File installPathFile = CommonUtils.getResourceFile(installPath);
			log.info("installPath exist: {},isDir:{}", installPathFile == null ? false : installPathFile.exists(),
					installPathFile == null ? false : installPathFile.isDirectory());
			if (installPathFile == null || !installPathFile.isDirectory()) {
				throw new NZException("file download error", 500);
			}

			File[] listFiles = installPathFile.listFiles((file, name) -> {
				return confagentPattern.matcher(name).find();
			});

			if (Tool.ArrayUtil.isEmpty(listFiles)) {
				throw new NZException("file download error", 500);
			}
			File file = CommonUtils.getResourceFile(listFiles[0].getAbsolutePath());
			Tool.ResponseUtil.downloadFile(response, file);
		} catch (IORuntimeException | IOException e) {
			log.error("file download error", e);
			throw new NZException("file download error", 500);
		}
	}

	/**
	 * @Description 将confagent自动注册 为promserver
	 * @Author rui
	 * @Date 2021/4/2
	 */
	@PostMapping("/register")
	@RequiresPermissions({ "promserver:save" })
	public R autoRegister(HttpServletRequest request, @RequestBody Promserver promserver) {
		if (Tool.StrUtil.isEmpty(promserver.getHost())) {
			promserver.setHost(request.getRemoteAddr());
		}
		// 保存 agent
		Integer id = promserverService.saveOrUpdatePromserver(promserver, false);
		return R.ok().putData("id", id);
	}

	/**
	 * @Description 访问confagent refresh接口，刷新confagent token
	 * @Author rui
	 * @Date 2021/4/2
	 */
	@PostMapping("/token/refresh")
	public R refreshToken(HttpServletResponse response, @RequestBody Map<String, Object> params) {
		String protocol = (String) params.get("protocol");
		String host = (String) params.get("host");
		String token = (String) params.get("token");
		Integer port = (Integer) params.get("port");
		// 校验参数
		ValidateUtils.is(host).notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(token)
				.notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(port).notNull(RCode.AGENT_INSTALL_PARAM_MISS);
		protocol = Tool.StrUtil.isBlank(protocol) ? "http" : "https";
		// 拼接url
		UrlBuilder urlBuilder = UrlBuilder.create();
		String agentPath = sysConfigService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
		urlBuilder.setScheme(protocol).setHost(host).setPort(port).appendPath(agentPath).appendPath("auth")
				.appendPath("refresh");
		// 调用 confagent接口 获取结果
		String result = AgentUtil.requestGet(urlBuilder.build(), token, String.class);
		log.info("result:{}", result);
		// 响应结果
		if (Tool.StrUtil.isNotEmpty(result)) {
			return Tool.JSONUtil.toBean(result, R.class);
		} else {
			return R.error(500, "token refresh failed");
		}
	}

}
