FROM openeuler/openeuler:22.03 as BUILDER

RUN cd / \
    && yum install -y wget \
    && wget https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jdk/x64/linux/OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz \
    && tar -zxvf OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz \
    && wget https://repo.huaweicloud.com/apache/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz \
    && tar -zxvf apache-maven-3.8.1-bin.tar.gz \
    && yum install -y git

COPY . /software-search-import

ENV JAVA_HOME=/jdk-17.0.10+7
ENV PATH=${JAVA_HOME}/bin:$PATH

ENV MAVEN_HOME=/apache-maven-3.8.1
ENV PATH=${MAVEN_HOME}/bin:$PATH

RUN cd /software-search-import \
    && mvn clean install package -Dmaven.test.skip

FROM openeuler/openeuler:22.03

RUN yum update -y \
    && yum install -y shadow

RUN groupadd -g 1001 easysearch \
    && useradd -u 1001 -g easysearch -s /bin/bash -m easysearch

ENV WORKSPACE=/home/easysearch

WORKDIR ${WORKSPACE}

COPY --chown=easysearch --from=Builder /software-search-import/target ${WORKSPACE}/target

RUN echo "umask 027" >> /home/easysearch/.bashrc \
    && source /home/easysearch/.bashrc \
    && chmod 550 -R /home/easysearch

RUN dnf install -y wget \
    && wget https://mirrors.tuna.tsinghua.edu.cn/Adoptium/17/jre/x64/linux/OpenJDK17U-jre_x64_linux_hotspot_17.0.10_7.tar.gz \
    && tar -zxvf OpenJDK17U-jre_x64_linux_hotspot_17.0.10_7.tar.gz

ENV JAVA_HOME=${WORKSPACE}/jdk-17.0.10+7-jre
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV LANG="C.UTF-8"
ENV NO_ID_USER=anonymous

EXPOSE 8080

USER easysearch

CMD java -jar ${WORKSPACE}/target/EaseSearch-data-import-easysoftware-0.0.1-SNAPSHOT.jar --spring.config.location=${APPLICATION_PATH}