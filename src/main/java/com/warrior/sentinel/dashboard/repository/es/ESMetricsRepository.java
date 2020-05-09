package com.warrior.sentinel.dashboard.repository.es;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.repository.metric.MetricsRepository;
import com.alibaba.csp.sentinel.util.StringUtil;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author majun
 * @description sentinel 监控数据ES相关操作
 * @date 2020/4/27
 */
@Repository("esMetricsRepository")
@Primary
public class ESMetricsRepository implements MetricsRepository<MetricEntity> {

    private static final Logger logger = LoggerFactory.getLogger(ESMetricsRepository.class);

    @Autowired
    private JestClient jestClient;

    @Override
    public void save(MetricEntity metric) {

        if (metric == null || StringUtil.isBlank(metric.getApp())) {
            return;
        }

        ESMetric esMetric = new ESMetric();

        BeanUtils.copyProperties(metric, esMetric);
        esMetric.setTimestamp(metric.getTimestamp().getTime());

        try {
            //保存监控数据到ES中
            Index index = new Index.Builder(esMetric).index(ESMetric.INDEX_NAME).type(ESMetric.TYPE).build();
            jestClient.execute(index);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            logger.debug("metrics is empty");
            return;
        }

        metrics.forEach(this::save);
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<MetricEntity>();

        if (StringUtil.isBlank(app)) {
            return results;
        }

        if (StringUtil.isBlank(resource)) {
            return results;
        }

        //多条件查询设置
        MatchPhraseQueryBuilder mpqb1 = QueryBuilders.matchPhraseQuery("app", app);
        MatchPhraseQueryBuilder mpqb2 = QueryBuilders.matchPhraseQuery("resource", resource);

        // 设置时间过滤
        RangeQueryBuilder rqb1 = QueryBuilders.rangeQuery("timestamp").gte(startTime).lte(endTime);

        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(mpqb1).must(mpqb2).must(rqb1);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(queryBuilder);

        //建立查询
        Search search = new Search.Builder(sourceBuilder.toString())
                .addIndex(ESMetric.INDEX_NAME).addType(ESMetric.TYPE).build();
        try {
            JestResult result = jestClient.execute(search);
            List<ESMetric> esMetrics = result.getSourceAsObjectList(ESMetric.class);
            if (CollectionUtils.isEmpty(esMetrics)) {
                return results;
            }
            for (ESMetric esMetric : esMetrics) {
                MetricEntity metricEntity = new MetricEntity();
                BeanUtils.copyProperties(esMetric, metricEntity);
                metricEntity.setTimestamp(Date.from(Instant.ofEpochMilli(esMetric.getTimestamp())));
                results.add(metricEntity);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return results;
        }
        return results;
    }

    @Override
    public List<String> listResourcesOfApp(String app) {

        List<String> results = new ArrayList<>();

        if (StringUtil.isBlank(app)) {
            return results;
        }

        //多条件查询设置
        MatchPhraseQueryBuilder mpqb1 = QueryBuilders.matchPhraseQuery("app", app);
//        MatchPhraseQueryBuilder mpqb2 = QueryBuilders.matchPhraseQuery("resource", resource);
        //设置时间过滤
        long startTime = System.currentTimeMillis() - 1000 * 60 * 60;
        RangeQueryBuilder rqb1 = QueryBuilders.rangeQuery("timestamp").gte(startTime);

        QueryBuilder queryBuilder = QueryBuilders.boolQuery().must(mpqb1).must(rqb1);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(queryBuilder);

        //建立查询
        Search search = new Search.Builder(sourceBuilder.toString())
                .addIndex(ESMetric.INDEX_NAME).addType(ESMetric.TYPE).build();

        List<MetricEntity> metricEntities = new ArrayList<MetricEntity>();

        try {
            JestResult result = jestClient.execute(search);
            List<ESMetric> esMetrics = result.getSourceAsObjectList(ESMetric.class);
            if (CollectionUtils.isEmpty(esMetrics)) {
                return results;
            }

            for (ESMetric esMetric : esMetrics) {
                MetricEntity metricEntity = new MetricEntity();
                BeanUtils.copyProperties(esMetric, metricEntity);
                metricEntities.add(metricEntity);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return results;
        }

        Map<String, MetricEntity> resourceCount = new HashMap<>(32);

        for (MetricEntity metricEntity : metricEntities) {
            String resource = metricEntity.getResource();
            if (resourceCount.containsKey(resource)) {
                MetricEntity oldEntity = resourceCount.get(resource);
                oldEntity.addPassQps(metricEntity.getPassQps());
                oldEntity.addRtAndSuccessQps(metricEntity.getRt(), metricEntity.getSuccessQps());
                oldEntity.addBlockQps(metricEntity.getBlockQps());
                oldEntity.addExceptionQps(metricEntity.getExceptionQps());
                oldEntity.addCount(1);
            } else {
                resourceCount.put(resource, MetricEntity.copyOf(metricEntity));
            }
        }

        // Order by last minute b_qps DESC.
        return resourceCount.entrySet()
                .stream()
                .sorted((o1, o2) -> {
                    MetricEntity e1 = o1.getValue();
                    MetricEntity e2 = o2.getValue();
                    int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                    if (t != 0) {
                        return t;
                    }
                    return e2.getPassQps().compareTo(e1.getPassQps());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
