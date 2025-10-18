package com.gdufe.readingonline.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

/**
 * 文件上传配置类
 * 
 * @author gdufe
 * @date 2024
 */
@Configuration
public class FileUploadConfig {
    
    /**
     * 配置文件上传参数
     * 设置更大的文件上传限制
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        
        // 设置单个文件最大大小
        factory.setMaxFileSize(DataSize.ofMegabytes(200));
        
        // 设置整个请求最大大小
        factory.setMaxRequestSize(DataSize.ofMegabytes(200));
        
        // 设置文件写入磁盘的阈值 - 设置为0表示所有文件都保存在内存中，不写入临时文件
        factory.setFileSizeThreshold(DataSize.ofBytes(0));
        
        // 不设置临时文件存储位置，让系统使用内存处理
        
        return factory.createMultipartConfig();
    }
}
