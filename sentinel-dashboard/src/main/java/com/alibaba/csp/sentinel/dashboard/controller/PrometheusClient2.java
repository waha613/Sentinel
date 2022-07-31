package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.dashboard.config.MyGauge;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.domain.vo.MetricVo;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class PrometheusClient2 {

    private static Logger logger = LoggerFactory.getLogger(MetricController.class);

    @Autowired
    private MetricsRepository<MetricEntity> metricStore;

    @Autowired
    private AppManagement appManagement;

    @Autowired
    private CollectorRegistry collectorRegistry;

    private MyGauge gauge = null;
    @PostConstruct
    public void setMetrics() throws InterruptedException {
        Thread thread = new Thread(){
            @Override
            public void run(){
                while (true){
                    List<String> allAppName = getAllAppName();
                    for (String appName : allAppName) {
                        System.out.println(appName);
                        Map<String, Iterable<MetricVo>> resourceMetrics = queryTopResourceMetric(appName, null, null);
                        System.out.println(resourceMetrics);
                        generateMetrics(appName,resourceMetrics);
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();

    }

    private void generateMetrics(String app,Map<String, Iterable<MetricVo>> appMetricMap) {
        List<Collector.MetricFamilySamples> metricFamilySamplesList = new ArrayList<>();
        List<Collector.MetricFamilySamples.Sample> samples = new ArrayList<>();
        Collector.MetricFamilySamples metricFamilySamples = new Collector.MetricFamilySamples("sentinel_client_requests", Collector.Type.GAUGE, "sentinel_client_requests", samples);

        Set<String> resources = appMetricMap.keySet();
        for (String resource : resources) {
            Iterable<MetricVo> metricVos = appMetricMap.get(resource);
            for (MetricVo metricVo : metricVos) {

                samples.add(setPassQps(metricVo));
                samples.add(setBlockQps(metricVo));
                samples.add(setSuccessQps(metricVo));
                samples.add(setExceptionQps(metricVo));
                samples.add(setRt(metricVo));

                samples.add(resetPassQps(metricVo));
                samples.add(resetBlockQps(metricVo));
                samples.add(resetSuccessQps(metricVo));
                samples.add(resetExceptionQps(metricVo));
                samples.add(resetRt(metricVo));
            }
        }
        metricFamilySamplesList.add(metricFamilySamples);
        if(gauge == null){
            gauge = MyGauge.build().name("sentinel_client_requests").help("sentinel_client_requests").register(collectorRegistry);
        }
        gauge.setSamples(metricFamilySamplesList);

        List<Collector.MetricFamilySamples> collect = gauge.collect();
        for (Collector.MetricFamilySamples familySamples : collect) {
            System.out.println(familySamples);
        }
    }

    private List<String> getAllAppName(){
        return appManagement.getAppNames();
    }

    private Map<String, Iterable<MetricVo>> queryTopResourceMetric(String app,Long startTime, Long endTime){
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }
        if (startTime == null) {
            startTime = endTime - 1000 * 60 * 5;
        }

        List<String> resources = metricStore.listResourcesOfApp(app);
        logger.debug("queryTopResourceMetric(), resources.size()={}", resources.size());

        final Map<String, Iterable<MetricVo>> map = new ConcurrentHashMap<>();
        logger.debug("topResource={}", resources);
        long time = System.currentTimeMillis();
        for (final String resource : resources) {
            List<MetricEntity> entities = metricStore.queryByAppAndResourceBetween(
                    app, resource, startTime, endTime);
            logger.debug("resource={}, entities.size()={}", resource, entities == null ? "null" : entities.size());
            List<MetricVo> vos = MetricVo.fromMetricEntities(entities, resource);
            Iterable<MetricVo> vosSorted = sortMetricVoAndDistinct(vos);
            map.put(resource, vosSorted);
        }
        logger.debug("queryTopResourceMetric() total query time={} ms", System.currentTimeMillis() - time);

        return map;
    }

    private Iterable<MetricVo> sortMetricVoAndDistinct(List<MetricVo> vos) {
        if (vos == null) {
            return null;
        }
        Map<Long, MetricVo> map = new TreeMap<>();
        for (MetricVo vo : vos) {
            MetricVo oldVo = map.get(vo.getTimestamp());
            if (oldVo == null || vo.getGmtCreate() > oldVo.getGmtCreate()) {
                map.put(vo.getTimestamp(), vo);
            }
        }
        return map.values();
    }

    private Collector.MetricFamilySamples.Sample setPassQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("passQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,metricVo.getPassQps(),metricVo.getTimestamp());
    }

    private Collector.MetricFamilySamples.Sample resetPassQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("passQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,0,metricVo.getTimestamp() + 500);
    }

    private Collector.MetricFamilySamples.Sample setBlockQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("blockQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,metricVo.getBlockQps(),metricVo.getTimestamp());
    }

    private Collector.MetricFamilySamples.Sample resetBlockQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("blockQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,0,metricVo.getTimestamp() + 500);
    }

    private Collector.MetricFamilySamples.Sample setSuccessQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("successQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,metricVo.getSuccessQps(),metricVo.getTimestamp());
    }

    private Collector.MetricFamilySamples.Sample resetSuccessQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("successQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,0,metricVo.getTimestamp() + 500);
    }

    private Collector.MetricFamilySamples.Sample setExceptionQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("exceptionQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,metricVo.getExceptionQps(),metricVo.getTimestamp());
    }

    private Collector.MetricFamilySamples.Sample resetExceptionQps(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("exceptionQps");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,0,metricVo.getTimestamp() + 500);
    }

    private Collector.MetricFamilySamples.Sample setRt(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("rt");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,metricVo.getRt(),metricVo.getTimestamp());
    }

    private Collector.MetricFamilySamples.Sample resetRt(MetricVo metricVo){
        List<String> labelName = new ArrayList<>();
        labelName.add("application");
        labelName.add("uri");
        labelName.add("type");

        List<String> labelValue = new ArrayList<>();
        labelValue.add(metricVo.getApp());
        labelValue.add(metricVo.getResource());
        labelValue.add("rt");
        return new Collector.MetricFamilySamples.Sample("sentinel_client_requests",labelName,labelValue,0,metricVo.getTimestamp() + 500);
    }

}
