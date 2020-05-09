package com.warrior.sentinel.dashboard.repository.es;

import java.io.Serializable;
import java.util.Date;

/**
 * @author majun
 * @description sentinel 监控数据ES存储封装对象
 * @date 2020/4/27
 */
public class ESMetric implements Serializable {
    private static final long serialVersionUID = 7200023615444172716L;

    public static final String INDEX_NAME = "sentinel";

    public static final String TYPE = "sentinel_metric";

    /**id，主键*/
    private Long id;

    /**创建时间*/
    private Date gmtCreate;

    /**修改时间*/
    private Date gmtModified;

    /**应用名称*/
    private String app;

    /**统计时间*/
    private Long timestamp;

    /**资源名称*/
    private String resource;

    /**通过qps*/
    private Long passQps;

    /**成功qps*/
    private Long successQps;

    /**限流qps*/
    private Long blockQps;

    /**发送异常的次数*/
    private Long exceptionQps;

    /**所有successQps的rt的和*/
    private Double rt;

    /**本次聚合的总条数*/
    private Integer count;

    /**资源的hashCode*/
    private Integer resourceCode;

    public static String getIndexName() {
        return INDEX_NAME;
    }

    public static String getTYPE() {
        return TYPE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getPassQps() {
        return passQps;
    }

    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    public Long getSuccessQps() {
        return successQps;
    }

    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    public Long getBlockQps() {
        return blockQps;
    }

    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    public Long getExceptionQps() {
        return exceptionQps;
    }

    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    public Double getRt() {
        return rt;
    }

    public void setRt(Double rt) {
        this.rt = rt;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(Integer resourceCode) {
        this.resourceCode = resourceCode;
    }

    @Override
    public String toString() {
        return "ESMetric{" +
                "id=" + id +
                ", gmtCreate=" + gmtCreate +
                ", gmtModified=" + gmtModified +
                ", app='" + app + '\'' +
                ", timestamp=" + timestamp +
                ", resource='" + resource + '\'' +
                ", passQps=" + passQps +
                ", successQps=" + successQps +
                ", blockQps=" + blockQps +
                ", exceptionQps=" + exceptionQps +
                ", rt=" + rt +
                ", count=" + count +
                ", resourceCode=" + resourceCode +
                '}';
    }
}
