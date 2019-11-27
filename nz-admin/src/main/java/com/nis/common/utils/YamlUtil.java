package com.nis.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.nis.modules.alert.entity.AlertRuleEntity;


public class YamlUtil {
	
	private static Yaml y;
	
	static {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		y = new Yaml(options);
	}

	public static boolean checkRule(Map toCheck) throws Exception {
		boolean success = false;
		
		File file = new File(Constant.TEMP_PATH + File.separator + "promRuleCheck" + System.currentTimeMillis() + ".yml");
		OutputStream out = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
		y.dump(toCheck, osw); //写出为文件
		
		//校验
		StringReader result = RuntimeUtil.run(null, null, new String[]{Configurations.getStringProperty("promtool.path", Constant.TOOL_PATH) + File.separator + "promtool", "check", "rules", file.getAbsolutePath()});
		BufferedReader reader = new BufferedReader(result);
		String line1 = reader.readLine();
		if (!StringUtils.isBlank(line1)) {
			String line2 = reader.readLine();
			if (!StringUtils.isBlank(line2) && line2.contains("SUCCESS")) {
				success = true;
			}
		}
		
		//校验结束后若文件存在则删除文件
		if (file.isFile()) {
			file.delete();
		}
		
		return success;
	}
	
	public static Map alertRuleEntityToYamlMap(AlertRuleEntity alertRule) {
		Map toCheck = new HashMap();
		
		List groups = new ArrayList();
		toCheck.put("groups", groups);
		
		Map group = new HashMap();
		groups.add(group);
		
		List rules = new ArrayList();
		group.put("name", "check" + System.currentTimeMillis());
		group.put("rules", rules);
		
		Map rule = new HashMap();
		rules.add(rule);
		
		Map labels = new HashMap();
		labels.put("severity", alertRule.getSeverity());
		Map annotations = new HashMap();
		annotations.put("summary", alertRule.getSummary());
		annotations.put("description", alertRule.getDescription());
		
		rule.put("alert", alertRule.getAlertName());
		rule.put("expr", alertRule.getExpr());
		rule.put("for", alertRule.getLast() + "s");
		rule.put("labels", labels);
		rule.put("annotations", annotations);
		
		return toCheck;
	}
	
	public static void main(String[] args) throws Exception {
		/*AlertRuleEntity alertRule = new AlertRuleEntity();
		alertRule.setAlertName("test");
		alertRule.setDescription("descr");
		alertRule.setExpr("up == 1");
		alertRule.setLast(50);
		alertRule.setReceiver(11);
		alertRule.setSeverity("warning");
		alertRule.setSummary("xixi");
		
		checkRule(alertRuleEntityToYamlMap(alertRule));*/
		//File f = new File("C:/Users/song/git/nz-web/nz-admin/tmp/promRuleCheck1574821467589.yml");
	}
}
