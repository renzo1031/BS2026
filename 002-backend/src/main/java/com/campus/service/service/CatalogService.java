package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.entity.ServiceCategory;
import com.campus.service.entity.ServiceItem;
import com.campus.service.entity.Venue;
import com.campus.service.mapper.ServiceCategoryMapper;
import com.campus.service.mapper.ServiceItemMapper;
import com.campus.service.mapper.VenueMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogService {
    private final ServiceCategoryMapper categoryMapper;
    private final ServiceItemMapper itemMapper;
    private final VenueMapper venueMapper;

    public CatalogService(ServiceCategoryMapper categoryMapper, ServiceItemMapper itemMapper, VenueMapper venueMapper) {
        this.categoryMapper = categoryMapper;
        this.itemMapper = itemMapper;
        this.venueMapper = venueMapper;
    }

    public List<ServiceCategory> categories() {
        return categoryMapper.selectList(new QueryWrapper<ServiceCategory>().eq("enabled", 1).orderByAsc("sort_no"));
    }

    public List<ServiceItem> items(Long categoryId) {
        QueryWrapper<ServiceItem> query = new QueryWrapper<ServiceItem>().eq("enabled", 1).orderByAsc("id");
        if (categoryId != null) {
            query.eq("category_id", categoryId);
        }
        return itemMapper.selectList(query);
    }

    public List<Venue> venues() {
        return venueMapper.selectList(new QueryWrapper<Venue>().eq("status", "AVAILABLE").orderByAsc("id"));
    }
}
