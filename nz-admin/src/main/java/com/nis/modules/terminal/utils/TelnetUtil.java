package com.nis.modules.terminal.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import lombok.Data;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.*;

/**
 * TelnetUtil
 * user login
 *
 * @author HUAWEI
 */
@Data
public class TelnetUtil {

    private static final Log log = Log.get();

    private byte[] buff = new byte[2048];

    private TelnetClient telnetClient = new TelnetClient();

    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    private String host;
    private Integer port;

    /**
     * default Prompt str
     */
    private static final String USER_PROMPT = ".*ogin:";
    private static final String PIN_PROMPT = ".*assword:";

    /**
     * init TelnetClient
     */
    public TelnetUtil() {
        // VT100 VT52 VT220 VTNT ANSI
        telnetClient = new TelnetClient("VT100");
    }

    /**
     * connect
     *
     * @param ip
     * @throws IOException
     */
    public void connect(String ip, Integer port) throws IOException {
        this.host = ip;
        this.port = Tool.ObjectUtil.defaultIfNull(port, 23);
        telnetClient.connect(ip, Tool.ObjectUtil.defaultIfNull(port, 23));
        inputStream = telnetClient.getInputStream();
        outputStream = telnetClient.getOutputStream();
    }

    /**
     * login
     * 防止该方法出现解析失败情况，陷入死循环，设置 3 分钟登录超时，超时则视为登录失败
     *
     * @param user
     * @param pin
     * @param userPrompt
     * @param pinPrompt
     * @return
     * @throws IOException
     */
    public boolean login(String user, String pin, String userPrompt, String pinPrompt) throws IOException {
        log.info("[login begin] [host: {}] [user: {}] [userPrompt: {}] [pinPrompt: {}]", this.host, user, userPrompt, pinPrompt);
        userPrompt = Tool.StrUtil.emptyToDefault(userPrompt, USER_PROMPT);
        pinPrompt = Tool.StrUtil.emptyToDefault(pinPrompt, PIN_PROMPT);

        String finalUserPrompt = userPrompt;
        String finalPinPrompt = pinPrompt;
        Callable<Boolean> task = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // user name
                String read = readString();
                while (true) {
                    log.info("[login] [read the response content. find user name prompt] [host: {}] [response: {}] [user tip: {}]", host, read, finalUserPrompt);
                    if (Tool.ReUtil.isMatch(finalUserPrompt, read.trim())) {
                        break;
                    } else {
                        read = readString();
                    }
                }
                log.info("[login] [write user name] [host: {}] [user: {}]", host, user);
                write(user);

                // pin
                read = readString();
                while (true) {
                    log.info("[login] [read the response content. find password prompt] [host: {}] [response: {}] [password tip: {}]", host, read, finalPinPrompt);
                    if (Tool.ReUtil.isMatch(finalPinPrompt, read.trim())) {
                        break;
                    } else {
                        read = readString();
                    }
                }
                log.info("[login] [write password] [host: {}] [pin: {}]", host, "******");
                write(pin);

                for (; ; ) {
                    read = readString();
                    log.info("[login] [server response] [content: {}]", read);
                    if (StrUtil.containsIgnoreCase(read, "Last login")) {
                        log.info("[login] [login success] [host: {}] [user: {}]", host, user);
                        return true;
                    } else if (StrUtil.containsIgnoreCase(read, "Login incorrect")) {
                        log.warn("[login] [login failed] [host: {}] [user: {}]", host, user);
                        return false;
                    }
                }
            }
        };
        Boolean loginResult = false;
        ExecutorService executorService = Tool.ThreadUtil.newExecutor();
        try {
            Future<Boolean> future = executorService.submit(task);
            if (!future.isCancelled()) {
                // 防止该方法出现解析失败情况，陷入死循环，设置 3分钟登录超时，超时则视为登录失败
                loginResult = future.get(3, TimeUnit.MINUTES);
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error(e, "[login] [error] [host: {}] [user: {}] [userPrompt: {}] [pinPrompt: {}]", this.host, user, userPrompt, pinPrompt);
        } catch (TimeoutException e) {
            log.error(e, "[login] [login timeout] [host: {}] [user: {}] [userPrompt: {}] [pinPrompt: {}]", this.host, user, userPrompt, pinPrompt);
        } finally {
            executorService.shutdown();
        }
        log.info("[TelnetUtil] [login finshed] [host: {}] [user: {}]", this.host, user);
        return loginResult;
    }


    /**
     * read
     *
     * @return
     * @throws IOException
     */
    public String readString() throws IOException {
        int size = inputStream.read(this.buff);
        if (size == Tool.StrUtil.INDEX_NOT_FOUND) {
            throw new IndexOutOfBoundsException();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            char chr = (char) (buff[i] & 0xff);
            sb.append(chr);
        }

        return sb.toString().trim();
    }


    /**
     * 向服务器写字符串
     *
     * @param text
     * @throws IOException
     */
    public void write(String text) {
        IoUtil.write(outputStream, false, text.getBytes());
        IoUtil.write(outputStream, false, Tool.StrUtil.CRLF.getBytes());
        IoUtil.flush(outputStream);
        // sleep for wait response
        Tool.ThreadUtil.sleep(200);
    }

    /**
     * simply write
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    public String exec(String cmd) throws IOException {
        write(cmd);
        return readString();
    }

    /**
     * close
     */
    public void close() {
        try {
            write("exit");
            write("exit");
            write("exit");
        } catch (Exception ex) {
        }

        // close
        IoUtil.close(inputStream);
        IoUtil.close(outputStream);

        try {
            if (null != telnetClient) {
                telnetClient.disconnect();
            }
        } catch (Exception e) {
        }
    }

    /**
     * get Telenet Login Tip
     * prompt information for priority use of assets
     *
     * @param assetTip
     * @param systemTipConfigMap
     * @param configKey
     * @return
     */
    public static String getTelenetLoginTip(String assetTip, Map<String, String> systemTipConfigMap, String configKey) {
        // main asset tip
        if (Tool.StrUtil.isNotEmpty(assetTip)) {
            return ".*" + assetTip.trim();
        }
        // system tip config
        String systemConfigTip = systemTipConfigMap.get(configKey);
        return ".*" + systemConfigTip;
    }

}
