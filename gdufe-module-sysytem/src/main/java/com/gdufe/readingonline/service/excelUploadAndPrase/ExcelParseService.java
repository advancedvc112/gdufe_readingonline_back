package com.gdufe.readingonline.service.excelUploadAndPrase;

import org.springframework.web.multipart.MultipartFile;

/**
 * Excel解析和数据库导入服务接口
 * 
 * @author gdufe
 * @date 2024
 */
public interface ExcelParseService {
    
    /**
     * 解析Excel文件并导入数据库
     * 
     * @param excelFile Excel文件
     * @param fileSource 文件来源（0:畅想之星, 1:京东）
     * @return 导入结果
     */
    ParseResult parseAndImportExcel(MultipartFile excelFile, String fileSource);
    
    /**
     * 解析结果类
     */
    class ParseResult {
        private boolean success;
        private String message;
        private int totalRows;
        private int insertedRows;
        private int skippedRows;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        
        public int getInsertedRows() { return insertedRows; }
        public void setInsertedRows(int insertedRows) { this.insertedRows = insertedRows; }
        
        public int getSkippedRows() { return skippedRows; }
        public void setSkippedRows(int skippedRows) { this.skippedRows = skippedRows; }
    }
}
