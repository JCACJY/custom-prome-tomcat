package com.custom.prome.interceptor;

import com.custom.prome.mbean.RequestCountInterceptorMBean;
import com.custom.prome.util.MyProperty;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @Date 2019-11-30
 * @author create by JC
 * @implNote MVC拦截器，用于统计每个URL的访问次数、访问失败次数、30秒内平均响应时间等
 */
public class RequestCountInterceptor implements HandlerInterceptor {
    /**
     * 任务睡眠时间,建议睡眠2秒钟
     */
    private static final int TASK_SlEEP_TIME = 2;
    /**
     * 每个URL访问的时间，分为A孩B孩两个容器
     */
    private static final Map<String, ConcurrentLinkedQueue<Long>> timePoolOfEveryRequest_A = new ConcurrentHashMap<>() ;
    private static final Map<String, ConcurrentLinkedQueue<Long>> timePoolOfEveryRequest_B = new ConcurrentHashMap<>() ;
    /**
     * 获取指挥员实例（单例）
     */
    private static final PoolExchanger poolExchanger = PoolExchanger.getInstance();
    private static final ThreadLocal<Long> requestBeanThreadLocal = new ThreadLocal<>();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if(RequestCountInterceptorMBean.countOfEveryRequest.get(request.getRequestURI()) == null){
            RequestCountInterceptorMBean.countOfEveryRequest.put(request.getRequestURI(), new AtomicLong(0L));
        }
        requestBeanThreadLocal.set(System.currentTimeMillis());
        return true;
    }
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Map<String, ConcurrentLinkedQueue<Long>> timePoolOfEveryRequest = poolExchanger.getTimePoolOfEveryRequest();
        ConcurrentLinkedQueue<Long> timePool = timePoolOfEveryRequest.get(request.getRequestURI());
        //如果还没有的话，先放一个进去(池子存在替换问题，所以不能在preHandle()中先放入，否则postHandle()可能取到空值)
        if(timePool == null){
            timePool = new ConcurrentLinkedQueue<>();
            timePoolOfEveryRequest.put(request.getRequestURI(), timePool);
        }
        //将URL访问消耗时间放进去
        timePool.add(System.currentTimeMillis() - requestBeanThreadLocal.get());
        //业务流程处理结束后会执行该方法，在这里请求数加一
        AtomicLong count_atomic = RequestCountInterceptorMBean.countOfEveryRequest.get(request.getRequestURI());
        count_atomic.incrementAndGet();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //统计错误请求
        if(ex != null){
            String requestURI = request.getRequestURI();
            if(RequestCountInterceptorMBean.errorCount.get(requestURI) == null){
                RequestCountInterceptorMBean.errorCount.put(requestURI, new AtomicLong(1L));
                return;
            }
            RequestCountInterceptorMBean.errorCount.get(requestURI).incrementAndGet();
        }
    }
    public static PoolExchanger getPoolExchanger(){
        return RequestCountInterceptor.poolExchanger;
    }
    public static ThreadLocal getRequestBeanThreadLocal(){
        return RequestCountInterceptor.requestBeanThreadLocal;
    }
    /**
     * 定时指挥员类，专为内部使用
     */
    public static class PoolExchanger{
        //使用 A/B池 的指挥员
        private volatile boolean flagMan = false;
        //切换 A/B池 的定时器
        private static ScheduledExecutorService exchangerExecutor = null;
        private static PoolExchanger poolExchanger = new PoolExchanger();
        public static PoolExchanger getInstance(){return PoolExchanger.poolExchanger;}
        private PoolExchanger(){
            exchangerExecutor = Executors.newSingleThreadScheduledExecutor();
            exchangerExecutor.scheduleAtFixedRate(() -> {
                //long startTime = System.currentTimeMillis();
                Map<String, ConcurrentLinkedQueue<Long>> timePoolOfEveryRequest_notUse;
                //System.out.println("定时器执行，当前获取："+(flagMan?"A池":"B池"));
                //先翻转状态，表示立即停止正在使用的池，很重要！
                if(flagMan){
                    flagMan = false;
                }else {
                    flagMan = true;
                }
                //这里睡眠2秒钟，避免有部分线程没有完成ConcurrentLinkedQueue.add()操作
                try {
                    TimeUnit.SECONDS.sleep(RequestCountInterceptor.TASK_SlEEP_TIME);
                    timePoolOfEveryRequest_notUse = getTimePoolOfEveryRequest(!flagMan);
                    //开始清算停用的池子中的数据
                    handleData(timePoolOfEveryRequest_notUse);
                    //清算完数据后，把所有数据清掉
                    timePoolOfEveryRequest_notUse.clear();
                } catch (Exception e) { throw new RuntimeException("数据统计异常！"+e); }
                //long costTime = System.currentTimeMillis() - startTime - 2000;
                //System.out.println("数据统计结束，耗时："+costTime+"ms");
            }, 0, MyProperty.FLASH_TIME, TimeUnit.SECONDS);
        }
        public Map<String, ConcurrentLinkedQueue<Long>> getTimePoolOfEveryRequest(){
            //System.out.println("当前状态："+(flagMan?"A池":"B池")+"生效，"+(!flagMan?"A池":"B池")+"空闲");
            return flagMan?timePoolOfEveryRequest_A:timePoolOfEveryRequest_B;
        }
        public ScheduledExecutorService getExchangerExecutor(){return PoolExchanger.exchangerExecutor;}
        private Map<String, ConcurrentLinkedQueue<Long>> getTimePoolOfEveryRequest(boolean flagMan){
            //System.out.println("当前状态："+(flagMan?"A池":"B池")+"生效，"+(!flagMan?"A池":"B池")+"空闲");
            return flagMan?timePoolOfEveryRequest_A:timePoolOfEveryRequest_B;
        }
        /**
         * 统计监控数据方法，这里的逻辑是统计每个URL30秒内的平均响应时间
         */
        private void handleData(Map<String, ConcurrentLinkedQueue<Long>> timePoolOfEveryRequest){
            Long dataSum = 0L;
            //先清空上一次的数据,再写入新数据
            RequestCountInterceptorMBean.avgTime.clear();
            for (Map.Entry<String,ConcurrentLinkedQueue<Long>>  dataEntry: timePoolOfEveryRequest.entrySet()) {
                ConcurrentLinkedQueue<Long> dataList = dataEntry.getValue();
                for (Long data: dataList) {
                    dataSum += data;
                }
                //一个URL请求的平均时间
                double avgTime =  dataSum/dataList.size();
                RequestCountInterceptorMBean.avgTime.put(dataEntry.getKey(), avgTime);
            }
        }
    }

}
