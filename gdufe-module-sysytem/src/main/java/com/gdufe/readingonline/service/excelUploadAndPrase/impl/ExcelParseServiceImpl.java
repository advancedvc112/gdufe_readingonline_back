package com.gdufe.readingonline.service.excelUploadAndPrase.impl;

import com.gdufe.readingonline.service.excelUploadAndPrase.ExcelParseService;
import com.gdufe.readingonline.dal.dataobject.GdufeLibraryEbookDO;
import com.gdufe.readingonline.dal.mysqlmapper.GdufeLibraryEbookMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel解析和数据库导入服务实现类
 * 
 * @author gdufe
 * @date 2024
 */
@Service
public class ExcelParseServiceImpl implements ExcelParseService {
    
    @Autowired
    private GdufeLibraryEbookMapper ebookMapper;
    
    /**
     * 解析Excel文件并导入数据库
     * 
     * @param excelFile Excel文件
     * @param fileSource 文件来源（0:畅想之星, 1:京东）
     * @return 导入结果
     */
    public ParseResult parseAndImportExcel(MultipartFile excelFile, String fileSource) {
        ParseResult result = new ParseResult();
        
        try {
            // 根据文件来源选择不同的解析方法
            List<GdufeLibraryEbookDO> ebookList;
            if ("0".equals(fileSource) || "changxiang".equalsIgnoreCase(fileSource)) {
                // 畅想之星
                ebookList = parseChangxiangExcelFile(excelFile);
            } else if ("1".equals(fileSource) || "jingdong".equalsIgnoreCase(fileSource)) {
                // 京东
                ebookList = parseJingdongExcelFile(excelFile);
            } else {
                result.setSuccess(false);
                result.setMessage("不支持的文件来源：" + fileSource);
                return result;
            }
            
            if (ebookList == null || ebookList.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Excel文件中没有有效数据");
                return result;
            }
            
            // 批量插入数据库
            int insertedCount = 0;
            int skippedCount = 0;
            
            // 设置公共字段
            LocalDateTime now = LocalDateTime.now();
            for (GdufeLibraryEbookDO ebook : ebookList) {
                ebook.setCreateTime(now);
                ebook.setUpdateTime(now);
                ebook.setIsDeleted(0); // 默认未删除
            }
            
            // 分批处理，每批1000条记录
            int batchSize = 1000;
            int totalSize = ebookList.size();
            
            for (int i = 0; i < totalSize; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalSize);
                List<GdufeLibraryEbookDO> batch = ebookList.subList(i, endIndex);
                
                try {
                    // 使用批量插入或更新（基于ISBN）
                    int batchProcessed = ebookMapper.batchInsertOrUpdate(batch);
                    insertedCount += batchProcessed;
                    
                    System.out.println("批量处理第 " + (i/batchSize + 1) + " 批，处理 " + batchProcessed + " 条记录（插入或更新）");
                    
                } catch (Exception e) {
                    System.err.println("批量插入或更新第 " + (i/batchSize + 1) + " 批失败：" + e.getMessage());
                    
                    // 如果批量操作失败，尝试逐条插入或更新
                    for (GdufeLibraryEbookDO ebook : batch) {
                        try {
                            // 先查询是否存在
                            GdufeLibraryEbookDO existingEbook = ebookMapper.selectByBookIsbn(ebook.getBookIsbn());
                            if (existingEbook != null) {
                                // 存在则更新
                                ebook.setId(existingEbook.getId());
                                ebook.setCreateTime(existingEbook.getCreateTime()); // 保持原创建时间
                                ebookMapper.updateById(ebook);
                                System.out.println("更新记录：ISBN=" + ebook.getBookIsbn() + ", 书名=" + ebook.getBookName());
                            } else {
                                // 不存在则插入
                                ebookMapper.insert(ebook);
                                System.out.println("插入记录：ISBN=" + ebook.getBookIsbn() + ", 书名=" + ebook.getBookName());
                            }
                            insertedCount++;
                        } catch (Exception ex) {
                            System.err.println("单条处理失败：ISBN=" + ebook.getBookIsbn() + ", 错误=" + ex.getMessage());
                            skippedCount++;
                        }
                    }
                }
            }
            
            result.setSuccess(true);
            result.setMessage("Excel文件解析并导入完成（基于ISBN进行插入或更新）");
            result.setTotalRows(ebookList.size());
            result.setInsertedRows(insertedCount);
            result.setSkippedRows(skippedCount);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Excel文件解析失败：" + e.getMessage());
        }
        
