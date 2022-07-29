package com.nis.modules.agent.controller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nis.common.annotation.SysLog;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.OperationEnum;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.common.utils.TemplateUtil;
import com.nis.common.utils.Tool;
import com.nis.common.utils.TypeEnum;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.agent.service.AgentService;
import com.nis.modules.agent.util.AgentUtil;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.log.Log;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@RestController
@RequestMapping("/agent")
public class AgentController {
	private Log log = Log.get();

	@Value("${nezha.installPath:install/}")
	private String installPath;

	@Value("${server.ssl.enabled}")
	private String sslEnabled;

	@Autowired
	private AgentService agentService;

	@Autowired
	private SysConfigService sysConfigService;

	@Autowired
	private AgentUtil agentUtil;
	
	@Value("${nezha.otaConcurrentThreads:5}")
	private Integer otaConcurrentThreads;
	/**
	 * ota 升级线程池
	 */
	private ThreadPoolExecutor otaExecutor;

	@Autowired
	private RedisTemplate redisTemplate;

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
		File rpmFile = agentUtil.getAgentRpmFile(os);
		try {
			Tool.ResponseUtil.downloadFile(response, rpmFile);
		} catch (Exception e) {
			log.error(e,"agent rpm download error");
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
	@RequiresPermissions({"agent:update"})
	public R ota(String ids) {
		ValidateUtils.is(ids).notNull(RCode.AGENT_ID_ISNULL);
		List<String> idList = Tool.ListUtil.toList(ids.split(","));
		Collection<Agent> agentList = agentService.listByIds(idList);
		// 检查 agent 是否全部可用
		agentList.forEach( agent -> {
			Integer status = agent.getStatus();
			if(Tool.ObjectUtil.notEqual(status, 1)) {
				throw new NZException("There is an agent with abnormal state",999);
			}
		});
		//异步调用 agent ota 接口执行升级
		agentList.forEach(agent -> {
			this.getOtaExecutor().execute(new Runnable() {
				@Override
				public void run() {
					Thread.currentThread().setName("agent-ota-"+ agent.getId());
					log.info("ota start");
					String result = agentUtil.agentOTA(agent);
					log.info("ota result: {}",result);
					log.info("ota end");
				}
			});
			log.info("execute agent ota, agent: {}",Tool.JSONUtil.toJsonStr(agent));
		});
		return R.ok();
	}

	@PutMapping("/resetConfig")
	@RequiresPermissions({"agent:update"})
	public R resetConfig() {
		HashOperations opsForHash = redisTemplate.opsForHash();
		Map<String, String> confEvent = opsForHash.entries(Constant.REDIS_KEY_CONF_EVENT);
		if(!Tool.MapUtil.isNotEmpty(confEvent)) {
			return R.error(500, "confEvent is empty");
		}
		log.info("confEvent: {}", Tool.JSONUtil.toJsonStr(confEvent));
		opsForHash.delete(Constant.REDIS_KEY_CONF_EVENT,confEvent.keySet().toArray());
		return R.ok();
	}
	
	public synchronized ThreadPoolExecutor getOtaExecutor() {
		if(this.otaExecutor == null) {
			otaExecutor = Tool.ThreadUtil.newExecutor(1, otaConcurrentThreads);
		}
		return otaExecutor;
	}
	
}
