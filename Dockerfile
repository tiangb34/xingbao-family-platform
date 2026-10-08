# 编译阶段：带maven环境打包项目
FROM maven:3.8.8-eclipse-temurin-17 AS builder
WORKDIR /app

# 缓存maven依赖，加速构建
COPY pom.xml .
RUN mvn dependency:go-offline -Dmaven.repo.local=.m2/repository

# 复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true -Dmaven.repo.local=.m2/repository

# 运行阶段：轻量Alpine JRE，适配Render免费实例
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 声明服务端口
EXPOSE 8080

# 复制打包产物
COPY --from=builder /app/target/*.jar app.jar

# 限制堆内存，读取Render分配的PORT环境变量
ENTRYPOINT java -Xms128m -Xmx256m -jar app.jar --server.port=$PORT
