package com.nis.modules.agent;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.HttpConnection;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.log.Log;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.service.SysConfigService;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class AgentController {
    private Log log = Log.get();

    @Value("${nezha.resourcePath:./}")
    private String resourcePath;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private SysConfigService sysConfigService;

    /**
    * @Description 下载 install.sh脚本
    * @Author  rui
    * @Date   2021/4/2
    */
    @GetMapping("/{dcId}/{type}/install.sh")
    public String pushInstallScript(HttpServletRequest request, @PathVariable("dcId") Integer dcId, @PathVariable("type") Integer type){
        ValidateUtils.is(dcId).notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(type).notNull(RCode.AGENT_INSTALL_PARAM_MISS);

        String scriptPath = resourcePath+File.separator+"install"+ File.separator+"install.sh.template";

        String scriptContent = Tool.FileUtil.readUtf8String(scriptPath);

        String localhost = request.getLocalAddr(); //web component ip
        Integer localPort = request.getLocalPort();

        String remoteHost = request.getRemoteHost(); // agent component ip

        String webToken = request.getHeader(Constant.AUTH_TOKEN_CODE);

        Map<String,String> map = new HashMap<>();

        map.put("remoteIP",localhost); // to install.sh is remote
        map.put("remotePort",localPort+"");
        map.put("localhost",remoteHost); // to install.sh is local
        map.put("webToken",webToken);
        map.put("datacenterID",dcId+"");
        map.put("type",type+"");

        scriptContent = replaceParamter(map,scriptContent);

        return scriptContent;
    }


    /**
    * @Description 下载confagent 的安装包
    * @Author  rui
    * @Date   2021/4/2
    */
    @GetMapping("/download")
    public void downloadAgent(HttpServletResponse response, @RequestParam(defaultValue = "centos",required = false) String os){
        log.info("download agent package to {}",os);
        String json = sysConfigService.getValue("confagent_name");
        JSONObject confagentNames = Tool.JSONUtil.parseObj(json);
        String confagentName = (String)confagentNames.get(os);
        downloadAgentToOs(response,confagentName);
    }

    /**
    * @Description 将confagent自动注册 为promserver
    * @Author  rui
    * @Date   2021/4/2
    */
    @PostMapping("/register")
    public R autoRegister(HttpServletRequest request,@RequestBody Promserver promserver){
        if(Tool.StrUtil.isEmpty(promserver.getHost())){
            promserver.setHost(request.getRemoteAddr());
        }

        return R.ok().putData("id",promserverService.saveOrUpdatePromserver(promserver, false));
    }


    /**
    * @Description 访问confagent refresh接口，刷新confagent token
    * @Author  rui
    * @Date   2021/4/2
    */
    @PostMapping("/token/refresh")
    public void refreshToken(HttpServletResponse response,@RequestBody Map<String,Object> params){
        String host = (String)params.get("host");
        String token = (String)params.get("token");
        Integer port = (Integer)params.get("port");

        ValidateUtils.is(host).notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(token).notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(port).notNull(RCode.AGENT_INSTALL_PARAM_MISS);

        UrlBuilder urlBuilder = UrlBuilder.create();
        urlBuilder.setScheme("http").setHost(host).setPort(port).appendPath("confagent").appendPath("auth").appendPath("refresh");
        refreshToken(urlBuilder.build(),token,response);
    }

    private String replaceParamter(Map<String,String> keyValue,String content){
        if(Tool.MapUtil.isEmpty(keyValue)) return content;

        try{
            Template template = TemplateUtil.stringToTemplate(content,"installTemplate");

            content = FreeMarkerTemplateUtils.processTemplateIntoString(template, keyValue);

        }catch (IOException | TemplateException e){
            log.error("install template parse error",e);
        }
        return content;
    }

    private void downloadAgentToOs(HttpServletResponse response, String filename){
        File file = Tool.FileUtil.file(resourcePath,"install",filename);
        response.reset();
        response.setContentType("application/octet-stream");
        String fileName = Tool.URLUtil.encode(file.getName(), Tool.CharsetUtil.UTF_8);
        response.addHeader("Content-Disposition", "attachment; filename=" +fileName );
        response.addHeader("Content-Length", "" + file.length());

        try {
            Tool.FileUtil.writeToStream(file,new BufferedOutputStream(response.getOutputStream()));
        } catch (IOException e) {
            log.error("download agent package failed",e);
        }
    }

    private void refreshToken(String url,String token,HttpServletResponse response){
        OutputStream responseOutputStream = null;
        InputStream connInputStream = null;
        try{

            response.setContentType("application/json");
            responseOutputStream = response.getOutputStream();

            HttpRequest get = Tool.HttpUtil.createGet(url);
            get.auth(token);
            HttpResponse executeResponse = get.execute(false);
            connInputStream = executeResponse.bodyStream();
            Tool.IoUtil.copy(connInputStream, responseOutputStream);
            responseOutputStream.flush();//flush 输出流

        }catch (Exception e){
            try {
                response.sendError(500, "request error");
            } catch (IOException e1) {
                log.error("proxy request error",e1);
            }
            log.error("request error : ",e);
        }finally {
            Tool.IoUtil.close(responseOutputStream);
            Tool.IoUtil.close(connInputStream);
        }
    }

}
