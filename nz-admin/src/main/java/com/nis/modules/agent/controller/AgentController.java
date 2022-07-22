package com.nis.modules.agent.controller;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.sys.service.SysConfigService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/agent")
public class AgentController {
	private Log log = Log.get();

	@Value("${nezha.installPath:install/}")
	private String installPath;

	@Value("${server.ssl.enabled}")
	private String sslEnabled;

	@Value("${thread.count}")
	private Integer count;

	static Integer defaultCorePoolSize = 5;

	static ThreadPoolExecutor execPingThreadPoolExecutor;

	@Autowired
	private AgentService agentService;

	@Autowired
	private SysConfigService sysConfigService;

	static {
		execPingThreadPoolExecutor = new ThreadPoolExecutor(defaultCorePoolSize, Integer.MAX_VALUE,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder().setNamePrefix("ota-pool-").build());
	}

	/**
	 * 详情
	 *
	 * @param id
	 * @return
	 */
	@GetMapping("/{id}")
	public R agentDetails(@PathVariable("id") Integer id) {
		return R.ok(agentService.queryAgentDetails(id));
	}

	@GetMapping
	@SysLog(operation = OperationEnum.QUERY, type = TypeEnum.PROMSERVER)
	public R list(@RequestParam Map<String, Object> params) {
		PageUtils page = agentService.queryPage(params);
		return R.ok(page);
	}

	@PostMapping
	@SysLog(operation = OperationEnum.ADD, type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"agent:save"})
	public R save(@RequestBody Agent agent) {

		Map map = new HashMap(2);
		map.put("id", agentService.saveOrUpdateAgent(agent, false));
		return R.ok(map);
	}

	@PutMapping
	@SysLog(operation = OperationEnum.UPDATE, type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"agent:update"})
	public R modify(@RequestBody Agent agent) {
		ValidateUtils.is(agent.getId()).notNull(RCode.AGENT_ID_ISNULL);

		Map map = new HashMap(2);
		map.put("id", agentService.saveOrUpdateAgent(agent, true));
		return R.ok(map);
	}

	@DeleteMapping
	@SysLog(operation = OperationEnum.DELETE, type = TypeEnum.PROMSERVER)
	@RequiresPermissions({"agent:delete"})
	public R remove(String ids) {
		ValidateUtils.is(ids).notNull(RCode.AGENT_ID_ISNULL);
		String[] split = ids.split(",");
		agentService.removeByIds(Arrays.asList(split));
		return R.ok();
	}

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
			map.put("sslEnabled",sslEnabled);
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
	 * @Description 将confagent自动注册 为 agent
	 * @Author rui
	 * @Date 2021/4/2
	 */
	@PostMapping("/register")
	@RequiresPermissions({ "agent:save" })
	public R autoRegister(HttpServletRequest request, @RequestBody Agent agent) {
		if (Tool.StrUtil.isEmpty(agent.getHost())) {
			agent.setHost(request.getRemoteAddr());
		}
		// 保存 agent
		Integer id = agentService.saveOrUpdateAgent(agent, false);
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

	@PutMapping("/ota")
	public R ota(@RequestParam String ids){
		ValidateUtils.is(ids).notNull(RCode.AGENT_ID_ISNULL);
		List<String> agentIds = Arrays.asList(ids.split(","));
		//获取可用的agent
		List<Agent> agentList = agentService.list(new LambdaQueryWrapper<Agent>().eq(Agent::getStatus, "1").in(Agent::getId, agentIds));
		if(Tool.CollectionUtil.isEmpty(agentList)){
			throw new NZException(RCode.AGENT_UNAVAILABLE);
		}
		//agent是否全部可用
		List<Integer> agentIdsList = agentList.stream().map(Agent::getId).collect(Collectors.toList());
		List<String> agentIdsNotInList = agentIds.stream().filter(id -> !agentIdsList.contains(Integer.valueOf(id))).collect(Collectors.toList());
		if(Tool.CollectionUtil.isNotEmpty(agentIdsNotInList)){
			throw new NZException(RCode.AGENT_UNAVAILABLE);
		}

		//获取agent更新包
		String json = sysConfigService.getValue("confagent_name");
		JSONObject confagentNames = Tool.JSONUtil.parseObj(json);

		//配置的正则
		String agentName = (String) confagentNames.get("centos");
		Pattern agentPattern = Pattern.compile(agentName);
		try {
			File installPathFile = CommonUtils.getResourceFile(installPath);
			log.info("installPath exist: {},isDir:{}", installPathFile == null ? false : installPathFile.exists(), installPathFile == null ? false : installPathFile.isDirectory());
			if (installPathFile == null || !installPathFile.isDirectory()) {
				throw new NZException("update package not exist", 500);
			}

			File[] listFiles = installPathFile.listFiles((file, name) -> {
				return agentPattern.matcher(name).find();
			});

			if (Tool.ArrayUtil.isEmpty(listFiles)) {
				throw new NZException("update package not exist", 500);
			}
			File updateFile = CommonUtils.getResourceFile(listFiles[0].getAbsolutePath());


			if (count != null){
				execPingThreadPoolExecutor.setCorePoolSize(count);
			}

			for (Agent agent : agentList) {
				execPingThreadPoolExecutor.execute(new Runnable() {
					@Override
					public void run() {
						String host = agent.getHost();
						Integer port = agent.getPort();
						String protocol = agent.getProtocol();
						String token = agent.getToken();
						//校验参数
						ValidateUtils.is(host).notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(token)
								.notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(port).notNull(RCode.AGENT_INSTALL_PARAM_MISS);
						protocol = Tool.StrUtil.isBlank(protocol) ? "http" : "https";
						try {
							//获取文件md5
							String updateFileMd5 = Tool.DigestUtil.md5Hex(new FileInputStream(updateFile));
							//md5加密
							String md5 = Tool.AesUtil.encrypt(updateFileMd5, Constant.AES_SECRET_KEY);
							// 拼接url
							UrlBuilder urlBuilder = UrlBuilder.create();
							String agentPath = sysConfigService.getValueOrDefault(Constant.CONFAGENT_PATH, "");
							urlBuilder.setScheme(protocol).setHost(host).setPort(port).appendPath(agentPath).appendPath("ota");
							log.info("agent ota url : {}", urlBuilder.toString());
							//发送请求
							AgentUtil.requestPost(urlBuilder.toString() , HttpMethod.POST , token , updateFile, md5);
						} catch (Exception e) {
							log.error("agent update failed",500);
						}
					}
				});
			}
		} catch (Exception e) {
			log.error("agent update failed", 500);
		}
		return R.ok();
	}

}
