package com.nis.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.nis.modules.alert.entity.AlertRuleEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class YamlUtil {
	
	private static Yaml y;
	
	@Value("${nezha.promtoolPath}")
	private String promtoolPath;
	
	static {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		y = new Yaml(options);
	}

	/**
	 * @param toCheck 待校验的map
	 * @return
	 * @throws Exception
	 */
	public boolean checkRule(Map toCheck) throws Exception {
		boolean success = false;
		
		File file = new File(Constant.TEMP_PATH + File.separator + "promRuleCheck" + System.currentTimeMillis() + ".yml");
		OutputStream out = new FileOutputStream(file);
		OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
		y.dump(toCheck, osw); //写出为文件
		
		//校验
		StringReader result = RuntimeUtil.run(null, null, new String[]{promtoolPath + File.separator + "promtool", "check", "rules", file.getAbsolutePath()});
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
	
	/**
	 * 将告警规则实体类转化为符合prometheus告警规则格式的Map
	 * @param alertRule
	 * @return
	 */
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
        rule.put("expr", "(" + alertRule.getExpr() + ")" + alertRule.getOperator() + alertRule.getThreshold());
		rule.put("for", alertRule.getLast() + "s");
		rule.put("labels", labels);
		rule.put("annotations", annotations);
		
		return toCheck;
	}

    /**
     * 读取 yaml 生成 json 并返回
     *
     * @param file
     * @return
     */
    public String yamlToJson(String file) throws FileNotFoundException {
        Map<String, Object> loaded = null;
        FileInputStream fis = new FileInputStream(file);
        loaded = (Map<String, Object>) y.load(fis);
        return JSONObject.toJSONString(loaded);
    }


    /**
     * 將json 转化为 yaml 格式并生成 yaml 文件
     *
     * @param jsonString
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     */
    public void createYaml(String yaml_url, String jsonString) throws JsonProcessingException, IOException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        // save it as YAML
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);

        Map<String, Object> map = (Map<String, Object>) y.load(jsonAsYaml);

        FileWriter writer;
        try {
            writer = new FileWriter(yaml_url);
            y.dump(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
