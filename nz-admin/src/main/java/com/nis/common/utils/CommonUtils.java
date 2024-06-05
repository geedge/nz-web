package com.nis.common.utils;

import cn.hutool.Hutool;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import org.apache.commons.net.util.SubnetUtils;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 
 * 公用方法工具类
 *
 */
public class CommonUtils {
	private static String OS = System.getProperty("os.name").toLowerCase();

    /**
     * 非空判断
     *
     * @param objs 要判断,处理的对象
     * @return Boolean
     * @author <a href="mailto:benjobs@qq.com">Ben</a>
     * @see <b>对象为Null返回true,集合的大小为0也返回true,迭代器没有下一个也返回true..</b>
     * @since 1.0
     */
    public static Boolean isEmpty(Object... objs) {

        if (objs == null) {
            return Boolean.TRUE;
        }

        if (objs.length == 0) return Boolean.TRUE;

        for (Object obj : objs) {
            if (obj == null) {
                return true;
            }

            // 字符序列集
            if ((obj instanceof CharSequence) && "".equals(obj.toString().trim())) {
                return true;
            }
            // 单列集合
            if (obj instanceof Collection) {
                if (((Collection<?>) obj).isEmpty()) {
                    return true;
                }
            }
            // 双列集合
            if (obj instanceof Map) {
                if (((Map<?, ?>) obj).isEmpty()) {
                    return true;
                }
            }

            if (obj instanceof Iterable) {
                if (((Iterable<?>) obj).iterator() == null || !((Iterable<?>) obj).iterator().hasNext()) {
                    return true;
                }
            }

            // 迭代器
            if (obj instanceof Iterator) {
                if (!((Iterator<?>) obj).hasNext()) {
                    return true;
                }
            }

            // 文件类型
            if (obj instanceof File) {
                if (!((File) obj).exists()) {
                    return true;
                }
            }

            if ((obj instanceof Object[]) && ((Object[]) obj).length == 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * 空判断
     *
     * @param obj 要判断,处理的对象
     * @return Boolean
     * @author <a href="mailto:benjobs@qq.com">Ben</a>
     * @see <b>与非空相反</b>
     * @since 1.0
     */
    public static Boolean notEmpty(Object... obj) {
        return !isEmpty(obj);
    }



    public static Integer toInt(Object val, Integer defVal) {
        if (isEmpty(val)) {
            return defVal;
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defVal;
        }
    }





    public static Integer toInt(Object val) {
        return toInt(val, null);
    }




    public static boolean contains(Iterator iterator, Object element) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (ObjectUtils.safeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether the given Enumeration contains the given element.
     *
     * @param enumeration the Enumeration to check
     * @param element     the element to look for
     * @return <code>true</code> if found, <code>false</code> else
     */
    public static boolean contains(Enumeration enumeration, Object element) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Object candidate = enumeration.nextElement();
                if (ObjectUtils.safeEquals(candidate, element)) {
                    return true;
                }
            }
        }
        return false;
    }



    public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
        ArrayList<A> elements = new ArrayList<A>();
        while (enumeration.hasMoreElements()) {
            elements.add(enumeration.nextElement());
        }
        return elements.toArray(array);
    }


    /**
    * @Description 根据相对路径获取根目录下的文件
    * @Date   2021/4/23
    */
    public static File getResourceFile(String path){
        File file = Tool.FileUtil.newFile(path);
        if(file.exists()){
            return file;
        }else{
            URL resource = CommonUtils.class.getClassLoader().getResource(path);
            return resource == null?null: Tool.FileUtil.newFile(resource.getFile());
        }
    }

    public static boolean isLinux(){
        return OS.indexOf("linux")>=0;
    }

    public static boolean isMacOS(){
        return OS.indexOf("mac")>=0&&OS.indexOf("os")>0&&OS.indexOf("x")<0;
    }

    public static boolean isMacOSX(){
        return OS.indexOf("mac")>=0&&OS.indexOf("os")>0&&OS.indexOf("x")>0;
    }

    public static boolean isWindows(){
        return OS.indexOf("windows")>=0;
    }

    public static boolean isOS2(){
        return OS.indexOf("os/2")>=0;
    }

    public static boolean isSolaris(){
        return OS.indexOf("solaris")>=0;
    }

    public static boolean isSunOS(){
        return OS.indexOf("sunos")>=0;
    }

    public static boolean isMPEiX(){
        return OS.indexOf("mpe/ix")>=0;
    }

    public static boolean isHPUX(){
        return OS.indexOf("hp-ux")>=0;
    }

    public static boolean isAix(){
        return OS.indexOf("aix")>=0;
    }

    public static boolean isOS390(){
        return OS.indexOf("os/390")>=0;
    }

    public static boolean isFreeBSD(){
        return OS.indexOf("freebsd")>=0;
    }