        return result;
    }
    
    
    /**
     * 解析畅想之星Excel文件
     * 
     * @param excelFile Excel文件
     * @return 电子书数据列表
     */
    private List<GdufeLibraryEbookDO> parseChangxiangExcelFile(MultipartFile excelFile) {
        List<GdufeLibraryEbookDO> ebookList = new ArrayList<>();
        
        try {
            Workbook workbook;
            String fileName = excelFile.getOriginalFilename();
            
            if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(excelFile.getInputStream());
            } else {
                workbook = new HSSFWorkbook(excelFile.getInputStream());
            }
            
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            
            // 解析标题行，获取列索引映射
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel文件第一行（标题行）为空");
            }
            
            Map<String, Integer> columnIndexMap = parseChangxiangHeaderRow(headerRow);
            
            // 从第二行开始解析数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    GdufeLibraryEbookDO ebook = parseChangxiangRow(row, columnIndexMap);
                    if (ebook != null) {
                        ebookList.add(ebook);
                    }
                } catch (Exception e) {
                    System.err.println("解析畅想之星行数据失败：" + e.getMessage());
                    continue;
                }
            }
            
            workbook.close();
            
        } catch (IOException e) {
            System.err.println("读取Excel文件失败：" + e.getMessage());
        }
        
        return ebookList;
    }
    
    /**
     * 解析畅想之星Excel标题行，获取列索引映射
     * 
     * @param headerRow 标题行
     * @return 列名到列索引的映射
     */
    private Map<String, Integer> parseChangxiangHeaderRow(Row headerRow) {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        
        // 遍历标题行的所有单元格
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            
            String headerValue = getCellStringValue(cell).trim();
            if (headerValue.isEmpty()) continue;
            
            // 根据畅想之星固定的字段名进行匹配
            if (headerValue.equals("主题名")) {
                columnIndexMap.put("bookName", i);
            } else if (headerValue.equals("第一责任人")) {
                columnIndexMap.put("author", i);
            } else if (headerValue.equals("第二责任人")) {
                columnIndexMap.put("secondAuthor", i);
            } else if (headerValue.equals("出版社")) {
                columnIndexMap.put("publisher", i);
            } else if (headerValue.equals("ISBN")) {
                columnIndexMap.put("isbn", i);
            } else if (headerValue.equals("中图法分类")) {
                columnIndexMap.put("category", i);
            } else if (headerValue.equals("学科分类")) {
                columnIndexMap.put("subjectCategory", i);
            } else if (headerValue.equals("简介")) {
                columnIndexMap.put("description", i);
            } else if (headerValue.equals("URL")) {
                columnIndexMap.put("ebookFileUrl", i);
            } else if (headerValue.equals("出版时间")) {
                columnIndexMap.put("publishDate", i);
            }
        }
        
        // 验证必要的列是否存在
        if (!columnIndexMap.containsKey("bookName")) {
            throw new RuntimeException("畅想之星Excel文件中缺少书名列，请检查标题行");
        }
        
        return columnIndexMap;
    }
    
    /**
     * 解析畅想之星Excel行数据为电子书对象
     * 
     * @param row Excel行数据
     * @param columnIndexMap 列索引映射
     * @return 电子书对象
     */
    private GdufeLibraryEbookDO parseChangxiangRow(Row row, Map<String, Integer> columnIndexMap) {
        GdufeLibraryEbookDO ebook = new GdufeLibraryEbookDO();
        
        try {
            // 使用动态列索引读取数据
            
            // 书名（必填）
            Integer bookNameIndex = columnIndexMap.get("bookName");
            if (bookNameIndex != null) {
                Cell bookNameCell = row.getCell(bookNameIndex);
                String bookName = getCellStringValue(bookNameCell);
                if (bookName != null && !bookName.trim().isEmpty()) {
                    ebook.setBookName(bookName.trim());
                } else {
                    // 书名为空，跳过该行
                    return null;
                }
            } else {
                throw new RuntimeException("找不到书名列");
            }
            
            // 第一责任人（作者）
            Integer authorIndex = columnIndexMap.get("author");
            if (authorIndex != null) {
                Cell authorCell = row.getCell(authorIndex);
                String author = getCellStringValue(authorCell);
                if (author != null && !author.trim().isEmpty()) {
                    ebook.setBookAuthor(author.trim());
                }
            }
            
            // 第二责任人
            Integer secondAuthorIndex = columnIndexMap.get("secondAuthor");
            if (secondAuthorIndex != null) {
                Cell secondAuthorCell = row.getCell(secondAuthorIndex);
                String secondAuthor = getCellStringValue(secondAuthorCell);
                if (secondAuthor != null && !secondAuthor.trim().isEmpty()) {
                    // 如果已有第一责任人，则合并；否则设置为作者
                    String currentAuthor = ebook.getBookAuthor();
                    if (currentAuthor != null && !currentAuthor.trim().isEmpty()) {
                        ebook.setBookAuthor(currentAuthor + ";" + secondAuthor.trim());
                    } else {
                        ebook.setBookAuthor(secondAuthor.trim());
                    }
                }
            }
            
            // 出版社
            Integer publisherIndex = columnIndexMap.get("publisher");
            if (publisherIndex != null) {
                Cell publisherCell = row.getCell(publisherIndex);
                String publisher = getCellStringValue(publisherCell);
                if (publisher != null && !publisher.trim().isEmpty()) {
                    ebook.setBookPress(publisher.trim());
                }
            }
            
            // ISBN
            Integer isbnIndex = columnIndexMap.get("isbn");
            if (isbnIndex != null) {
                Cell isbnCell = row.getCell(isbnIndex);
                String isbn = getCellStringValue(isbnCell);
                if (isbn != null && !isbn.trim().isEmpty()) {
                    ebook.setBookIsbn(isbn.trim());
                }
            }
            
            // 分类
            Integer categoryIndex = columnIndexMap.get("category");
            if (categoryIndex != null) {
                Cell categoryCell = row.getCell(categoryIndex);
                String category = getCellStringValue(categoryCell);
                if (category != null && !category.trim().isEmpty()) {
                    ebook.setBookChineseLibraryClassification(category.trim());
                }
            }
            
            // 描述
            Integer descriptionIndex = columnIndexMap.get("description");
            if (descriptionIndex != null) {
                Cell descriptionCell = row.getCell(descriptionIndex);
                String description = getCellStringValue(descriptionCell);
                if (description != null && !description.trim().isEmpty()) {
                    ebook.setBookBriefIntroduction(description.trim());
                }
            }
            
            // 电子书文件URL
            Integer ebookFileIndex = columnIndexMap.get("ebookFileUrl");
            if (ebookFileIndex != null) {
                Cell ebookFileUrlCell = row.getCell(ebookFileIndex);
                String ebookFileUrl = getCellStringValue(ebookFileUrlCell);
                if (ebookFileUrl != null && !ebookFileUrl.trim().isEmpty()) {
                    ebook.setBookUrl(ebookFileUrl.trim());
                }
            }
            
            // 学科分类
            Integer subjectCategoryIndex = columnIndexMap.get("subjectCategory");
            if (subjectCategoryIndex != null) {
                Cell subjectCategoryCell = row.getCell(subjectCategoryIndex);
                String subjectCategory = getCellStringValue(subjectCategoryCell);
                if (subjectCategory != null && !subjectCategory.trim().isEmpty()) {
                    ebook.setBookSubjectClassification(subjectCategory.trim());
                }
            }
            
            // 出版日期
            Integer publishDateIndex = columnIndexMap.get("publishDate");
            if (publishDateIndex != null) {
                Cell publishDateCell = row.getCell(publishDateIndex);
                if (publishDateCell != null) {
                    try {
                        if (publishDateCell.getCellType() == CellType.NUMERIC) {
                            if (DateUtil.isCellDateFormatted(publishDateCell)) {
                                ebook.setBookPublicationTime(publishDateCell.getDateCellValue().toInstant()
                                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
                            }
                        } else {
                            String dateStr = getCellStringValue(publishDateCell);
                            if (dateStr != null && !dateStr.trim().isEmpty()) {
                                // 尝试解析日期字符串
                                try {
                                    ebook.setBookPublicationTime(LocalDate.parse(dateStr.trim()));
                                } catch (Exception e) {
                                    // 忽略日期解析错误
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略日期解析错误
                    }
                }
            }
            
            
            // 设置来源
            ebook.setBookSource(0); // 畅想之星
            
            return ebook;
            
        } catch (Exception e) {
            System.err.println("解析行数据异常：" + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字类型，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }
    
    /**
     * 获取单元格数值
     */
    private double getCellNumericValue(Cell cell) {
        if (cell == null) {
            return 0.0;
        }
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            case BOOLEAN:
                return cell.getBooleanCellValue() ? 1.0 : 0.0;
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }
    
    /**
     * 解析京东Excel文件
     * 京东Excel格式：书名、ISBN、著者、出版社、一级分类、二级分类、出版时间、URL链接、简介
     * 
     * @param excelFile Excel文件
     * @return 解析后的电子书列表
     * @throws IOException IO异常
     */
    private List<GdufeLibraryEbookDO> parseJingdongExcelFile(MultipartFile excelFile) throws IOException {
        List<GdufeLibraryEbookDO> ebookList = new ArrayList<>();
        
        Workbook workbook = null;
        try {
            // 根据文件扩展名选择工作簿类型
            String fileName = excelFile.getOriginalFilename();
            if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(excelFile.getInputStream());
            } else {
                workbook = new HSSFWorkbook(excelFile.getInputStream());
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return ebookList;
            }
            
            // 解析标题行，获取列索引映射
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IOException("Excel文件缺少标题行");
            }
            
            Map<String, Integer> columnIndexMap = parseJingdongHeaderRow(headerRow);
            
            // 从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                GdufeLibraryEbookDO ebook = parseJingdongRow(row, columnIndexMap);
                if (ebook != null) {
                    ebookList.add(ebook);
                }
            }
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        
        return ebookList;
    }
    
    /**
     * 解析京东Excel标题行，获取列索引映射
     * 只匹配固定的字段名：书名、ISBN、著者、出版社、一级分类、二级分类、出版时间、URL链接、简介
     * 
     * @param headerRow 标题行
     * @return 列名到列索引的映射
     * @throws IOException 如果缺少必要的列
     */
    private Map<String, Integer> parseJingdongHeaderRow(Row headerRow) throws IOException {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        
        // 遍历标题行的所有单元格
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = getCellStringValue(cell).trim();
                
                // 只匹配固定的字段名
                if (headerValue.equals("书名")) {
                    columnIndexMap.put("bookName", i);
                } else if (headerValue.equals("ISBN")) {
                    columnIndexMap.put("isbn", i);
                } else if (headerValue.equals("著者")) {
                    columnIndexMap.put("author", i);
                } else if (headerValue.equals("出版社")) {
                    columnIndexMap.put("publisher", i);
                } else if (headerValue.equals("一级分类")) {
                    columnIndexMap.put("primaryCategory", i);
                } else if (headerValue.equals("二级分类")) {
                    columnIndexMap.put("secondaryCategory", i);
                } else if (headerValue.equals("出版时间")) {
                    columnIndexMap.put("publishDate", i);
                } else if (headerValue.equals("URL链接")) {
                    columnIndexMap.put("url", i);
                } else if (headerValue.equals("简介")) {
                    columnIndexMap.put("description", i);
                }
            }
        }
        
        // 验证必要的列是否存在
        if (!columnIndexMap.containsKey("bookName")) {
            throw new IOException("Excel文件缺少必要的列：书名");
        }
        
        return columnIndexMap;
    }
    
    /**
     * 解析京东Excel行数据
     * 根据列索引映射动态读取数据
     * 
     * @param row Excel行
     * @param columnIndexMap 列索引映射
     * @return 电子书对象
     */
    private GdufeLibraryEbookDO parseJingdongRow(Row row, Map<String, Integer> columnIndexMap) {
        GdufeLibraryEbookDO ebook = new GdufeLibraryEbookDO();
        
        try {
            // 书名 - 必填字段
            Integer bookNameIndex = columnIndexMap.get("bookName");
            if (bookNameIndex != null) {
                Cell bookNameCell = row.getCell(bookNameIndex);
                if (bookNameCell != null) {
                    String bookName = getCellStringValue(bookNameCell).trim();
                    if (!bookName.isEmpty()) {
                        ebook.setBookName(bookName);
                    } else {
                        // 书名为空，跳过此行
                        return null;
                    }
                } else {
                    // 书名为空，跳过此行
                    return null;
                }
            } else {
                // 缺少书名列，跳过此行
                return null;
            }
            
            // ISBN
            Integer isbnIndex = columnIndexMap.get("isbn");
            if (isbnIndex != null) {
                Cell isbnCell = row.getCell(isbnIndex);
                if (isbnCell != null) {
                    ebook.setBookIsbn(getCellStringValue(isbnCell).trim());
                }
            }
            
            // 著者
            Integer authorIndex = columnIndexMap.get("author");
            if (authorIndex != null) {
                Cell authorCell = row.getCell(authorIndex);
                if (authorCell != null) {
                    ebook.setBookAuthor(getCellStringValue(authorCell).trim());
                }
            }
            
            // 出版社
            Integer publisherIndex = columnIndexMap.get("publisher");
            if (publisherIndex != null) {
                Cell publisherCell = row.getCell(publisherIndex);
                if (publisherCell != null) {
                    ebook.setBookPress(getCellStringValue(publisherCell).trim());
                }
            }
            
            // 一级分类
            Integer primaryCategoryIndex = columnIndexMap.get("primaryCategory");
            if (primaryCategoryIndex != null) {
                Cell primaryCategoryCell = row.getCell(primaryCategoryIndex);
                if (primaryCategoryCell != null) {
                    ebook.setBookPrimaryClassification(getCellStringValue(primaryCategoryCell).trim());
                }
            }
            
            // 二级分类
            Integer secondaryCategoryIndex = columnIndexMap.get("secondaryCategory");
            if (secondaryCategoryIndex != null) {
                Cell secondaryCategoryCell = row.getCell(secondaryCategoryIndex);
                if (secondaryCategoryCell != null) {
                    ebook.setBookSecondaryClassification(getCellStringValue(secondaryCategoryCell).trim());
                }
            }
            
            // 出版时间
            Integer publishDateIndex = columnIndexMap.get("publishDate");
            if (publishDateIndex != null) {
                Cell publishDateCell = row.getCell(publishDateIndex);
                if (publishDateCell != null) {
                    try {
                        if (publishDateCell.getCellType() == CellType.NUMERIC) {
                            if (DateUtil.isCellDateFormatted(publishDateCell)) {
                                ebook.setBookPublicationTime(publishDateCell.getDateCellValue().toInstant()
                                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
                            }
                        } else if (publishDateCell.getCellType() == CellType.STRING) {
                            // 尝试解析字符串格式的日期
                            String dateStr = publishDateCell.getStringCellValue().trim();
                            if (!dateStr.isEmpty()) {
                                try {
                                    // 支持多种日期格式
                                    if (dateStr.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                                        ebook.setBookPublicationTime(LocalDate.parse(dateStr));
                                    } else if (dateStr.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
                                        ebook.setBookPublicationTime(LocalDate.parse(dateStr.replace("/", "-")));
                                    } else if (dateStr.matches("\\d{4}年\\d{1,2}月\\d{1,2}日")) {
                                        String cleanDate = dateStr.replace("年", "-").replace("月", "-").replace("日", "");
                                        ebook.setBookPublicationTime(LocalDate.parse(cleanDate));
                                    }
                                } catch (Exception e) {
                                    // 忽略日期解析错误
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略日期解析错误
                    }
                }
            }
            
            // URL链接
            Integer urlIndex = columnIndexMap.get("url");
            if (urlIndex != null) {
                Cell urlCell = row.getCell(urlIndex);
                if (urlCell != null) {
                    ebook.setBookUrl(getCellStringValue(urlCell).trim());
                }
            }
            
            // 简介
            Integer descriptionIndex = columnIndexMap.get("description");
            if (descriptionIndex != null) {
                Cell descriptionCell = row.getCell(descriptionIndex);
                if (descriptionCell != null) {
                    ebook.setBookBriefIntroduction(getCellStringValue(descriptionCell).trim());
                }
            }
            
            // 设置京东来源
            ebook.setBookSource(1); // 京东
            
            // 验证必填字段
            if (ebook.getBookName() == null || ebook.getBookName().trim().isEmpty()) {
                return null; // 书名为空则跳过
            }
            
            return ebook;
            
        } catch (Exception e) {
            // 解析行数据出错，返回null跳过该行
            return null;
        }
    }
    
    
}
