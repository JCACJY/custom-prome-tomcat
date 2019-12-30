package com.custom.prome.mbean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 该类将暴露为MBean，用于监控RequestCountInterceptor中的数据
 */
public class RequestCountInterceptorMBean {
    /**
     * 已经处理好的每个URL请求的平均时间
     */
    public static Map<String,Double> avgTime = new HashMap<>();
    /**
     * 每个URL访问的次数--需要通过MBean暴露出去---存在并发操作,需要使用AtomicLong
     */
    public static Map<String, AtomicLong> countOfEveryRequest = new ConcurrentHashMap<>();
    /**
     * 每个URL请求错误次数--需要通过MBean暴露出去---存在并发操作,需要使用AtomicLong
     */
    public static Map<String, AtomicLong> errorCount = new ConcurrentHashMap<>();
    /**
     * 以下方法用于MBean暴露出去，只暴露getter方法
     */
    public Map<String, Double> getAvgTime() {
        return avgTime;
    }
    public Map<String, Long> getCountOfEveryRequest() {
        Map<String,Long> count4MBean = new HashMap<>();
        for (Map.Entry<String, AtomicLong>  dataEntry: countOfEveryRequest.entrySet()) {
            count4MBean.put(dataEntry.getKey(), dataEntry.getValue().longValue());
        }
        return count4MBean;
    }
    public Map<String, Long> getErrorCount() {
        Map<String,Long> count4MBean = new HashMap<>();
        for (Map.Entry<String, AtomicLong>  dataEntry: errorCount.entrySet()) {
            count4MBean.put(dataEntry.getKey(), dataEntry.getValue().longValue());
        }
        return count4MBean;
    }
    public void resetCountOfEveryRequest() {
        for (Map.Entry<String,AtomicLong> dataEntry: countOfEveryRequest.entrySet()) {
            countOfEveryRequest.put(dataEntry.getKey(), new AtomicLong(0L));
        }
    }

}