    public static boolean isIrix(){
        return OS.indexOf("irix")>=0;
    }

    public static boolean isDigitalUnix(){
        return OS.indexOf("digital")>=0&&OS.indexOf("unix")>0;
    }

    public static boolean isNetWare(){
        return OS.indexOf("netware")>=0;
    }

    public static boolean isOSF1(){
        return OS.indexOf("osf1")>=0;
    }

    public static boolean isOpenVMS(){
        return OS.indexOf("openvms")>=0;
    }

    public static boolean isUnix() {
        boolean isUnix = isLinux();
        if (!isUnix) {
            isUnix = isMacOS();
        }
        if (!isUnix) {
            isUnix = isMacOSX();
        }
        if (!isUnix) {
            isUnix = isLinux();
        }
        if (!isUnix) {
            isUnix = isDigitalUnix();
        }
        if (!isUnix) {
            isUnix = isAix();
        }
        if (!isUnix) {
            isUnix = isFreeBSD();
        }
        if (!isUnix) {
            isUnix = isHPUX();
        }
        if (!isUnix) {
            isUnix = isIrix();
        }
        if (!isUnix) {
            isUnix = isMPEiX();
        }
        if (!isUnix) {
            isUnix = isNetWare();
        }
        if (!isUnix) {
            isUnix = isOpenVMS();
        }
        if (!isUnix) {
            isUnix = isOS2();
        }
        if (!isUnix) {
            isUnix = isOS390();
        }
        if (!isUnix) {
            isUnix = isOSF1();
        }
        if (!isUnix) {
            isUnix = isSunOS();
        }
        if (!isUnix) {
            isUnix = isSolaris();
        }
        return isUnix;
    }
    /**
     * linux内核平台 1
     * window： 2
     * 其他平台 0
     */
    public static int getPlatform(){
        int platform = 0;
        if (CommonUtils.isUnix()) {
            platform = 1;
        }
        if (CommonUtils.isWindows()) {
            platform = 2;
        }
        return platform;
    }


    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 生成指定长度的uuid
     *
     * @param len
     * @return
     */
    public static String uuid(int len) {
        StringBuffer sb = new StringBuffer();
        while (sb.length() < len) {
            sb.append(uuid());
        }
        return sb.toString().substring(0, len);
    }


    /**
     * 获取本机所有网卡IP
     * @return
     */
    public static Set<String> getInetIpAddress() {

        return NetUtil.localIpv4s();
    }

    /**
     * 检查 id 集合 是否一致
     * @param list
     * @param list1
     * @return
     */
    public static boolean checkIdListDiffrent(List<Integer> list, List<Integer> list1) {
        if (list.size() != list1.size())
            return false;

        for (Integer i : list) {
            if (!list1.contains(i))
                return false;
        }
        return true;
    }

	/**
	 * 端口合法性校验
	 * @param port
	 * @return
	 */
	public static boolean checkPort(String port) {
		String regex = "^[1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]{1}|6553[0-5]$";
		boolean flag = Pattern.matches(regex, port.toString());
		return flag;
	}
	
	public static boolean checkIp(String ip) {
		Pattern ipv6Check = Pattern.compile("^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:)|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}(:[0-9A-Fa-f]{1,4}){1,2})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){1,3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){1,4})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){1,5})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){1,6})|(:(:[0-9A-Fa-f]{1,4}){1,7})|(([0-9A-Fa-f]{1,4}:){6}(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){0,1}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){0,2}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){0,4}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(:(:[0-9A-Fa-f]{1,4}){0,5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}))$");
		Pattern ipv4Check = Pattern.compile("^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$");
		boolean ipv6Result = ipv6Check.matcher(ip).matches();
		boolean ipv4Result = ipv4Check.matcher(ip).matches();
		if (!(ipv6Result||ipv4Result)){
            return false;
        }
        return true;
	}

	public static boolean checkIpv4(String ip) {
		Pattern ipv4Check = Pattern.compile("^(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}$");
		boolean ipv4Result = ipv4Check.matcher(ip).matches();
		if (!ipv4Result){
            return false;
        }
        return true;
	}
	
	public static boolean checkIpv6(String ip) {
		Pattern ipv6Check = Pattern.compile("^((([0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){1,7}:)|(([0-9A-Fa-f]{1,4}:){6}:[0-9A-Fa-f]{1,4})|(([0-9A-Fa-f]{1,4}:){5}(:[0-9A-Fa-f]{1,4}){1,2})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){1,3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){1,4})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){1,5})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){1,6})|(:(:[0-9A-Fa-f]{1,4}){1,7})|(([0-9A-Fa-f]{1,4}:){6}(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){4}(:[0-9A-Fa-f]{1,4}){0,1}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){3}(:[0-9A-Fa-f]{1,4}){0,2}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(([0-9A-Fa-f]{1,4}:){2}(:[0-9A-Fa-f]{1,4}){0,3}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|([0-9A-Fa-f]{1,4}:(:[0-9A-Fa-f]{1,4}){0,4}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})|(:(:[0-9A-Fa-f]{1,4}){0,5}:(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}))$");
		boolean ipv6Result = ipv6Check.matcher(ip).matches();
		if (!ipv6Result){
            return false;
        }
        return true;
	}

