package com.nis.common.config;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.modules.panel.entity.VisualPanel;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.report.job.ReportJobManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.service.LicenseService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysUserService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.setup.entity.SetupEntity;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.setting.dialect.Props;

/**
 * setup初始化操作
 * @author fang
 *
 */
@Component
public class SetupRunner implements CommandLineRunner{
	private static final Log log = Log.get();
	@Value("${nezha.inited}")
    private int inited;
	
    @Value("${nezha.adminId:1}")
    private String adminId;

    @Value("${server.tomcat.basedir}")
    private String tmpDir;
    @Value("${nezha.config.path:./config/nezha.properties}")
    private String nezhaConfigPath;
    
    @Value("${compileType:release}")
	private String compileType;
    
    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    SysUserService sysUserService;
    
    @Autowired
    LicenseService licenseService;

    @Autowired
	ReportJobManager reportJobManager;

    @Autowired
	VisualPanelService visualPanelService;
	
    
    
	@Override
	public void run(String... args) throws Exception {		
		log.info("compileType  value  is : "+compileType);
		//启动时加载证书
		if(! Tool.StrUtil.equalsIgnoreCase(compileType, "debug")) {
			boolean lr = licenseService.installLicense();
			log.info("license install {}", lr);
		}
		log.info("Setup inited {}", inited);

		// 加载已有 panel report job type = DASHBOARD
		log.info("Setup dashboard report job begin...");
		List<VisualPanel> panelList = visualPanelService.list(new LambdaQueryWrapper<VisualPanel>().eq(VisualPanel::getType, Constant.PanelType.DASHBOARD.getValue()));
		for (VisualPanel panel : panelList) {
			Map paramMap = JSONObject.parseObject(panel.getParam().toString(), Map.class);
			if (Tool.MapUtil.isNotEmpty(paramMap)) {
				String reportEnable = JSONPath.read(panel.getParam().toString(), "report.enable").toString();
				if (Tool.StrUtil.equalsIgnoreCase("true", StrUtil.nullToDefault(reportEnable, "false"))) {
					reportJobManager.createReportJob(panel.getParam().toString(), panel.getId().toString());
				}
			}
		}
		log.info("Setup dashboard report job finished");

		if (inited == 2) {
			log.info(" inited = {}	,skip init setup", inited);
			return;
		}
		if (inited == 1) {
			log.info(" inited = {}	, init system config license_token", inited);
			// 创建设备16位数字的license token
			String licenseToken = sysConfigService.getValue("license_token");
			if(ToolUtil.isEmpty(licenseToken)|| ! licenseToken.startsWith("GDNT-")||licenseToken.length()!=21) {
				int random = (int) (Math.random()*9+1);
		        String valueOf = String.valueOf(random);
		        //生成uuid的hashCode值
		        int hashCode = UUID.randomUUID().toString().hashCode();
		        //可能为负数
		        if(hashCode<0){
		            hashCode = -hashCode;
		        }
		        licenseToken = StrUtil.concat(true, "GDNT-",valueOf,String.format("%015d", hashCode));
		        sysConfigService.updateValueByKey("license_token",licenseToken);
			}
		}
		File file = Tool.FileUtil.file(tmpDir, Constant.SETUP_FILE_NAME);
		log.debug("file path: {}",file.getAbsoluteFile());
		if(file.exists()) {
			log.info("start inited setup");
			String jsonStr = Tool.FileUtil.readUtf8String(file);
	        SetupEntity setupEntity = Tool.JSONUtil.toBean(jsonStr, SetupEntity.class);
	        sysConfigService.updateValueByKey("alert_api",setupEntity.getSystem().getAlertPath()==null ? "":setupEntity.getSystem().getAlertPath());
//	        sysConfigService.updateValueByKey("alert_path_prefix",setupEntity.getSystem().getAlertPrefix());
	        sysConfigService.updateValueByKey("prometheus_federation_enabled",setupEntity.getSystem().getFederationEnabled());
	        log.info("update db config");
	        SysUserEntity sysUser =  sysUserService.getById(adminId);
	        sysUser.setId(Long.parseLong(adminId));
	        sysUser.setUsername(setupEntity.getSystem().getUsername());
	        sysUser.setPin(ShiroUtils.sha256(setupEntity.getSystem().getPin(), sysUser.getSalt()));
	        log.info("update admin user");
	        sysUserService.updateById(sysUser);
	        Tool.FileUtil.del(file);//删除临时配置文件
		}
		//配置文件
    	boolean absolutePath = Tool.FileUtil.isAbsolutePath(nezhaConfigPath);
		File configFile = absolutePath ? Tool.FileUtil.file(nezhaConfigPath) : Tool.FileUtil.file(Tool.WebPathUtil.getRootPath(),nezhaConfigPath);
		//更改 nezha.inited 为 2，下次启动时不再执行
		//nezha.properties 配置文件路径：./config/nezha.properties
		Props prop = new Props(configFile, Constant.DEFAULT_CHARTSET_NAME);
		prop.put("nezha.inited", "2");
		prop.store(configFile.getAbsolutePath());
	}

}
