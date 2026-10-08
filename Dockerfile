# 编译阶段：带maven环境打包项目
FROM maven:3.8.8-openjdk-17 AS builder
WORKDIR /app
# 缓存maven依赖，加速构建
COPY pom.xml .
RUN mvn dependency:go-offline
# 复制源码并打包
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段：轻量JDK镜像，仅保留jar包
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
# 限制内存，适配Render免费实例PORT环境变量
ENTRYPOINT ["java","-Xmx256m","-jar","app.jar"]
