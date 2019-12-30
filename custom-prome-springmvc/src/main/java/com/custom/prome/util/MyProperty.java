package com.custom.prome.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class MyProperty {
    private MyProperty(){}
    /**
     * 访问超时时间,用于设置URL超时时间阈值
     */
    public static final Integer TIMEOUT_SECOND = 2;
    /**
     * timeOfEveryRequest内存刷新时间，默认30秒刷新一次
     */
    public static final Integer FLASH_TIME = 30;

    /**
     * Object中的方法
     */
    public static List<String> objMethods = Arrays.asList("wait","equals","toString","hashCode","getClass","notify","notifyAll");

    /**
     * 拦截器排除资源后缀
     */
    public static String[]  resourcesSuffix =
            (String[])Arrays.asList("*.jpg","*.gif","*.png","*.svg","*.js","*.css", "*.html","*.htm","*.xml",
                    "*.swf","*.cab","*.xlsx","*.pdf","*.doc","*.docx","*.txt","*.JPG","*.GIF","*.PNG",
                    "*.SVG","*.JS","*.CSS","*.HTML","*.HTM","*.XML","*.SWF","*.CAB","*.XLSX","*.PDF",
                    "*.DOC","*.DOCX","*.TXT","*.ico","*.ICO","*.jsp","*.JSP").toArray();

}
