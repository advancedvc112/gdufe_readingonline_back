package com.gdufe.readingonline.controller.admin.vo;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel上传VO
 * 包含前端传入的4个参数
 * 
 * @author gdufe
 * @date 2024
 */
@Data
public class ExcelUploadVO {
    
    /**
     * Excel文件
     */
    private MultipartFile excelFile;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件来源
     */
    private String fileSource;
}
