package com.nis.common.utils;

import cn.hutool.core.net.NetUtil;
import cn.hutool.log.Log;
import org.apache.commons.collections.CollectionUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 
 * 公用方法工具类
 *
 */
public class CommonUtils {
    private final static Log logger = Log.get();
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

    public static Long toLong(Object val, Long defVal) {
        if (isEmpty(val)) {
            return defVal;
        }
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static Long toLong(Object val) {
        return toLong(val, null);
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


    public static float toFloat(Object val, float defVal) {
        if (isEmpty(val)) {
            return defVal;
        }
        try {
            return Float.parseFloat(val.toString());
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static Boolean toBoolean(String text, Boolean defVal) {
        if (isEmpty(text)) {
            return false;
        }
        try {
            return Boolean.parseBoolean(text);
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public static Boolean toBoolean(String text) {
        return toBoolean(text, false);
    }

    public static Integer toInt(Object val) {
        return toInt(val, null);
    }

    public static Float toFloat(Object val) {
        return toFloat(val, 0f);
    }

    /**
     * 对Null作预处理
     *
     * @param obj   待处理的对象
     * @param clazz 该对象的类型
     * @return T 返回处理后的不为Null的该对象
     * @author <a href="mailto:benjobs@qq.com">Ben</a>
     * @see <b>对Null作预处理,有效避免NullPointerException</b>
     * @since 1.0
     */
    public static <T> T preparedNull(T obj, Class<?> clazz) {

        if (notEmpty(obj)) {
            return obj;
        }

        AssertUtils.notNull(clazz, "this class must be not null!");

        Object val = null;

        // 单列集合
        if (List.class.isAssignableFrom(clazz)) {
            val = new ArrayList<Object>(0);
        } else if (Set.class.isAssignableFrom(clazz)) {
            val = new HashSet<Object>(0);
        } else if (Map.class.isAssignableFrom(clazz)) {
            val = new HashMap<Object, Object>(0);
        } else {
            try {
                val = clazz.newInstance();
            } catch (IllegalAccessException e) {
                logger.error(e);
            } catch (InstantiationException e) {
                logger.error(e);
            }
        }
        return (T) val;
    }


    public static List arrayToList(Object source) {
        return Arrays.asList(ObjectUtils.toObjectArray(source));
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

    /**
     * Check whether the given Collection contains the given element instance.
     * <p>Enforces the given instance to be present, rather than returning
     * <code>true</code> for an equal element as well.
     *
     * @param collection the Collection to check
     * @param element    the element to look for
     * @return <code>true</code> if found, <code>false</code> else
     */
    public static boolean containsInstance(Collection collection, Object element) {
        if (collection != null) {
            for (Object candidate : collection) {
                if (candidate == element) {
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
     * Adapt an enumeration to an iterator.
     *
     * @param enumeration the enumeration
     * @return the iterator
     */
    public static <E> Iterator<E> toIterator(Enumeration<E> enumeration) {
        @SuppressWarnings("hiding")
        class EnumerationIterator<E> implements Iterator<E> {
            private Enumeration<E> enumeration;

            public EnumerationIterator(Enumeration<E> enumeration) {
                this.enumeration = enumeration;
            }

            @Override
            public boolean hasNext() {
                return this.enumeration.hasMoreElements();
            }

            @Override
            public E next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                } else {
                    return this.enumeration.nextElement();
                }
            }

            @Override
            public void remove() throws UnsupportedOperationException {
                throw new UnsupportedOperationException("Not supported");
            }
        }
        return new EnumerationIterator<E>(enumeration);
    }

    //获取系统名字
    public static String getOsName() {
        return OS;
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
    /**
     * 将String数组转化为Long数组
     *
     * @param strArr String数组
     * @return Long数组
     * @Title: strArr2LongArr
     * @author: wanghajie 2012-12-13上午10:15:42
     */
    public static Long[] string2LongArray(String[] strArr) {
        if (CommonUtils.isEmpty(strArr)) {
            return null;
        }
        Long longArray[] = new Long[strArr.length];
        for (int i = 0; i < longArray.length; i++) {
            longArray[i] = StringUtils.parseLong(strArr[i]);
        }
        return longArray;
    }

    /**
     * 将将String数组转化为LongList
     *
     * @param strArr String数组
     * @return LongList
     * @Title: strArr2LongList
     * @author: wanghajie 2012-12-13上午11:09:10
     */
    public static List<Long> string2LongList(String[] strArr) {
        // 将String数组转化为Long数组
        Long[] longArr = string2LongArray(strArr);
        return longArr == null ? ((List<Long>) Collections.EMPTY_LIST) : Arrays.asList(longArr);
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <T> T[] arrayRemoveElements(T[] array, T... elem) {
        AssertUtils.notNull(array);
        List<T> arrayList = new ArrayList<T>(0);
        Collections.addAll(arrayList, array);
        if (isEmpty(elem)) return array;
        for (T el : elem) arrayList.remove(el);
        return Arrays.copyOf(arrayList.toArray(array), arrayList.size());
    }

    public static <T> T[] arrayRemoveIndex(T[] array, int... index) {
        AssertUtils.notNull(array);
        for (int j : index) {
            if (j < 0 || j > array.length - 1) throw new IndexOutOfBoundsException("index error.@" + j);
        }
        List<T> arrayList = new ArrayList<T>(0);
        Collections.addAll(arrayList, array);
        int i = 0;
        for (int j : index) {
            arrayList.remove(j - i);
            ++i;
        }
        return Arrays.copyOf(arrayList.toArray(array), arrayList.size());
    }


    public static <T> T[] arrayInsertIndex(T[] array, int index, T t) {
        AssertUtils.notNull(array);
        List<T> arrayList = new ArrayList<T>(array.length + 1);
        if (index == 0) {
            arrayList.add(t);
            Collections.addAll(arrayList, array);

        } else {
            T[] before = Arrays.copyOfRange(array, 0, index);
            T[] after = Arrays.copyOfRange(array, index, array.length);
            Collections.addAll(arrayList, before);
            arrayList.add(t);
            Collections.addAll(arrayList, after);
        }
        return arrayList.toArray(array);
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

    public static String getSystemCharset(){
        return System.getProperty("file.encoding");
    }

    /**
     * 获取本机所有网卡IP
     * @return
     */
    public static Set<String> getInetIpAddress() {

        return NetUtil.localIpv4s();
    }

    /**
     * 处理导入错误信息
     * @param failDetails
     * @return
     */
    public static List<Map> handleImportFailDetails(List<Map> failDetails) {
        if (CollectionUtils.isEmpty(failDetails)) return Collections.emptyList();
        Map<Object, List<Map>> listMap = failDetails.stream().collect(Collectors.groupingBy(map -> map.get("lineNo")));

        List<Map> resultData = new ArrayList<>(listMap.size());
        List<Map> tempList;
        Map tempMap;
        String msg, lineFailInfos;
        for (Map.Entry<Object, List<Map>> entry : listMap.entrySet()) {
            tempList = entry.getValue();
            lineFailInfos = "";
            for (Map map : tempList) {
                msg = (String) map.get("errorMsg");
                lineFailInfos += msg + (msg.endsWith(".") ? " " : ". ");
            }
            tempMap = new HashMap(4);
            tempMap.put("lineNo", entry.getKey());
            tempMap.put("errorMsg", lineFailInfos);
            resultData.add(tempMap);
        }
        return resultData;
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

	public static boolean checkDomain(String domain){
        boolean flag = Pattern.matches(DOMAIN_NAME_STR, domain);
	    return flag;
    }

    /**
     * 校验url
     *
     * @param url
     * @return
     */
    public static boolean checkUrl(String url) {
        Pattern compile = Pattern.compile("("
                + "("
                + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?" + ")?"
                + "(?:" + DOMAIN_NAME_STR + ")"
                + "(?:" + PORT_NUMBER + ")?"
                + ")"
                + "(" + PATH_AND_QUERY + ")?"
                + WORD_BOUNDARY
                + ")");
        return compile.matcher(url).matches();
    }

    public static boolean checkUrlInModel(String url) {
        Pattern compile = Pattern.compile("("
                + "("
                + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?" + ")?"
                + "(?:" + DOMAIN_NAME_IN_MODEL + ")"
                + "(?:" + PORT_NUMBER + ")?"
                + ")"
                + "(" + PATH_AND_QUERY + ")?"
                + WORD_BOUNDARY
                + ")");
        return compile.matcher(url).matches();
    }

    private static  final String DOMAIN_NAME_IN_MODEL = "(" + "[a-zA-Z0-9.]*" + ")";

    private static final String IP_ADDRESS_STRING =
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))";

    /**
     * Valid characters for IRI label defined in RFC 3987.
     */
    private static final String LABEL_CHAR = "a-zA-Z0-9";

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

    private static final String PROTOCOL = "(?i:http|https|rtsp|ftp)://";

    /* A word boundary or end of input.  This is to stop foo.sure from matching as foo.su */
    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

    private static final String USER_INFO = "(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
            + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
            + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@";

    private static final String PORT_NUMBER = "\\:\\d{1,5}";

    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";
}
