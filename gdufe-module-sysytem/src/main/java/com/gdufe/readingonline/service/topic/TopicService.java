package com.gdufe.readingonline.service.topic;

import com.gdufe.readingonline.controller.admin.vo.TopicCreateRequestVO;
import com.gdufe.readingonline.controller.admin.vo.TopicCreateResponseVO;

/**
 * 图书专题Service接口
 * 
 * @author gdufe
 * @date 2025
 */
public interface TopicService {
    
    /**
     * 创建图书专题
     * 
     * @param requestVO 创建请求VO
     * @return 创建响应VO
     */
    TopicCreateResponseVO createTopic(TopicCreateRequestVO requestVO);
}
