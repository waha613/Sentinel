package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.vo.MetricVo;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ll4859332@hotmail.com
 * @version V1.0
 * @title
 * @description
 * @date 2022-07-27 16:22
 */
@Component
public class PrometheusClient {

    @Autowired
    private MetricsRepository<MetricEntity> metricStore;

    @Autowired
    private AppManagement appManagement;

    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    private void addSummary() throws InterruptedException {
        Thread thread = new Thread(){
            @Override
            public void run() {
                long end = System.currentTimeMillis();
                end = end - end % 1000 - 2000;
                long start = end - 1000;

                Map<Long,MetricEntity> map;
                while (true){
                    map = new HashMap<>();

                    List<String> appNames = appManagement.getAppNames();
                    for (String appName : appNames) {

                        System.out.println("appName:" + appName);

                        List<String> resources = metricStore.listResourcesOfApp(appName);
                        for (String resource : resources) {
                            if(!resource.startsWith("/")){
                                continue;
                            }

                            System.out.println("resource:" + resource);

                            List<MetricEntity> metricEntities = metricStore.queryByAppAndResourceBetween(appName, resource, start, end);
                            for (MetricEntity metricEntity : metricEntities) {
                                MetricEntity oldEntity = map.get(metricEntity.getTimestamp().getTime());
                                if (oldEntity == null || metricEntity.getGmtCreate().after(oldEntity.getGmtCreate()) ) {
                                    map.put(metricEntity.getTimestamp().getTime(), metricEntity);
                                }
                            }
                            Collection<MetricEntity> distinctEntities = map.values();
                            if(!map.isEmpty()){
                                System.out.println("map:" + map);
                                System.out.println("mapsize:" + map.size()+",distinctsize:"+distinctEntities.size());
                            }

                            for (MetricEntity metricEntity : distinctEntities) {
                                meterRegistry.summary("sentinel_client_requests_seconds",
                                        "application","sentinel-dashboard",
                                        "uri",resource,
                                        "type","passQps").record(metricEntity.getPassQps());

                                meterRegistry.summary("sentinel_client_requests_seconds",
                                        "application","sentinel-dashboard",
                                        "uri",resource,
                                        "type","blockQps").record(metricEntity.getBlockQps());

                                meterRegistry.summary("sentinel_client_requests_seconds",
                                        "application","sentinel-dashboard",
                                        "uri",resource,
                                        "type","successQps").record(metricEntity.getSuccessQps());

                                meterRegistry.summary("sentinel_client_requests_seconds",
                                        "application","sentinel-dashboard",
                                        "uri",resource,
                                        "type","exceptionQps").record(metricEntity.getExceptionQps());

                                meterRegistry.summary("sentinel_client_requests_seconds",
                                        "application","sentinel-dashboard",
                                        "uri",resource,
                                        "type","rt").record(metricEntity.getRt());

//                                System.out.println("数据+1");
//                                System.out.println(metricEntity.getTimestamp().getTime());
//                                System.out.println(metricEntity);
                            }
                        }

                    }

                    try {
                        end += 1000;
                        start += 1000;
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        thread.start();
    }

}
