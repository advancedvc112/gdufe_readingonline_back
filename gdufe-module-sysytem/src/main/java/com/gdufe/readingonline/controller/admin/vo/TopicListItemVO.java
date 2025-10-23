package com.gdufe.readingonline.controller.admin.vo;

import lombok.Data;

/**
 * 图书专题列表项VO
 * 
 * @author gdufe
 * @date 2025
 */
@Data
public class TopicListItemVO {
    
    /**
     * 专题编号
     */
    private String topicNo;
    
    /**
     * 专题HTML文本
     */
    private String htmlContent;
    
    /**
     * 发布时间
     */
    private String publishTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}

