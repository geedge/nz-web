package com.nis.common.utils;

import java.awt.Graphics;
import java.awt.Robot;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

public class Tool {

	/**
	 * 数据库操作工具类
	 */
	public static cn.hutool.db.DbUtil DbUtil = new cn.hutool.db.DbUtil();
	/**
	 * 安全相关工具类<br>
	 * 加密分为三种：<br>
	 * 1、对称加密（symmetric），例如：AES、DES等<br>
	 * 2、非对称加密（asymmetric），例如：RSA、DSA等<br>
	 * 3、摘要加密（digest），例如：MD5、SHA-1、SHA-256、HMAC等<br>
	 */
	public static cn.hutool.crypto.SecureUtil SecureUtil = new cn.hutool.crypto.SecureUtil();
	/**
	 * 代理工具类
	 */
	public static cn.hutool.aop.ProxyUtil ProxyUtil = new cn.hutool.aop.ProxyUtil();
	/**
	 * 敏感词工具类
	 */
	public static cn.hutool.dfa.SensitiveUtil SensitiveUtil = new  cn.hutool.dfa.SensitiveUtil();
	
	/**
	 * 数据库元数据信息工具类
	 *
	 * <p>
	 * 需要注意的是，此工具类在某些数据库（比如Oracle）下无效，此时需要手动在数据库配置中增加：
	 * <pre>
	 *  remarks = true
	 *  useInformationSchema = true
	 * </pre>
	 *
	 * @author looly
	 */
	public static class MetaUtil extends cn.hutool.db.meta.MetaUtil {}
	/**
	 * 异常工具类
	 *
	 * @author Looly
	 */
	public static class ExceptionUtil extends cn.hutool.core.exceptions.ExceptionUtil {}
	/**
	 * 正则相关工具类<br>
	 * 常用正则请见 {@link cn.hutool.core.lang.Validator}
	 *
	 * @author xiaoleilu
	 */
	public static class ReUtil extends cn.hutool.core.util.ReUtil {}
	/**
	 * SOAP相关工具类
	 * 
	 * @author looly
	 * @since 4.5.7
	 */
	public static class SoapUtil extends cn.hutool.http.webservice.SoapUtil {}
	/**
	 * 脚本工具类
	 *
	 * @author Looly
	 */
	public static class ScriptUtil extends cn.hutool.script.ScriptUtil {}
	/**
	 * 提供Unicode字符串和普通字符串之间的转换
	 *
	 * @author 兜兜毛毛, looly
	 * @since 4.0.0
	 */
	public static class UnicodeUtil extends cn.hutool.core.text.UnicodeUtil {}
	/**
	 * 时间工具类
	 *
	 * @author xiaoleilu
	 */
	public static class DateUtil extends cn.hutool.core.date.DateUtil {}
	/**
	 * EC密钥参数相关工具类封装
	 *
	 * @author looly
	 * @since 5.4.3
	 */
	public static class ECKeyUtil extends cn.hutool.crypto.ECKeyUtil {}
	/**
	 * 枚举工具类
	 *
	 * @author looly
	 * @since 3.3.0
	 */
	public static class EnumUtil extends cn.hutool.core.util.EnumUtil {}
	/**
	 * 数学相关方法工具类<br>
	 * 此工具类与{@link cn.hutool.core.util.NumberUtil}属于一类工具，NumberUtil偏向于简单数学计算的封装，MathUtil偏向复杂数学计算
	 * 
	 * @author looly
	 * @since 4.0.7
	 */
	public static class MathUtil extends cn.hutool.core.math.MathUtil {}
	/**
	 * Sax方式读取Excel相关工具类
	 *
	 * @author looly
	 */
	public static class ExcelSaxUtil extends cn.hutool.poi.excel.sax.ExcelSaxUtil {}
	/**
	 * 数字工具类<br>
	 * 对于精确值计算应该使用 {@link BigDecimal}<br>
	 * JDK7中<strong>BigDecimal(double val)</strong>构造方法的结果有一定的不可预知性，例如：
	 *
	 * <pre>
	 * new BigDecimal(0.1)
	 * </pre>
	 * <p>
	 * 表示的不是<strong>0.1</strong>而是<strong>0.1000000000000000055511151231257827021181583404541015625</strong>
	 *
	 * <p>
	 * 这是因为0.1无法准确的表示为double。因此应该使用<strong>new BigDecimal(String)</strong>。
	 * </p>
	 * 相关介绍：
	 * <ul>
	 * <li>http://www.oschina.net/code/snippet_563112_25237</li>
	 * <li>https://github.com/venusdrogon/feilong-core/wiki/one-jdk7-bug-thinking</li>
	 * </ul>
	 *
	 * @author Looly
	 */
	public static class NumberUtil extends cn.hutool.core.util.NumberUtil {}
	/**
	 * Jsch工具类<br>
	 * Jsch是Java Secure Channel的缩写。JSch是一个SSH2的纯Java实现。<br>
	 * 它允许你连接到一个SSH服务器，并且可以使用端口转发，X11转发，文件传输等。<br>
	 *
	 * @author Looly
	 * @since 4.0.0
	 */
	public static class JschUtil extends cn.hutool.extra.ssh.JschUtil {}
	/**
	 * 线程池工具
	 *
	 * @author luxiaolei
	 */
	public static class ThreadUtil extends cn.hutool.core.thread.ThreadUtil {}
	/**
	 * Oshi库封装的工具类，通过此工具类，可获取系统、硬件相关信息
	 * 
	 * <pre>
	 * 1、系统信息
	 * 2、硬件信息
	 * </pre>
	 *
	 * 相关内容见：https://github.com/oshi/oshi
	 * 
	 * @author Looly
	 * @since 4.6.4
	 */
	public static class OshiUtil extends cn.hutool.system.oshi.OshiUtil {}
	/**
	 * URL（Uniform Resource Locator）统一资源定位符相关工具类
	 *
	 * <p>
	 * 统一资源定位符，描述了一台特定服务器上某资源的特定位置。
	 * </p>
	 * URL组成：
	 * <pre>
	 *   协议://主机名[:端口]/ 路径/[:参数] [?查询]#Fragment
	 *   protocol :// hostname[:port] / path / [:parameters][?query]#fragment
	 * </pre>
	 *
	 * @author xiaoleilu
	 */
	public static class URLUtil extends cn.hutool.core.util.URLUtil {}
	/**
	 * ID生成器工具类，此工具类中主要封装：
	 *
	 * <pre>
	 * 1. 唯一性ID生成器：UUID、ObjectId（MongoDB）、Snowflake
	 * </pre>
	 *
	 * <p>
	 * ID相关文章见：http://calvin1978.blogcn.com/articles/uuid.html
	 *
	 * @author looly
	 * @since 4.1.13
	 */
	public static class IdUtil extends cn.hutool.core.util.IdUtil {}
	/**
	 * Http请求工具类
	 *
	 * @author xiaoleilu
	 */
	public static class HttpUtil extends cn.hutool.http.HttpUtil {}
	/**
	 * 桌面相关工具（平台相关）<br>
	 * Desktop 类允许 Java 应用程序启动已在本机桌面上注册的关联应用程序，以处理 URI 或文件。
	 * 
	 * @author looly
	 * @since 4.5.7
	 */
	public static class DesktopUtil extends cn.hutool.core.swing.DesktopUtil {}
	/**
	 * 图片处理工具类：<br>
	 * 功能：缩放图像、切割图像、旋转、图像类型转换、彩色转黑白、文字水印、图片水印等 <br>
	 * 参考：http://blog.csdn.net/zhangzhikaixinya/article/details/8459400
	 *
	 * @author Looly
	 */
	public static class ImgUtil extends cn.hutool.core.img.ImgUtil {}
	/**
	 * 分页工具类
	 *
	 * @author xiaoleilu
	 */
	public static class PageUtil extends cn.hutool.core.util.PageUtil {}
	/**
	 * HTML工具类
	 * 
	 * <p>
	 * 比如我们在使用爬虫爬取HTML页面后，需要对返回页面的HTML内容做一定处理，<br>
	 * 比如去掉指定标签（例如广告栏等）、去除JS、去掉样式等等，这些操作都可以使用此工具类完成。
	 * 
	 * @author xiaoleilu
	 * 
	 */
	public static class HtmlUtil extends cn.hutool.http.HtmlUtil {}
	/**
	 * PEM(Privacy Enhanced Mail)格式相关工具类。（基于Bouncy Castle）
	 *
	 * <p>
	 * PEM一般为文本格式，以 -----BEGIN... 开头，以 -----END... 结尾，中间的内容是 BASE64 编码。
	 * <p>
	 * 这种格式可以保存证书和私钥，有时我们也把PEM格式的私钥的后缀改为 .key 以区别证书与私钥。
	 *
	 * @author looly
	 * @since 5.1.6
	 */
	public static class PemUtil extends cn.hutool.crypto.PemUtil {}
	/**
	 * 分词工具类
	 * 
	 * @author looly
	 * @since 4.3.3
	 */
	public static class TokenizerUtil extends cn.hutool.extra.tokenizer.TokenizerUtil {}
	/**
	 * {@link org.apache.poi.ss.extractor.ExcelExtractor}工具封装
	 *
	 * @author looly
	 * @since 5.4.4
	 */
	public static class ExcelExtractorUtil extends cn.hutool.poi.excel.ExcelExtractorUtil {}
	/**
	 * 邮件工具类，基于javax.mail封装
	 *
	 * @author looly
	 * @since 3.1.2
	 */
	public static class MailUtil extends cn.hutool.extra.mail.MailUtil {}
	/**
	 * NIO中Path对象操作封装
	 *
	 * @author looly
	 * @since 5.4.1
	 */
	public static class PathUtil extends cn.hutool.core.io.file.PathUtil {}
	/**
	 * 邮件内部工具类
	 * @author looly
	 * @since 3.2.3
	 */
	public static class InternalMailUtil extends cn.hutool.extra.mail.InternalMailUtil {}
	/**
	 * 定时任务工具类<br>
	 * 此工具持有一个全局{@link cn.hutool.cron.Scheduler}，所有定时任务在同一个调度器中执行<br>
	 * {@link #setMatchSecond(boolean)} 方法用于定义是否使用秒匹配模式，如果为true，则定时任务表达式中的第一位为秒，否则为分，默认是分
	 * 
	 * @author xiaoleilu
	 *
	 */
	public static class CronUtil extends cn.hutool.cron.CronUtil {}
	/**
	 * Java的System类封装工具类。<br>
	 * 参考：http://blog.csdn.net/zhongweijian/article/details/7619383
	 *
	 * @author Looly
	 */
	public static class SystemUtil extends cn.hutool.system.SystemUtil {}
	/**
	 * 压缩工具类<br>
	 * 基于commons-compress的压缩解压封装
	 *
	 * @author looly
	 * @since 5.5.0
	 */
	public static class CompressUtil extends cn.hutool.extra.compress.CompressUtil {}
	/**
	 * {@link ClassLoader}工具类
	 * 
	 * @author Looly
	 * @since 3.0.9
	 */
	public static class ClassLoaderUtil extends cn.hutool.core.util.ClassLoaderUtil {}
	/**
	 * {@link Spliterator}相关工具类
	 *
	 * @author looly
	 * @since 5.4.3
	 */
	public static class SpliteratorUtil extends cn.hutool.core.collection.SpliteratorUtil {}
	/**
	 * 监听工具类<br>
	 * 主要负责文件监听器的快捷创建
	 * 
	 * @author Looly
	 * @since 3.1.0
	 */
	public static class WatchUtil extends cn.hutool.core.io.watch.WatchUtil {}
	/**
	 * Excel样式工具类
	 * 
	 * @author looly
	 * @since 4.0.0
	 */
	public static class StyleUtil extends cn.hutool.poi.excel.style.StyleUtil {}
	/**
	 * 系统剪贴板工具类
	 * 
	 * @author looly
	 * @since 3.2.0
	 */
	public static class ClipboardUtil extends cn.hutool.core.swing.clipboard.ClipboardUtil {}
	/**
	 * 文件类型判断工具类
	 *
	 * <p>此工具根据文件的前几位bytes猜测文件类型，对于文本、zip判断不准确，对于视频、图片类型判断准确</p>
	 *
	 * <p>需要注意的是，xlsx、docx等Office2007格式，全部识别为zip，因为新版采用了OpenXML格式，这些格式本质上是XML文件打包为zip</p>
	 *
	 * @author Looly
	 */
	public static class FileTypeUtil extends cn.hutool.core.io.FileTypeUtil {}
	/**
	 * 引用工具类，主要针对{@link Reference} 工具化封装<br>
	 * 主要封装包括：
	 * <pre>
	 * 1. {@link SoftReference} 软引用，在GC报告内存不足时会被GC回收
	 * 2. {@link WeakReference} 弱引用，在GC时发现弱引用会回收其对象
	 * 3. {@link PhantomReference} 虚引用，在GC时发现虚引用对象，会将{@link PhantomReference}插入{@link ReferenceQueue}。 此时对象未被真正回收，要等到{@link ReferenceQueue}被真正处理后才会被回收。
	 * </pre>
	 * 
	 * @author looly
	 * @since 3.1.2
	 */
	public static class ReferenceUtil extends cn.hutool.core.util.ReferenceUtil {}
	/**
	 * 布隆过滤器工具
	 * 
	 * @author looly
	 * @since 4.1.5
	 */
	public static class BloomFilterUtil extends cn.hutool.bloomfilter.BloomFilterUtil {}
	/**
	 * 修饰符工具类
	 * 
	 * @author looly
	 * @since 4.0.5
	 */
	public static class ModifierUtil extends cn.hutool.core.util.ModifierUtil {}
	/**
	 * 集合的stream操作封装
	 *
	 * @author 528910437@QQ.COM
	 * @since 5.5.2
	 */
	public static class CollStreamUtil extends cn.hutool.core.collection.CollStreamUtil {}
	/**
	 * 摘要算法工具类
	 *
	 * @author Looly
	 */
	public static class DigestUtil extends cn.hutool.crypto.digest.DigestUtil {}
	/**
	 * 对象工具类，包括判空、克隆、序列化等操作
	 *
	 * @author Looly
	 */
	public static class ObjectUtil extends cn.hutool.core.util.ObjectUtil {}
	/**
	 * Hash算法大全<br>
	 * 推荐使用FNV1算法
	 *
	 * @author Goodzzp, Looly
	 */
	public static class HashUtil extends cn.hutool.core.util.HashUtil {}
	/**
	 * Bouncy Castle相关工具类封装
	 *
	 * @author looly
	 * @since 4.5.0
	 */
	public static class BCUtil extends cn.hutool.crypto.BCUtil {}
	/**
	 * 针对 {@link Type} 的工具类封装<br>
	 * 最主要功能包括：
	 *
	 * <pre>
	 * 1. 获取方法的参数和返回值类型（包括Type和Class）
	 * 2. 获取泛型参数类型（包括对象的泛型参数或集合元素的泛型类型）
	 * </pre>
	 *
	 * @author Looly
	 * @since 3.0.8
	 */
	public static class TypeUtil extends cn.hutool.core.util.TypeUtil {}
	/**
	 * Excel文件工具类
	 * 
	 * @author looly
	 * @since 4.2.1
	 */
	public static class ExcelFileUtil extends cn.hutool.poi.excel.ExcelFileUtil {}
	/**
	 * NIO工具类
	 *
	 * @since 5.4.0
	 */
	public static class NioUtil extends cn.hutool.socket.nio.NioUtil {}
	/**
	 * 类工具类 <br>
	 *
	 * @author xiaoleilu
	 */
	public static class ClassUtil extends cn.hutool.core.util.ClassUtil {}
	/**
	 * 针对{@link Calendar} 对象封装工具类
	 *
	 * @author looly
	 * @since 5.3.0
	 */
	public static class CalendarUtil extends cn.hutool.core.date.CalendarUtil {}
	/**
	 * 网络相关工具
	 *
	 * @author xiaoleilu
	 */
	public static class NetUtil extends cn.hutool.core.net.NetUtil {}
	/**
	 * 源码编译工具类，主要封装{@link JavaCompiler} 相关功能
	 *
	 * @author looly
	 * @since 5.5.2
	 */
	public static class CompilerUtil extends cn.hutool.core.compiler.CompilerUtil {}
	/**
	 * 锁相关工具
	 *
	 * @author looly
	 * @since 5.2.5
	 */
	public static class LockUtil extends cn.hutool.core.thread.lock.LockUtil {}
	/**
	 * 文件名相关工具类
	 *
	 * @author looly
	 * @since 5.4.1
	 */
	public static class FileNameUtil extends cn.hutool.core.io.file.FileNameUtil {}
	/**
	 * SM国密算法工具类<br>
	 * 此工具类依赖org.bouncycastle:bcpkix-jdk15on
	 *
	 * @author looly
	 * @since 4.3.2
	 */
	public static class SmUtil extends cn.hutool.crypto.SmUtil {}
	/**
	 * Props工具类<br>
	 * 提供静态方法获取配置文件
	 *
	 * @author looly
	 * @since 5.1.3
	 */
	public static class PropsUtil extends cn.hutool.setting.dialect.PropsUtil {}
	/**
	 * XML工具类<br>
	 * 此工具使用w3c dom工具，不需要依赖第三方包。<br>
	 * 工具类封装了XML文档的创建、读取、写出和部分XML操作
	 *
	 * @author xiaoleilu
	 */
	public static class XmlUtil extends cn.hutool.core.util.XmlUtil {}
	/**
	 * Velocity模板引擎工具类<br>
	 * 使用前必须初始化工具类
	 *
	 * @author xiaoleilu
	 * @deprecated 使用TemplateUtil替代
	 */
	public static class VelocityUtil extends cn.hutool.extra.template.engine.velocity.VelocityUtil {}
	/**
	 * {@link ByteBuffer} 工具类<br>
	 * 此工具来自于 t-io 项目以及其它项目的相关部分收集<br>
	 * ByteBuffer的相关介绍见：https://www.cnblogs.com/ruber/p/6857159.html
	 * 
	 * @author tanyaowu, looly
	 * @since 4.0.0
	 *
	 */
	public static class BufferUtil extends cn.hutool.core.io.BufferUtil {}
	/**
	 * 表达式引擎工具类
	 *
	 * @author looly
	 * @since 5.5.0
	 */
	public static class ExpressionUtil extends cn.hutool.extra.expression.ExpressionUtil {}
	/**
	 * 驱动相关工具类，包括自动获取驱动类名
	 * 
	 * @author looly
	 * @since 4.0.10
	 */
	public static class DriverUtil extends cn.hutool.db.dialect.DriverUtil {}
	/**
	 * IO工具类<br>
	 * IO工具类只是辅助流的读写，并不负责关闭流。原因是流可能被多次读写，读写关闭后容易造成问题。
	 *
	 * @author xiaoleilu
	 */
	public static class IoUtil extends cn.hutool.core.io.IoUtil {}
	/**
	 * JDK8+中的{@link LocalDateTime} 工具类封装
	 *
	 * @author looly
	 * @since 5.3.9
	 */
	public static class LocalDateTimeUtil extends cn.hutool.core.date.LocalDateTimeUtil {}
	/**
	 * 规范化对象生成工具
	 *
	 * @author looly
	 * @since 5.4.3
	 */
	public static class InternUtil extends cn.hutool.core.lang.intern.InternUtil {}
	/**
	 * 压缩工具类
	 *
	 * @author Looly
	 */
	public static class ZipUtil extends cn.hutool.core.util.ZipUtil {}
	/**
	 * 十六进制（简写为hex或下标16）在数学中是一种逢16进1的进位制，一般用数字0到9和字母A到F表示（其中:A~F即10~15）。<br>
	 * 例如十进制数57，在二进制写作111001，在16进制写作39。<br>
	 * 像java,c这样的语言为了区分十六进制和十进制数值,会在十六进制数的前面加上 0x,比如0x20是十进制的32,而不是十进制的20<br>
	 * <p>
	 * 参考：https://my.oschina.net/xinxingegeya/blog/287476
	 *
	 * @author Looly
	 */
	public static class HexUtil extends cn.hutool.core.util.HexUtil {}
	/**
	 * 数据大小工具类
	 *
	 * @author looly
	 * @since 5.3.10
	 */
	public static class DataSizeUtil extends cn.hutool.core.io.unit.DataSizeUtil {}
	/**
	 * User-Agent工具类
	 * 
	 * @author looly
	 *
	 */
	public static class UserAgentUtil extends cn.hutool.http.useragent.UserAgentUtil {}
	/**
	 * 统一社会信用代码工具类
	 *
	 * <pre>
	 * 第一部分：登记管理部门代码1位 (数字或大写英文字母)
	 * 第二部分：机构类别代码1位 (数字或大写英文字母)
	 * 第三部分：登记管理机关行政区划码6位 (数字)
	 * 第四部分：主体标识码（组织机构代码）9位 (数字或大写英文字母)
	 * 第五部分：校验码1位 (数字或大写英文字母)
	 * </pre>
	 *
	 * @author looly
	 * @since 5.2.4
	 */
	public static class CreditCodeUtil extends cn.hutool.core.util.CreditCodeUtil {}
	/**
	 * {@link Temporal} 工具类封装
	 *
	 * @author looly
	 * @since 5.4.5
	 */
	public static class TemporalUtil extends cn.hutool.core.date.TemporalUtil {}
	/**
	 * Setting工具类<br>
	 * 提供静态方法获取配置文件
	 *
	 * @author looly
	 */
	public static class SettingUtil extends cn.hutool.setting.SettingUtil {}
	/**
	 * Statement和PreparedStatement工具类
	 *
	 * @author looly
	 * @since 4.0.10
	 */
	public static class StatementUtil extends cn.hutool.db.StatementUtil {}
	/**
	 * 基于https://github.com/vdurmont/emoji-java的Emoji表情工具类
	 * <p>
	 * emoji-java文档以及别名列表见：https://github.com/vdurmont/emoji-java
	 *
	 * @author looly
	 * @since 4.2.1
	 */
	public static class EmojiUtil extends cn.hutool.extra.emoji.EmojiUtil {}
	/**
	 * Bean工具类
	 *
	 * <p>
	 * 把一个拥有对属性进行set和get方法的类，我们就可以称之为JavaBean。
	 * </p>
	 *
	 * @author Looly
	 * @since 3.1.2
	 */
	public static class BeanUtil extends cn.hutool.core.bean.BeanUtil {}
	/**
	 * Servlet相关工具类封装
	 * 
	 * @author looly
	 * @since 3.2.0
	 */
	public static class ServletUtil extends cn.hutool.extra.servlet.ServletUtil {}
	/**
	 * java bean 校验工具类，此工具类基于validation-api（jakarta.validation-api）封装
	 *
	 * <p>在实际使用中，用户需引入validation-api的实现，如：hibernate-validator</p>
	 * <p>注意：hibernate-validator还依赖了javax.el，需自行引入。</p>
	 *
	 * @author chengqiang
	 * @since 5.5.0
	 */
	public static class ValidationUtil extends cn.hutool.extra.validation.ValidationUtil {}
	/**
	 * Excel工作簿{@link org.apache.poi.ss.usermodel.Workbook}相关工具类
	 * 
	 * @author looly
	 * @since 4.0.7
	 *
	 */
	public static class WorkbookUtil extends cn.hutool.poi.excel.WorkbookUtil {}
	/**
	 * 拼音工具类，封装了TinyPinyin、JPinyin、Pinyin4j，通过SPI自动识别。
	 *
	 * @author looly
	 */
	public static class PinyinUtil extends cn.hutool.extra.pinyin.PinyinUtil {}
	/**
	 * 调用者。可以通过此类的方法获取调用者、多级调用者以及判断是否被调用
	 * 
	 * @author Looly
	 * @since 4.1.6
	 */
	public static class CallerUtil extends cn.hutool.core.lang.caller.CallerUtil {}
	/**
	 * 定时任务表达式工具类
	 * 
	 * @author looly
	 *
	 */
	public static class CronPatternUtil extends cn.hutool.cron.pattern.CronPatternUtil {}
	/**
	 * 系统运行时工具类，用于执行系统命令的工具
	 *
	 * @author Looly
	 * @since 3.1.1
	 */
	public static class RuntimeUtil extends cn.hutool.core.util.RuntimeUtil {}
	/**
	 * {@link TemporalAccessor} 工具类封装
	 *
	 * @author looly
	 * @since 5.3.9
	 */
	public static class TemporalAccessorUtil extends cn.hutool.core.date.TemporalAccessorUtil {}
	/**
	 * 图形验证码工具
	 *
	 * @author looly
	 * @since 3.1.2
	 */
	public static class CaptchaUtil extends cn.hutool.captcha.CaptchaUtil {}
	/**
	 * 反射工具类
	 *
	 * @author Looly
	 * @since 3.0.9
	 */
	public static class ReflectUtil extends cn.hutool.core.util.ReflectUtil {}
	/**
	 * IPV4地址工具类
	 *
	 * <p>pr自：https://gitee.com/loolly/hutool/pulls/161</p>
	 *
	 * @author ZhuKun
	 * @since 5.4.1
	 */
	public static class Ipv4Util extends cn.hutool.core.net.Ipv4Util {}
	/**
	 * {@link Robot} 封装工具类，提供截屏等工具
	 * 
	 * @author looly
	 * @since 4.1.14
	 */
	public static class RobotUtil extends cn.hutool.core.swing.RobotUtil {}
	/**
	 * {@link Graphics}相关工具类
	 *
	 * @author looly
	 * @since 4.5.2
	 */
	public static class GraphicsUtil extends cn.hutool.core.img.GraphicsUtil {}
	/**
	 * Cglib工具类
	 *
	 * @author looly
	 * @since 5.4.1
	 */
	public static class CglibUtil extends cn.hutool.extra.cglib.CglibUtil {}
	/**
	 * 转义和反转义工具类Escape / Unescape<br>
	 * escape采用ISO Latin字符集对指定的字符串进行编码。<br>
	 * 所有的空格符、标点符号、特殊字符以及其他非ASCII字符都将被转化成%xx格式的字符编码(xx等于该字符在字符集表里面的编码的16进制数字)。
	 *
	 * @author xiaoleilu
	 */
	public static class EscapeUtil extends cn.hutool.core.util.EscapeUtil {}
	/**
	 * 诊断工具类
	 *
	 * @author looly
	 * @since 5.5.2
	 */
	public static class DiagnosticUtil extends cn.hutool.core.compiler.DiagnosticUtil {}
	/**
	 * Excel中的行{@link org.apache.poi.ss.usermodel.Row}封装工具类
	 *
	 * @author looly
	 * @since 4.0.7
	 */
	public static class RowUtil extends cn.hutool.poi.excel.RowUtil {}
	/**
	 * 密钥工具类
	 *
	 * <p>
	 * 包括:
	 * <pre>
	 * 1、生成密钥（单密钥、密钥对）
	 * 2、读取密钥文件
	 * </pre>
	 *
	 * @author looly, Gsealy
	 * @since 4.4.1
	 */
	public static class KeyUtil extends cn.hutool.crypto.KeyUtil {}
	/**
	 * 基于Zxing的二维码工具类
	 *
	 * @author looly
	 * @since 4.0.2
	 */
	public static class QrCodeUtil extends cn.hutool.extra.qrcode.QrCodeUtil {}
	/**
	 * SQL相关工具类，包括相关SQL语句拼接等
	 *
	 * @author looly
	 * @since 4.0.10
	 */
	public static class SqlUtil extends cn.hutool.db.sql.SqlUtil {}
	/**
	 * Excel工具类,不建议直接使用index直接操作sheet，在wps/excel中sheet显示顺序与index无关，还有隐藏sheet
	 * 
	 * @author Looly
	 *
	 */
	public static class ExcelUtil extends cn.hutool.poi.excel.ExcelUtil {}
	/**
	 * 缓存工具类
	 * @author Looly
	 *@since 3.0.1
	 */
	public static class CacheUtil extends cn.hutool.cache.CacheUtil {}
	/**
	 * Word工具类
	 * 
	 * @author Looly
	 * @since 4.5.16
	 */
	public static class WordUtil extends cn.hutool.poi.word.WordUtil {}
	/**
	 * 注解工具类<br>
	 * 快速获取注解对象、注解值等工具封装
	 *
	 * @author looly
	 * @since 4.0.9
	 */
	public static class AnnotationUtil extends cn.hutool.core.annotation.AnnotationUtil {}
	/**
	 * Excel图片工具类
	 * 
	 * @author looly
	 * @since 4.0.7
	 */
	public static class ExcelPicUtil extends cn.hutool.poi.excel.ExcelPicUtil {}
	/**
	 * 屏幕相关（当前显示设置）工具类
	 * 
	 * @author looly
	 * @since 4.1.14
	 */
	public static class ScreenUtil extends cn.hutool.core.swing.ScreenUtil {}
	/**
	 * 文件工具类
	 *
	 * @author looly
	 */
	public static class FileUtil extends cn.hutool.core.io.FileUtil {}
	/**
	 * Word Document工具
	 * 
	 * @author looly
	 * @since 4.4.1
	 */
	public static class DocUtil extends cn.hutool.poi.word.DocUtil {}
	/**
	 * 比较工具类
	 *
	 * @author looly
	 */
	public static class CompareUtil extends cn.hutool.core.comparator.CompareUtil {}
	/**
	 * Boolean类型相关工具类
	 * 
	 * @author looly
	 * @since 4.1.16
	 */
	public static class BooleanUtil extends cn.hutool.core.util.BooleanUtil {}
	/**
	 * {@link Iterable} 和 {@link Iterator} 相关工具类
	 *
	 * @author Looly
	 * @since 3.1.0
	 */
	public static class IterUtil extends cn.hutool.core.collection.IterUtil {}
	/**
	 * 树工具类
	 *
	 * @author liangbaikai
	 */
	public static class TreeUtil extends cn.hutool.core.lang.tree.TreeUtil {}
	/**
	 * 身份证相关工具类<br>
	 * see https://www.oschina.net/code/snippet_1611_2881
	 *
	 * <p>
	 * 本工具并没有对行政区划代码做校验，如有需求，请参阅（2018年10月）：
	 * http://www.mca.gov.cn/article/sj/xzqh/2018/201804-12/20181011221630.html
	 * </p>
	 *
	 * @author Looly
	 * @since 3.0.4
	 */
	public static class IdcardUtil extends cn.hutool.core.util.IdcardUtil {}
	/**
	 * 字符工具类<br>
	 * 部分工具来自于Apache Commons系列
	 *
	 * @author looly
	 * @since 4.0.1
	 */
	public static class CharUtil extends cn.hutool.core.util.CharUtil {}
	/**
	 * Socket相关工具类
	 * 
	 * @author looly
	 * @since 4.5.0
	 */
	public static class SocketUtil extends cn.hutool.socket.SocketUtil {}
	/**
	 * 手机号工具类
	 *
	 * @author dahuoyzs
	 * @since 5.3.11
	 */
	public static class PhoneUtil extends cn.hutool.core.util.PhoneUtil {}
	/**
	 * 模板工具类
	 * 
	 * @author looly
	 * @since 4.1.0
	 */
	public static class TemplateUtil extends cn.hutool.extra.template.TemplateUtil {}
	/**
	 * 集合相关工具类
	 * <p>
	 * 此工具方法针对{@link Collection}及其实现类封装的工具。
	 * <p>
	 * 由于{@link Collection} 实现了{@link Iterable}接口，因此部分工具此类不提供，而是在{@link IterUtil} 中提供
	 *
	 * @author xiaoleilu
	 * @see IterUtil
	 * @since 3.1.1
	 */
	public static class CollUtil extends cn.hutool.core.collection.CollUtil {}
	/**
	 * {@link JavaFileObject} 相关工具类封装
	 *
	 * @author lzpeng, looly
	 * @since 5.5.2
	 */
	public static class JavaFileObjectUtil extends cn.hutool.core.compiler.JavaFileObjectUtil {}
	/**
	 * 数组工具类
	 *
	 * @author Looly
	 */
	public static class ArrayUtil extends cn.hutool.core.util.ArrayUtil {}
	/**
	 * SSL(Secure Sockets Layer 安全套接字协议)相关工具封装
	 *
	 * @author looly
	 * @since 5.5.2
	 */
	public static class SSLUtil extends cn.hutool.core.net.SSLUtil {}
	/**
	 * 集合相关工具类，包括数组，是{@link CollUtil} 的别名工具类类
	 * 
	 * @author xiaoleilu
	 * @see CollUtil
	 */
	public static class CollectionUtil extends cn.hutool.core.collection.CollectionUtil {}
	/**
	 * Excel表格中单元格工具类
	 *
	 * @author looly
	 * @since 4.0.7
	 */
	public static class CellUtil extends cn.hutool.poi.excel.cell.CellUtil {}
	/**
	 * 字符集工具类
	 *
	 * @author xiaoleilu
	 */
	public static class CharsetUtil extends cn.hutool.core.util.CharsetUtil {}
	/**
	 * 字符串工具类
	 *
	 * @author xiaoleilu
	 */
	public static class StrUtil extends cn.hutool.core.util.StrUtil {}
	/**
	 * Map相关工具类
	 *
	 * @author Looly
	 * @since 3.1.1
	 */
	public static class MapUtil extends cn.hutool.core.map.MapUtil {}
	/**
	 * SPI机制中的服务加载工具类，流程如下
	 *
	 * <pre>
	 *     1、创建接口，并创建实现类
	 *     2、ClassPath/META-INF/services下创建与接口全限定类名相同的文件
	 *     3、文件内容填写实现类的全限定类名
	 * </pre>
	 * 相关介绍见：https://www.jianshu.com/p/3a3edbcd8f24
	 *
	 * @author looly
	 * @since 5.1.6
	 */
	public static class ServiceLoaderUtil extends cn.hutool.core.util.ServiceLoaderUtil {}
	/**
	 * Spring(Spring boot)工具封装，包括：
	 *
	 * <pre>
	 *     1、Spring IOC容器中的bean对象获取
	 * </pre>
	 *
	 * @author loolly
	 * @since 5.1.0
	 */
	public static class SpringUtil extends cn.hutool.extra.spring.SpringUtil {}
	/**
	 * 随机工具类
	 *
	 * @author xiaoleilu
	 */
	public static class RandomUtil extends cn.hutool.core.util.RandomUtil {}
	public static class ListUtil extends cn.hutool.core.collection.ListUtil {}
	/**
	 * Resource资源工具类
	 * 
	 * @author Looly
	 *
	 */
	public static class ResourceUtil extends cn.hutool.core.io.resource.ResourceUtil {}
	/**
	 * AWT中字体相关工具类
	 *
	 * @author looly
	 * @since 5.3.6
	 */
	public static class FontUtil extends cn.hutool.core.img.FontUtil {}
	/**
	 * CSV工具
	 *
	 * @author looly
	 * @since 4.0.5
	 */
	public static class CsvUtil extends cn.hutool.core.text.csv.CsvUtil {}
	/**
	 * Word中表格相关工具
	 * 
	 * @author Looly
	 * @since 4.5.14
	 */
	public static class TableUtil extends cn.hutool.poi.word.TableUtil {}
	/**
	 * 原始类型数组工具类
	 *
	 * @author looly
	 * @since 5.5.2
	 */
	public static class PrimitiveArrayUtil extends cn.hutool.core.util.PrimitiveArrayUtil {}
	
	/**
	 * JSON工具类
	 */
	public static class JSONUtil extends com.nis.common.utils.JSONUtil {};
	

}
