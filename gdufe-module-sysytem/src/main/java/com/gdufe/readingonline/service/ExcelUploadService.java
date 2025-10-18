package com.gdufe.readingonline.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 * Excel文件处理服务接口
 * 
 * @author gdufe
 * @date 2024
 */
public interface ExcelUploadService {
    
    /**
     * 处理Excel文件上传并解析导入数据库
     * 
     * @param excelFile Excel文件
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileSource 文件来源
     * @return 处理结果
     */
    Map<String, Object> processExcelUpload(MultipartFile excelFile, String fileName, 
                                          Long fileSize, String fileSource);
    
    /**
     * 验证Excel文件
     * 
     * @param excelFile Excel文件
     * @return 验证结果
     */
    boolean validateExcelFile(MultipartFile excelFile);
}