    // Method to validate IPv4 range
    public static boolean checkIPv4Range(String ipRange) {
        try {
            String[] parts = ipRange.split("-");
            Ipv4Util.list(parts[0], parts[1]);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Method to validate IPv4 CIDR
    public static boolean checkIPv4CIDR(String cidr) {
        try {
            SubnetUtils utils = new SubnetUtils(cidr);
            utils.getInfo(); // 如果 CIDR 格式不合法，这里会抛出异常
            return true;
        } catch (Exception e) {
            return false;
        }
    }
	
	public static boolean checkDomain(String domain){
        boolean flag = Pattern.matches(DOMAIN_NAME_STR, domain);
	    return flag;
    }

    private static final String IP_ADDRESS_STRING =
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))";

    /**
     * Valid characters for IRI label defined in RFC 3987.
     */
    private static final String LABEL_CHAR = "a-zA-Z0-9\\s";

    /**
     * Valid characters for IRI TLD defined in RFC 3987.
     */
    private static final String TLD_CHAR = "a-zA-Z";

    /**
     * RFC 1035 Section 2.3.4 limits the labels to a maximum 63 octets.
     */
    private static final String IRI_LABEL =
            "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "_\\-]{0,61}[" + LABEL_CHAR + "]){0,1}";

    /**
     * RFC 3492 references RFC 1034 and limits Punycode algorithm output to 63 characters.
     */
    private static final String PUNYCODE_TLD = "xn\\-\\-[\\w\\-]{0,58}\\w";

    private static final String TLD = "(" + PUNYCODE_TLD + "|" + "[" + TLD_CHAR + "]{2,63}" + ")";

    private static final String HOST_NAME = "(" + IRI_LABEL + "\\.)+" + TLD;

    private static final String DOMAIN_NAME_STR = "(" + HOST_NAME + "|" + IP_ADDRESS_STRING + ")";

    public static boolean checkPinCorrect(String pin,int code) {
    	if(ToolUtil.isEmpty(pin)) {
    		return false;
    	}
    	switch(code){
    		// 校验字符中是否包含数字
    		case 1:
    			for(int i=0; i<pin.length();i++) {
		    		char charAt = pin.charAt(i);
		    		Integer ascii = Integer.valueOf(charAt);
		    		if(ascii>=48&&ascii<=57) {
		    			return true;
		    		}
		    	}
		    	break;
		    // 校验字符中是否包含小写字母
    		case 2:
    			for(int i=0; i<pin.length();i++) {
		    		char charAt = pin.charAt(i);
		    		Integer ascii = Integer.valueOf(charAt);
		    		if(ascii>=97&&ascii<=122) {
		    			return true;
		    		}
		    	}
    			break;
    		// 校验字符中是否包含大写字母
    		case 3:
    			for(int i=0; i<pin.length();i++) {
		    		char charAt = pin.charAt(i);
		    		Integer ascii = Integer.valueOf(charAt);
		    		if(ascii>=65&&ascii<=90) {
		    			return true;
		    		}
		    	}
    			break;
    		// 校验字符中是否包含特殊字符
    		case 4:
    			for(int i=0; i<pin.length();i++) {
		    		char charAt = pin.charAt(i);
		    		Integer ascii = Integer.valueOf(charAt);
		    		if((ascii>=33&&ascii<=47)||(ascii>=58&&ascii<=64)||(ascii>=91&&ascii<=96)||(ascii>=123&&ascii<=126)) {
		    			return true;
		    		}
		    	}
    			break;
    	}
    	return false;
    }
    public static boolean checkMac(String mac) {
        Pattern checkMac = Pattern.compile("^([A-Fa-f0-9]{2}[-,:]){5}[A-Fa-f0-9]{2}$");
        boolean matches = checkMac.matcher(mac).matches();
        if (!matches){
            return false;
        }
        return true;
    }


    /**
     * 根据系统配置时区偏移量 获取 SimpleDateFormat 对象
     *
     * @param offset
     * @param format
     * @return
     */
    public static DateFormat getDateFormatByOffset(Integer offset, String format) {
        SimpleDateFormat dateformat = new SimpleDateFormat(format,  Locale.US);
        TimeZone tz = new SimpleTimeZone(offset, "");
        dateformat.setTimeZone(tz);
        return dateformat;
    }
}
