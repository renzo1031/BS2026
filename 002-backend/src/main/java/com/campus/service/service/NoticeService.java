package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.common.AuthContext;
import com.campus.service.common.BusinessException;
import com.campus.service.entity.Notice;
import com.campus.service.mapper.NoticeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {
    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public void create(Long userId, Long requestId, String title, String content) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setRequestId(requestId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setReadFlag(0);
        notice.setCreatedAt(LocalDateTime.now());
        noticeMapper.insert(notice);
    }

    public List<Notice> my() {
        return noticeMapper.selectList(new QueryWrapper<Notice>()
                .eq("user_id", AuthContext.get().userId())
                .orderByDesc("created_at"));
    }

    public void read(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null || !notice.getUserId().equals(AuthContext.get().userId())) {
            throw new BusinessException("消息不存在");
        }
        notice.setReadFlag(1);
        noticeMapper.updateById(notice);
    }
}
