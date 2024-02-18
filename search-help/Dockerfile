FROM gplane/pnpm as Builder
ENV LANG="C.UTF-8"

ARG COMMUNITY=openeuler

WORKDIR /

RUN apt update \
    && wget https://download.oracle.com/java/17/archive/jdk-17.0.7_linux-x64_bin.tar.gz \
    && tar -zxvf jdk-17.0.7_linux-x64_bin.tar.gz \
    && wget https://repo.huaweicloud.com/apache/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz \
    && tar -zxvf apache-maven-3.8.1-bin.tar.gz \
    && npm i pnpm -g

ENV JAVA_HOME=/jdk-17.0.7
ENV PATH=${JAVA_HOME}/bin:$PATH

ENV MAVEN_HOME=/apache-maven-3.8.1
ENV PATH=${MAVEN_HOME}/bin:$PATH

COPY . /EaseSearch-Import/search-help

RUN cd /EaseSearch-Import/search-help \
    && mvn clean install package -Dmaven.test.skip

RUN cp -r /jdk-17.0.7 /jre

FROM openeuler/openeuler:23.03
ENV LANG="C.UTF-8"

RUN yum update -y \
    && yum install -y shadow

RUN groupadd -g 1001 easysearch \
    && useradd -u 1001 -g easysearch -s /bin/bash -m easysearch

ENV WORKSPACE=/home/easysearch
WORKDIR ${WORKSPACE}

COPY --chown=easysearch --from=Builder /EaseSearch-Import/search-help/target ${WORKSPACE}/target
COPY --chown=easysearch --from=Builder /jre ${WORKSPACE}/jre

ENV JAVA_HOME=${WORKSPACE}/jre
ENV PATH=${JAVA_HOME}/bin:$PATH

USER easysearch

CMD java -jar ${WORKSPACE}/target/search-help.jar


