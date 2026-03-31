# ─────────────────────────────────────────────────────────────
# 多阶段构建 Dockerfile
#
# 阶段一（build）：使用 Maven 镜像编译整个多模块项目
# 阶段二（runtime）：使用精简 JRE 镜像运行最终 Fat JAR
# ─────────────────────────────────────────────────────────────

# ─── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace

# 先只复制 POM 文件，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
COPY opencloud-common/pom.xml     opencloud-common/
COPY opencloud-auth/pom.xml       opencloud-auth/
COPY opencloud-system/pom.xml     opencloud-system/
COPY opencloud-monitor/pom.xml    opencloud-monitor/
COPY opencloud-cloud/pom.xml      opencloud-cloud/
COPY opencloud-web/pom.xml        opencloud-web/

# 下载所有依赖（利用缓存层，代码未变动时跳过此步骤）
RUN mvn dependency:go-offline -B --no-transfer-progress

# 复制全部源码
COPY opencloud-common/src     opencloud-common/src
COPY opencloud-auth/src       opencloud-auth/src
COPY opencloud-system/src     opencloud-system/src
COPY opencloud-monitor/src    opencloud-monitor/src
COPY opencloud-cloud/src      opencloud-cloud/src
COPY opencloud-web/src        opencloud-web/src

# 构建（跳过测试，加速 CI/CD）
RUN mvn clean package -pl opencloud-web -am -DskipTests -B --no-transfer-progress

# ─── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# 安全：以非 root 用户运行
RUN addgroup -S opencloud && adduser -S opencloud -G opencloud

WORKDIR /app

# 从构建阶段复制 Fat JAR
COPY --from=build /workspace/opencloud-web/target/opencloud-web-*.jar app.jar

# 日志目录（挂载时权限正确）
RUN mkdir -p /logs/opencloud && chown -R opencloud:opencloud /logs /app

USER opencloud

# JVM 调优参数（容器感知内存，G1 GC，优雅停机）
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=70.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai"

EXPOSE 8080

# 健康检查（配合 docker-compose depends_on）
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar $@", "--"]
