package com.custom.prome.listener;



import com.custom.prome.interceptor.RequestCountInterceptor;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.ScheduledExecutorService;

@WebListener
public class StopTheWorldListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) { }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        //释放定时器资源和ThreadLocal资源
        ScheduledExecutorService exchangerExecutor = RequestCountInterceptor.getPoolExchanger().getExchangerExecutor();
        if(!exchangerExecutor.isShutdown()){
            exchangerExecutor.shutdown();
        }
        RequestCountInterceptor.getRequestBeanThreadLocal().remove();
        System.out.println("tomcat停止，关闭定时器资源！");
    }
}
