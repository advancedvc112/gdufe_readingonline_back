package com.gdufe.readingonline.service.excelUploadAndPrase.impl;

import com.gdufe.readingonline.service.excelUploadAndPrase.ExcelUploadService;
import com.gdufe.readingonline.service.excelUploadAndPrase.ExcelParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

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
                                                  Long fileSize, String fileSource, Integer excelCategory) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证文件
            if (!validateExcelFile(excelFile)) {
                result.put("code", 400);
                result.put("message", "文件验证失败");
                result.put("finishedCount", 0);
                return result;
            }
            
            // 根据excelCategory选择不同的处理方式
            ExcelParseService.ParseResult parseResult;
            if (excelCategory == 0) {
                // 图书详情表（原有逻辑）
                System.out.println("开始处理图书详情表Excel...");
                parseResult = excelParseService.parseAndImportExcel(excelFile, fileSource);
            } else if (excelCategory == 1) {
                // 封面图详情表（新增逻辑）
                System.out.println("开始处理封面图详情表Excel...");
                parseResult = excelParseService.parseAndImportCoverImages(excelFile, fileSource);
            } else {
                result.put("code", 400);
                result.put("message", "不支持的Excel类型");
                result.put("finishedCount", 0);
                return result;
            }
            
            if (parseResult.isSuccess()) {
                result.put("code", 200);
                result.put("message", "excel解析并导入成功");
                result.put("finishedCount", parseResult.getInsertedRows());
                result.put("totalRows", parseResult.getTotalRows());
                result.put("skippedRows", parseResult.getSkippedRows());
                result.put("skippedRowsList", parseResult.getSkippedRowsList());
            } else {
                result.put("code", 500);
                result.put("message", "excel解析成功但导入失败");
                result.put("finishedCount", 0);
                result.put("totalRows", parseResult.getTotalRows());
                result.put("skippedRows", parseResult.getSkippedRows());
                result.put("skippedRowsList", parseResult.getSkippedRowsList());
            }
            
            return result;
            
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "文件处理失败：" + e.getMessage());
            result.put("finishedCount", 0);
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
