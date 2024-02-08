FROM openeuler/openeuler:22.03 as BUILDER
ENV LANG="C.UTF-8"
RUN cd / \
    && yum install -y wget \
    && wget https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz \
    && tar -zxvf OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz \
    && wget https://repo.huaweicloud.com/apache/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz \
    && tar -zxvf apache-maven-3.8.1-bin.tar.gz \
    && yum install -y git

COPY . /search-help

ENV JAVA_HOME=/jdk-17.0.10+7
ENV PATH=${JAVA_HOME}/bin:$PATH

ENV MAVEN_HOME=/apache-maven-3.8.1
ENV PATH=${MAVEN_HOME}/bin:$PATH

RUN cd /search-help \
    && mvn clean install package -Dmaven.test.skip

FROM openeuler/openeuler:22.03
ENV LANG="C.UTF-8"
RUN yum update -y \
    && yum install -y shadow

RUN groupadd -g 1001 easysearch \
    && useradd -u 1001 -g easysearch -s /bin/bash -m easysearch

ENV WORKSPACE=/home/easysearch
WORKDIR ${WORKSPACE}

COPY --chown=easysearch --from=Builder /search-help/target ${WORKSPACE}/target

ENV JAVA_HOME=${WORKSPACE}/jre
ENV PATH=${JAVA_HOME}/bin:$PATH

USER easysearch

CMD java -jar ${WORKSPACE}/target/search-help.jar


