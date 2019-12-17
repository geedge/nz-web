package com.nis.modules.endpoint.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.endpoint.dao.MetadataLabelDao;
import com.nis.modules.endpoint.entity.MetadataLabel;
import com.nis.modules.endpoint.service.MetadataLabelService;
import org.springframework.stereotype.Service;

@Service
public class MetadataLabelServiceImpl extends ServiceImpl<MetadataLabelDao, MetadataLabel> implements MetadataLabelService {
}
