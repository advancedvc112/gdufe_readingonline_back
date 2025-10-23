package com.gdufe.readingonline.service.topic.impl;

import com.gdufe.readingonline.controller.admin.vo.TopicCreateRequestVO;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateResponseVO;
import com.gdufe.readingonline.dal.dataobject.GdufeTopicEbookDO;
import com.gdufe.readingonline.dal.mysqlmapper.GdufeTopicEbookMapper;
import com.gdufe.readingonline.service.topic.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 图书专题Service实现类
 * 
 * @author gdufe
 * @date 2025
 */
@Service
public class TopicServiceImpl implements TopicService {
    
    private static final Logger logger = LoggerFactory.getLogger(TopicServiceImpl.class);
    
    @Autowired
    private GdufeTopicEbookMapper topicEbookMapper;
    
    /**
     * 创建图书专题
     * 
     * @param requestVO 创建请求VO
     * @return 创建响应VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicCreateResponseVO createTopic(TopicCreateRequestVO requestVO) {
        try {
            logger.info("开始创建图书专题，创建人：{}", requestVO.getCreator());
            
            // 生成专题编号：当前时间年月日 + 6位UUID
            String topicNo = generateTopicNo();
            logger.info("生成专题编号：{}", topicNo);
            
            // 创建实体对象
            GdufeTopicEbookDO topicDO = new GdufeTopicEbookDO();
            topicDO.setBookTopicNo(topicNo);
            topicDO.setBookTopicHtml(requestVO.getHtmlContent()); // 保持HTML文本完整
            topicDO.setBookTopicCreator(requestVO.getCreator());
            topicDO.setBookTopicStatus(0); // 默认为草稿状态
            topicDO.setIsDeleted(0); // 未删除
            topicDO.setCreateTime(LocalDateTime.now());
            topicDO.setUpdateTime(LocalDateTime.now());
            
            // 保存到数据库
            int insertResult = topicEbookMapper.insert(topicDO);
            
            if (insertResult > 0) {
                logger.info("图书专题创建成功，专题ID：{}，专题编号：{}", topicDO.getId(), topicNo);
                
                // 构建响应对象
                TopicCreateResponseVO responseVO = new TopicCreateResponseVO();
                responseVO.setTopicNo(topicNo);
                responseVO.setTopicId(topicDO.getId());
                responseVO.setCreateTime(topicDO.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                return responseVO;
            } else {
                logger.error("图书专题创建失败，数据库插入返回0");
                throw new RuntimeException("图书专题创建失败");
            }
            
        } catch (Exception e) {
            logger.error("创建图书专题时发生异常：{}", e.getMessage(), e);
            throw new RuntimeException("创建图书专题失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成专题编号：当前时间年月日 + 6位UUID
     * 格式：yyyyMMdd + 6位UUID
     * 
     * @return 专题编号
     */
    private String generateTopicNo() {
        // 获取当前日期，格式：yyyyMMdd
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // 生成6位UUID（取UUID的前6位）
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        
        return dateStr + uuid;
    }
}
