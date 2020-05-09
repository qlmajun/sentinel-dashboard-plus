FROM anapsix/alpine-java:8
WORKDIR /
ADD sentinel-dashboard-plus.jar sentinel-dashboard.jar
EXPOSE 9090
RUN echo 'Asia/Shanghai' > /etc/timezone
ENTRYPOINT  java -jar sentinel-dashboard.jar