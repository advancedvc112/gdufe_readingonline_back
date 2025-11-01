package com.gdufe.readingonline.service.excelUploadAndPrase.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdufe.readingonline.service.excelUploadAndPrase.ExcelParseService;
import com.gdufe.readingonline.dal.dataobject.GdufeLibraryEbookDO;
import com.gdufe.readingonline.dal.mysqlmapper.GdufeLibraryEbookMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel解析结果封装类
 */
class ExcelParseResult {
    private List<GdufeLibraryEbookDO> ebookList;
    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<Integer> skippedRows; // 跳过的记录行号（ISBN为空的记录）
    
    public ExcelParseResult(List<GdufeLibraryEbookDO> ebookList, int totalRows, int successCount, int failureCount, List<Integer> skippedRows) {
        this.ebookList = ebookList;
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.skippedRows = skippedRows;
    }
    
    public List<GdufeLibraryEbookDO> getEbookList() {
        return ebookList;
    }
    
    public int getTotalRows() {
        return totalRows;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public List<Integer> getSkippedRows() {
        return skippedRows;
    }
}

/**
 * Excel解析和数据库导入服务实现类
 * 
 * @author gdufe
 * @date 2024
 */
@Service
public class ExcelParseServiceImpl implements ExcelParseService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelParseServiceImpl.class);
    
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
            ExcelParseResult parseResult;
            if ("0".equals(fileSource) || "changxiang".equalsIgnoreCase(fileSource)) {
                // 畅想之星
                parseResult = parseChangxiangExcelFile(excelFile);
            } else if ("1".equals(fileSource) || "jingdong".equalsIgnoreCase(fileSource)) {
                // 京东
                parseResult = parseJingdongExcelFile(excelFile);
            } else {
                result.setSuccess(false);
                result.setMessage("不支持的文件来源：" + fileSource);
                return result;
            }
            
            List<GdufeLibraryEbookDO> ebookList = parseResult.getEbookList();
            if (ebookList == null || ebookList.isEmpty()) {
                result.setSuccess(false);
                result.setMessage("Excel文件中没有有效数据");
                return result;
            }
            
            // 记录解析统计信息
            logger.info("Excel解析统计 - 总行数：{}, 解析成功：{}, 解析失败：{}", 
                parseResult.getTotalRows(), parseResult.getSuccessCount(), parseResult.getFailureCount());
            
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
            
            // 分批处理，每批5000条记录
            int batchSize = 5000;
            int totalSize = ebookList.size();
            int totalBatches = (totalSize + batchSize - 1) / batchSize; // 计算总批次数
            
            logger.info("=== 开始批量处理Excel数据 ===");
            logger.info("总记录数：{}，批次大小：{}，总批次数：{}", totalSize, batchSize, totalBatches);
            
            for (int i = 0; i < totalSize; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalSize);
                List<GdufeLibraryEbookDO> batch = ebookList.subList(i, endIndex);
                int currentBatch = (i/batchSize + 1);
                
                logger.info("=== 开始处理第 {}/{} 批 === 批次大小：{}", currentBatch, totalBatches, batch.size());
                
                try {
                    // 使用批量插入或更新（基于ISBN）
                    int batchProcessed = ebookMapper.batchInsertOrUpdate(batch);
                    insertedCount += batchProcessed;
                    
                    // 成功时显示批次信息，便于监控处理进度
                    logger.info("=== 批次 {}/{} 处理成功 === 处理记录数：{}，累计成功：{}", 
                        (i/batchSize + 1), totalBatches, batchProcessed, insertedCount);
                    
                } catch (Exception e) {
                    // 失败时显示详细错误信息
                    logger.error("批量插入或更新第 {} 批失败，批次大小：{}，错误详情：{}", (i/batchSize + 1), batch.size(), e.getMessage(), e);
                    
                    // 如果批量操作失败，尝试逐条插入或更新
                    for (GdufeLibraryEbookDO ebook : batch) {
                        try {
                            // 先查询是否存在
                            LambdaQueryWrapper<GdufeLibraryEbookDO> queryWrapper = new LambdaQueryWrapper<>();
                            queryWrapper.eq(GdufeLibraryEbookDO::getBookIsbn, ebook.getBookIsbn());
                            GdufeLibraryEbookDO existingEbook = ebookMapper.selectOne(queryWrapper);
                            if (existingEbook != null) {
                                // 存在则更新
                                ebook.setId(existingEbook.getId());
                                ebook.setCreateTime(existingEbook.getCreateTime()); // 保持原创建时间
                                ebookMapper.updateById(ebook);
                                // 成功时不显示详细记录
                            } else {
                                // 不存在则插入
                                ebookMapper.insert(ebook);
                                // 成功时不显示详细记录
                            }
                            insertedCount++;
                        } catch (Exception ex) {
                            // 失败时显示详细错误信息
                            logger.error("单条处理失败：ISBN={}, 书名={}, 错误详情：{}", 
                                ebook.getBookIsbn(), ebook.getBookName(), ex.getMessage(), ex);
                            skippedCount++;
                        }
                    }
                }
            }
            
            logger.info("=== 批量处理完成 === 总批次数：{}，成功处理：{}条记录", totalBatches, insertedCount);
            
            result.setSuccess(true);
            result.setMessage("Excel文件解析并导入完成（基于ISBN进行插入或更新）");
            result.setTotalRows(parseResult.getTotalRows());
            result.setInsertedRows(insertedCount);
            result.setSkippedRows(skippedCount);
            result.setSkippedRowsList(parseResult.getSkippedRows());
            
            logger.info("Excel文件处理完成 - 解析统计：总行数={}, 解析成功={}, 解析失败={}, ISBN为空跳过={}, 跳过行号={}, 数据库操作：成功处理={}, 跳过={}", 
                parseResult.getTotalRows(), parseResult.getSuccessCount(), parseResult.getFailureCount(), 
                parseResult.getSkippedRows().size(), parseResult.getSkippedRows(), insertedCount, skippedCount);
            
        } catch (Exception e) {
            logger.error("Excel文件解析失败：{}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("Excel文件解析失败：" + e.getMessage());
        }
        
        return result;
    }
    
    
    /**
     * 解析畅想之星Excel文件
     * 
     * @param excelFile Excel文件
     * @return 解析结果包含电子书数据列表和统计信息
     */
    private ExcelParseResult parseChangxiangExcelFile(MultipartFile excelFile) {
        List<GdufeLibraryEbookDO> ebookList = new ArrayList<>();
        List<Integer> skippedRows = new ArrayList<>(); // 记录跳过的行号（ISBN为空的记录）
        int totalRows = 0;
        int successCount = 0;
        int failureCount = 0;
        
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
                if (row == null) {
                    failureCount++;
                    logger.warn("畅想之星解析失败 - 行号：{}, 原因：行为空", i + 1);
                    continue;
                }
                
                totalRows++;
                
                try {
                    // 先检查ISBN是否为空，如果为空则跳过该记录
                    Integer isbnIndex = columnIndexMap.get("isbn");
                    if (isbnIndex != null) {
                        Cell isbnCell = row.getCell(isbnIndex);
                        String isbn = getCellStringValue(isbnCell);
                        if (isbn == null || isbn.trim().isEmpty()) {
                            skippedRows.add(i + 1); // 记录跳过的行号
                            logger.warn("畅想之星跳过记录 - 行号：{}, 原因：ISBN号为空", i + 1);
                            continue;
                        }
                    } else {
                        skippedRows.add(i + 1); // 记录跳过的行号
                        logger.warn("畅想之星跳过记录 - 行号：{}, 原因：Excel文件中缺少ISBN列", i + 1);
                        continue;
                    }
                    
                    GdufeLibraryEbookDO ebook = parseChangxiangRow(row, columnIndexMap);
                    if (ebook != null) {
                        ebookList.add(ebook);
                        successCount++;
                    } else {
                        failureCount++;
                        logger.warn("畅想之星解析失败 - 行号：{}, 原因：书名为空或必填字段缺失", i + 1);
                    }
                } catch (Exception e) {
                    failureCount++;
                    logger.error("畅想之星解析失败 - 行号：{}, 原因：{}", i + 1, e.getMessage());
                }
            }
            
            workbook.close();
            
        } catch (IOException e) {
            logger.error("读取畅想之星Excel文件失败：{}", e.getMessage(), e);
        }
        
        logger.info("畅想之星Excel解析完成 - 总行数：{}, 成功：{}, 失败：{}, 跳过：{}", totalRows, successCount, failureCount, skippedRows.size());
        
        return new ExcelParseResult(ebookList, totalRows, successCount, failureCount, skippedRows);
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
            } else if (headerValue.equals("第一责任者")) {
                columnIndexMap.put("author", i);
            } else if (headerValue.equals("第二责任者")) {
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
            logger.warn("解析行数据异常：{}", e.getMessage());
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
     * 将分类名称转换为INT值
     * 映射关系：0-空白, 1-社会科学, 2-小说, 3-教材教辅, 4-科学新知, 5-文学, 6-经济管理, 7-少儿, 8-进口原版, 9-计算机, 10-生活休闲, 11-成功励志, 12-历史考古, 13-艺术摄影, 14-外语学习, 15-政治军事, 16-人物传记, 17-家教育儿, 18-中外名著, 19-漫画杂志, 20-职场进阶, 21-互联网+, 22-影视原著
     * 
     * @param categoryStr 分类名称或数字字符串
     * @return 对应的INT值，如果无法匹配或为空则返回0（空白）
     */
    private Integer convertCategoryToInteger(String categoryStr) {
        if (categoryStr == null || categoryStr.trim().isEmpty()) {
            logger.debug("分类为空，返回0（空白）");
            return 0; // 空白
        }
        
        String trimmed = categoryStr.trim();
        
        // 如果是数字字符串，直接转换
        try {
            int numericValue = Integer.parseInt(trimmed);
            // 验证范围在0-22之间
            if (numericValue >= 0 && numericValue <= 22) {
                logger.debug("分类为数字：{}，直接返回", numericValue);
                return numericValue;
            }
        } catch (NumberFormatException e) {
            // 不是数字，继续按名称匹配
        }
        
        // 按分类名称匹配
        switch (trimmed) {
            case "空白":
                return 0;
            case "社会科学":
                return 1;
            case "小说":
                return 2;
            case "教材教辅":
                return 3;
            case "科学新知":
                return 4;
            case "文学":
                return 5;
            case "经济管理":
                return 6;
            case "少儿":
                return 7;
            case "进口原版":
                return 8;
            case "计算机":
                return 9;
            case "生活休闲":
                return 10;
            case "成功励志":
                return 11;
            case "历史考古":
                return 12;
            case "艺术摄影":
                return 13;
            case "外语学习":
                return 14;
            case "政治军事":
                return 15;
            case "人物传记":
                return 16;
            case "家教育儿":
                return 17;
            case "中外名著":
                return 18;
            case "漫画杂志":
                return 19;
            case "职场进阶":
                return 20;
            case "互联网+":
                return 21;
            case "影视原著":
                return 22;
            default:
                // 无法匹配，返回0（空白）
                logger.warn("无法识别的分类名称：{}，将设置为0（空白）", trimmed);
                return 0;
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
     * 解析封面图Excel文件并更新数据库中的封面图URL
     * 
     * @param excelFile Excel文件
     * @param fileSource 文件来源（0:畅想之星, 1:京东）
     * @return 导入结果
     */
    @Override
    public ParseResult parseAndImportCoverImages(MultipartFile excelFile, String fileSource) {
        ParseResult result = new ParseResult();
        
        try {
            // 解析Excel文件
            Workbook workbook;
            String fileName = excelFile.getOriginalFilename();
            
            if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(excelFile.getInputStream());
            } else {
                workbook = new HSSFWorkbook(excelFile.getInputStream());
            }
            
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            
            // 解析标题行，获取ISBN和封面图URL的列索引
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                result.setSuccess(false);
                result.setMessage("Excel文件第一行（标题行）为空");
                return result;
            }
            
            Integer isbnColumnIndex = null;
            Integer coverUrlColumnIndex = null;
            
            // 遍历标题行查找ISBN和封面图列
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                
                String headerValue = getCellStringValue(cell).trim();
                if (headerValue.isEmpty()) continue;
                
                // 匹配ISBN列
                if (headerValue.equals("ISBN")) {
                    isbnColumnIndex = i;
                }
                // 匹配封面图列
                else if (headerValue.equals("图书封面")) {
                    coverUrlColumnIndex = i;
                }
            }
            
            // 验证必要的列是否存在
            if (isbnColumnIndex == null) {
                result.setSuccess(false);
                result.setMessage("Excel文件中缺少ISBN列");
                workbook.close();
                return result;
            }
            
            if (coverUrlColumnIndex == null) {
                result.setSuccess(false);
                result.setMessage("Excel文件中缺少图书封面列");
                workbook.close();
                return result;
            }
            
            logger.info("封面图Excel列索引 - ISBN列：{}, 封面图列：{}", isbnColumnIndex, coverUrlColumnIndex);
            
            // 解析数据行并更新数据库
            int totalRows = 0;
            int updatedRows = 0;
            int skippedRows = 0;
            List<Integer> skippedRowsList = new ArrayList<>();
            
            // 确定book_source的值
            Integer bookSource = null;
            if ("0".equals(fileSource) || "changxiang".equalsIgnoreCase(fileSource)) {
                bookSource = 0; // 畅想之星
            } else if ("1".equals(fileSource) || "jingdong".equalsIgnoreCase(fileSource)) {
                bookSource = 1; // 京东
            } else {
                result.setSuccess(false);
                result.setMessage("不支持的文件来源：" + fileSource);
                workbook.close();
                return result;
            }
            
            // 从第二行开始解析数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    skippedRows++;
                    skippedRowsList.add(i + 1);
                    logger.warn("封面图解析跳过 - 行号：{}, 原因：行为空", i + 1);
                    continue;
                }
                
                totalRows++;
                
                try {
                    // 读取ISBN
                    Cell isbnCell = row.getCell(isbnColumnIndex);
                    String isbn = getCellStringValue(isbnCell);
                    if (isbn == null || isbn.trim().isEmpty()) {
                        skippedRows++;
                        skippedRowsList.add(i + 1);
                        logger.warn("封面图解析跳过 - 行号：{}, 原因：ISBN为空", i + 1);
                        continue;
                    }
                    isbn = isbn.trim();
                    
                    // 读取封面图URL
                    Cell coverUrlCell = row.getCell(coverUrlColumnIndex);
                    String coverUrl = getCellStringValue(coverUrlCell);
                    if (coverUrl == null || coverUrl.trim().isEmpty()) {
                        skippedRows++;
                        skippedRowsList.add(i + 1);
                        logger.warn("封面图解析跳过 - 行号：{}, 原因：封面图URL为空", i + 1);
                        continue;
                    }
                    coverUrl = coverUrl.trim();
                    
                    // 根据ISBN和book_source查询数据库
                    LambdaQueryWrapper<GdufeLibraryEbookDO> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(GdufeLibraryEbookDO::getBookIsbn, isbn)
                               .eq(GdufeLibraryEbookDO::getBookSource, bookSource);
                    
                    GdufeLibraryEbookDO ebook = ebookMapper.selectOne(queryWrapper);
                    
                    if (ebook != null) {
                        // 找到记录，更新封面图URL
                        ebook.setBookPictureUrl(coverUrl);
                        ebook.setUpdateTime(LocalDateTime.now());
                        
                        int updateResult = ebookMapper.updateById(ebook);
                        if (updateResult > 0) {
                            updatedRows++;
                            logger.debug("封面图更新成功 - 行号：{}, ISBN：{}, 书名：{}", i + 1, isbn, ebook.getBookName());
                        } else {
                            skippedRows++;
                            skippedRowsList.add(i + 1);
                            logger.warn("封面图更新失败 - 行号：{}, ISBN：{}", i + 1, isbn);
                        }
                    } else {
                        // 未找到记录
                        skippedRows++;
                        skippedRowsList.add(i + 1);
                        logger.warn("封面图解析跳过 - 行号：{}, 原因：未找到匹配记录（ISBN：{}, 来源：{}）", i + 1, isbn, bookSource);
                    }
                    
                } catch (Exception e) {
                    skippedRows++;
                    skippedRowsList.add(i + 1);
                    logger.error("封面图解析失败 - 行号：{}, 原因：{}", i + 1, e.getMessage(), e);
                }
            }
            
            workbook.close();
            
            result.setSuccess(true);
            result.setMessage("封面图Excel解析并更新完成");
            result.setTotalRows(totalRows);
            result.setInsertedRows(updatedRows);
            result.setSkippedRows(skippedRows);
            result.setSkippedRowsList(skippedRowsList);
            
            logger.info("封面图Excel处理完成 - 总行数：{}, 更新成功：{}, 跳过：{}, 跳过行号：{}", 
                totalRows, updatedRows, skippedRows, skippedRowsList);
            
        } catch (Exception e) {
            logger.error("封面图Excel文件解析失败：{}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("封面图Excel文件解析失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 解析京东Excel文件
     * 京东Excel格式：书名、ISBN、著者、出版社、一级分类、二级分类、出版时间、URL链接、简介
     * 
     * @param excelFile Excel文件
     * @return 解析后的电子书列表
     * @throws IOException IO异常
     */
    private ExcelParseResult parseJingdongExcelFile(MultipartFile excelFile) throws IOException {
        List<GdufeLibraryEbookDO> ebookList = new ArrayList<>();
        List<Integer> skippedRows = new ArrayList<>(); // 记录跳过的行号（ISBN为空的记录）
        int totalRows = 0;
        int successCount = 0;
        int failureCount = 0;
        
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
                logger.warn("京东Excel文件缺少工作表");
                return new ExcelParseResult(ebookList, totalRows, successCount, failureCount, skippedRows);
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
                    failureCount++;
                    logger.warn("京东解析失败 - 行号：{}, 原因：行为空", i + 1);
                    continue;
                }
                
                totalRows++;
                
                try {
                    // 先检查ISBN是否为空，如果为空则跳过该记录
                    Integer isbnIndex = columnIndexMap.get("isbn");
                    if (isbnIndex != null) {
                        Cell isbnCell = row.getCell(isbnIndex);
                        String isbn = getCellStringValue(isbnCell);
                        if (isbn == null || isbn.trim().isEmpty()) {
                            skippedRows.add(i + 1); // 记录跳过的行号
                            logger.warn("京东跳过记录 - 行号：{}, 原因：ISBN号为空", i + 1);
                            continue;
                        }
                    } else {
                        skippedRows.add(i + 1); // 记录跳过的行号
                        logger.warn("京东跳过记录 - 行号：{}, 原因：Excel文件中缺少ISBN列", i + 1);
                        continue;
                    }
                    
                    GdufeLibraryEbookDO ebook = parseJingdongRow(row, columnIndexMap);
                    if (ebook != null) {
                        ebookList.add(ebook);
                        successCount++;
                    } else {
                        failureCount++;
                        logger.warn("京东解析失败 - 行号：{}, 原因：书名为空或必填字段缺失", i + 1);
                    }
                } catch (Exception e) {
                    failureCount++;
                    logger.error("京东解析失败 - 行号：{}, 原因：{}", i + 1, e.getMessage());
                }
            }
            
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        
        logger.info("京东Excel解析完成 - 总行数：{}, 成功：{}, 失败：{}, 跳过：{}", totalRows, successCount, failureCount, skippedRows.size());
        
        return new ExcelParseResult(ebookList, totalRows, successCount, failureCount, skippedRows);
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
            
            // ISBN（已在主循环中验证不为空）
            Integer isbnIndex = columnIndexMap.get("isbn");
            if (isbnIndex != null) {
                Cell isbnCell = row.getCell(isbnIndex);
                if (isbnCell != null) {
                    String isbn = getCellStringValue(isbnCell);
                    if (isbn != null && !isbn.trim().isEmpty()) {
                        ebook.setBookIsbn(isbn.trim());
                    }
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
                    String categoryStr = getCellStringValue(primaryCategoryCell);
                    if (categoryStr != null && !categoryStr.trim().isEmpty()) {
                        Integer categoryInt = convertCategoryToInteger(categoryStr);
                        ebook.setBookPrimaryClassification(categoryInt);
                        logger.debug("一级分类转换：{} -> {}", categoryStr, categoryInt);
                    }
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
