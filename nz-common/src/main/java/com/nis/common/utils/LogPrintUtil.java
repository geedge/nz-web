package com.nis.common.utils;

import cn.hutool.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;


public class LogPrintUtil {
    static Log logger= Log.get();
    public static String print(Throwable t){
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}

