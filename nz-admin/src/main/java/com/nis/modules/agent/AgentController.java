package com.nis.modules.agent;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.http.HttpConnection;
import cn.hutool.log.Log;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
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
    @Value("${spring.profiles.active}")
    private String profile;

    @Autowired
    private PromserverService promserverService;


    /**
    * @Description 下载 install.sh脚本
    * @Author  rui
    * @Date   2021/4/2
    */
    @GetMapping("/{dcId}/{type}/install.sh")
    public String pushInstallScript(HttpServletRequest request, @PathVariable("dcId") Integer dcId, @PathVariable("type") Integer type){
        ValidateUtils.is(dcId).notNull(RCode.AGENT_INSTALL_PARAM_MISS).and(type).notNull(RCode.AGENT_INSTALL_PARAM_MISS);

        String scriptPath = getScriptPath();

        String scriptContent = Tool.FileUtil.readUtf8String(scriptPath);

        String localhost = request.getLocalAddr(); //web component ip
        Integer localPort = request.getLocalPort();

        String remoteHost = request.getRemoteHost(); // agent component ip

        String webToken = request.getHeader(Constant.AUTH_TOKEN_CODE);

        String rootPath = getRootPath();

        Map<String,String> map = new HashMap<>();

        map.put("remoteIP",localhost); // to install.shis remote
        map.put("remotePort",localPort+"");
        map.put("localhost",remoteHost); // to install.sh is local
        map.put("webToken",webToken);
        map.put("rootPath",rootPath);
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
        switch (os){
            case "centos":
                downloadAgentToCentOS(response);
                break;
        }
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

        Map map = new HashMap(2);
        map.put("id", promserverService.saveOrUpdatePromserver(promserver, false));
        return R.ok(map);
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

    private String getScriptPath(){
        String rootPath = getRootPath();
        return rootPath+File.separator+"install"+ File.separator+"install.sh.template";
    }

    private String getRootPath(){
        if(Tool.StrUtil.equals("prod",profile)){
            return System.getProperty("user.dir");
        }
        return Tool.ClassUtil.getClassPath();
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

    private void downloadAgentToCentOS(HttpServletResponse response){
        File file = Tool.FileUtil.file(getRootPath(),"install","confagent.rpm");
        describeDownloadReponse(response,file);

        try {
            Tool.FileUtil.writeToStream(file,new BufferedOutputStream(response.getOutputStream()));
        } catch (IOException e) {
            log.error("download agent package failed",e);
        }
    }

    private void describeDownloadReponse(HttpServletResponse response,File file){
        response.reset();
        response.setContentType("application/octet-stream");
        String fileName = Tool.URLUtil.encode(file.getName(), Tool.CharsetUtil.UTF_8);
        response.addHeader("Content-Disposition", "attachment; filename=" +fileName );
        response.addHeader("Content-Length", "" + file.length());
    }

    private void refreshToken(String url,String token,HttpServletResponse response){
        HttpURLConnection conn = HttpConnection.create(url,null).getHttpURLConnection();
        InputStream connInputStream = null;
        OutputStream responseOutputStream = null;
        try{
            ReflectUtil.invoke(conn,"addRequestProperty",Constant.AUTH_TOKEN_CODE,token);
            responseOutputStream = response.getOutputStream();
            log.info("connect to confagent with URL:{},Token:{}",url,token);
            conn.connect();
            int responseCode = conn.getResponseCode();
            connInputStream = (responseCode < 400)? conn.getInputStream():conn.getErrorStream();
            String responseMessage = conn.getResponseMessage();
            response.setStatus(responseCode, responseMessage);

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
            Tool.IoUtil.close(connInputStream);
            Tool.IoUtil.close(responseOutputStream);
            conn.disconnect();
        }
    }

}
