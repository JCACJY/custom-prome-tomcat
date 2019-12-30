package com.custom.prome.config;


import com.custom.prome.interceptor.RequestCountInterceptor;
import com.custom.prome.mbean.RequestCountInterceptorMBean;
import com.custom.prome.util.MyProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.assembler.MethodNameBasedMBeanInfoAssembler;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableWebMvc
@Configuration
public class JmxJavaConfig extends WebMvcConfigurerAdapter {

    @Bean
    public RequestCountInterceptor requestCountInterceptor(){
        return new RequestCountInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InterceptorRegistration interceptorRegistration = registry.addInterceptor(requestCountInterceptor());
        interceptorRegistration.addPathPatterns("/**");
        interceptorRegistration.excludePathPatterns(MyProperty.resourcesSuffix);
    }

    @Bean
    public RequestCountInterceptorMBean requestCountInterceptorMBean(){
        return new RequestCountInterceptorMBean();
    }

    @Bean
    public MethodNameBasedMBeanInfoAssembler assembler() {
        MethodNameBasedMBeanInfoAssembler assembler = new MethodNameBasedMBeanInfoAssembler();
        RequestCountInterceptorMBean requestCountInterceptorMBean = requestCountInterceptorMBean();
        Class clazz = requestCountInterceptorMBean.getClass();
        Method[] methodz = clazz.getMethods();
        List<String> methodNames = new ArrayList<>();
        for (int i = 0; i < methodz.length; i++) {
            String methodName = methodz[i].getName();
            if(!MyProperty.objMethods.contains(methodName)){
                methodNames.add(methodName);
            }
        }
        String[] names = new String[methodNames.size()];
        for (int i = 0; i < methodNames.size(); i++) {
            names[i] = methodNames.get(i);
        }
        assembler.setManagedMethods(names);
        return assembler;
    }
    @Bean
    public MBeanExporter mbeanExporter() {
        MBeanExporter exporter = new MBeanExporter();
        Map<String, Object> beans = new HashMap<>();
        beans.put("Custom-PromeInterceptor:type=httprequest,name=urlstatistics", requestCountInterceptorMBean());
        exporter.setBeans(beans);
        exporter.setAssembler(assembler());
        return exporter;
    }
}
