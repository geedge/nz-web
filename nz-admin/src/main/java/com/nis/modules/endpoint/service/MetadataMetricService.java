package com.nis.modules.endpoint.service;

import com.nis.modules.endpoint.entity.MetadataMetric;

import java.io.IOException;
import java.util.List;

public interface MetadataMetricService{

    List<MetadataMetric> metadata(Integer endpointId) throws IOException;

}
