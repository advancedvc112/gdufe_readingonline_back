package com.gdufe.readingonline.controller.admin.controller;

import com.gdufe.readingonline.service.excelUploadAndPrase.ExcelUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

/**
 * Excel文件上传管理Controller
 * 
 * @author gdufe
 * @date 2024
 */
@RestController
@RequestMapping("/gdufeReadingOnline/manage")
@CrossOrigin(origins = "*") // 允许跨域访问
public class ExcelUploadController {
    
    @Autowired
    private ExcelUploadService excelUploadService;

    /**
     * 上传Excel文件接口
     * 
     * @param excelFile Excel文件
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileSource 文件来源
     * @return 上传结果
     */
    @PostMapping("/upload-excel")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize,
            @RequestParam("fileSource") String fileSource) {
        
        try {
            // 参数验证
            if (excelFile == null || excelFile.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "Excel文件不能为空");
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            if (fileName == null || fileName.trim().isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "文件名不能为空");
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            // 使用Service处理Excel上传
            Map<String, Object> result = excelUploadService.processExcelUpload(
                excelFile, fileName, fileSize, fileSource);
            
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "文件上传失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
}
