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

COPY ./es-client /EaseSearch-Import/es-client
COPY ./${COMMUNITY} /EaseSearch-Import/import-task

RUN cd /EaseSearch-Import/es-client \
    && mvn install \
    && cd /EaseSearch-Import/import-task \
    && mvn clean install package -Dmaven.test.skip

RUN cd /EaseSearch-Import/import-task/target/classes \
    && chmod +x initDoc.sh \
    && ./initDoc.sh

RUN cp -r jdk-17.0.7 jre


FROM openeuler/openeuler:23.03
ENV LANG="C.UTF-8"

RUN yum update -y \
    && yum install -y shadow

RUN groupadd -g 1001 easysearch \
    && useradd -u 1001 -g easysearch -s /bin/bash -m easysearch

ENV WORKSPACE=/home/easysearch
ENV TARGET=${WORKSPACE}/file/target
ENV BASEPATH=${WORKSPACE}

COPY --chown=easysearch --from=Builder /EaseSearch-Import/import-task/target ${WORKSPACE}/target
COPY --chown=easysearch --from=Builder /jre ${WORKSPACE}/jre
COPY --chown=easysearch --from=Builder /docs-file/target ${WORKSPACE}/file/target

ENV JAVA_HOME=${WORKSPACE}/jre
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV MAPPING_PATH=${WORKSPACE}/target/classes/mapping.json

ARG GITEE_USER
ARG GITEE_PASSWORD

ENV GITEE_USER=${GITEE_USER}
ENV GITEE_PASSWORD=${GITEE_PASSWORD}

USER easysearch

CMD java -jar ${WORKSPACE}/target/import.jar


