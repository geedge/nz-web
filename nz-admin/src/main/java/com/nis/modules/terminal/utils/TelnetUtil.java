package com.nis.modules.terminal.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.nis.common.utils.Tool;
import lombok.Data;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * TelnetUtil
 * user login
 *
 * @author HUAWEI
 */
@Data
public class TelnetUtil {

    private byte[] buff = new byte[2048];

    private TelnetClient telnetClient = new TelnetClient();

    private InputStream inputStream = null;
    private OutputStream outputStream = null;

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
        telnetClient.connect(ip, Tool.ObjectUtil.defaultIfNull(port, 23));
        inputStream = telnetClient.getInputStream();
        outputStream = telnetClient.getOutputStream();
    }

    /**
     * login
     *
     * @param user
     * @param pin
     * @param userPrompt
     * @param pinPrompt
     * @return
     * @throws IOException
     */
    public boolean login(String user, String pin, String userPrompt, String pinPrompt) throws IOException {
        userPrompt = Tool.StrUtil.emptyToDefault(userPrompt, USER_PROMPT);
        pinPrompt = Tool.StrUtil.emptyToDefault(pinPrompt, PIN_PROMPT);

        // user name
        String read = readString();
        while (true) {
            if (Tool.ReUtil.isMatch(userPrompt, read.trim())) {
                break;
            } else {
                read = readString();
            }
        }
        write(user);

        // pin
        read = readString();
        while (true) {
            if (Tool.ReUtil.isMatch(pinPrompt, read.trim())) {
                break;
            } else {
                read = readString();
            }
        }
        write(pin);

        for (; ; ) {
            read = readString();
            if (StrUtil.contains(read, "Last login")) {
                return true;
            } else if (StrUtil.contains(read, "Login incorrect")) {
                return false;
            }
        }
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
