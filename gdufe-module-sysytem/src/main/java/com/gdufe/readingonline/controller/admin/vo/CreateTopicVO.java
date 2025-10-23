package com.gdufe.readingonline.controller.admin.vo;

import lombok.Data;

/**
 * 创建图书专题VO
 * 
 * @author gdufe
 * @date 2025
 */
@Data
public class CreateTopicVO {
    
    /**
     * 专题html文本
     */
    private String html;
    
    /**
     * 创建人
     */
    private String creator;
}

