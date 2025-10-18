package com.gdufe.readingonline.service.impl;

import com.gdufe.readingonline.service.ExcelParseService;
import com.gdufe.readingonline.service.ExcelUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Excel文件处理服务实现类
 * 
 * @author gdufe
 * @date 2024
 */
@Service
public class ExcelUploadServiceImpl implements ExcelUploadService {
    
    @Autowired
    private ExcelParseService excelParseService;
    
    @Override
    public Map<String, Object> processExcelUpload(MultipartFile excelFile, String fileName, 
                                                  Long fileSize, String fileSource) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证文件
            if (!validateExcelFile(excelFile)) {
                result.put("success", false);
                result.put("message", "文件验证失败");
                return result;
            }
            
            // 解析Excel文件并导入数据库
            ExcelParseService.ParseResult parseResult = excelParseService.parseAndImportExcel(excelFile, fileSource);
            
            if (parseResult.isSuccess()) {
                result.put("success", true);
                result.put("message", parseResult.getMessage());
                result.put("fileId", UUID.randomUUID().toString());
                result.put("fileName", fileName);
                result.put("fileSize", fileSize);
                result.put("fileSource", fileSource);
                result.put("uploadTime", LocalDateTime.now());
                result.put("totalRows", parseResult.getTotalRows());
                result.put("insertedRows", parseResult.getInsertedRows());
                result.put("skippedRows", parseResult.getSkippedRows());
            } else {
                result.put("success", false);
                result.put("message", parseResult.getMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "文件处理失败：" + e.getMessage());
            return result;
        }
    }
    
    @Override
    public boolean validateExcelFile(MultipartFile excelFile) {
        if (excelFile == null || excelFile.isEmpty()) {
            return false;
        }
        
        // 检查文件类型
        String originalFilename = excelFile.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }
        
        String lowerCaseFilename = originalFilename.toLowerCase();
        if (!lowerCaseFilename.endsWith(".xlsx") && !lowerCaseFilename.endsWith(".xls")) {
            return false;
        }
        
        // 检查Content-Type
        String contentType = excelFile.getContentType();
        if (contentType == null || 
            (!contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") &&
             !contentType.equals("application/vnd.ms-excel"))) {
            return false;
        }
        
        return true;
    }
}
