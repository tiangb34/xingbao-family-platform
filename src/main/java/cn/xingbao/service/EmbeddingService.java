package cn.xingbao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文本嵌入服务
 * 调用豆包嵌入模型 API，将文本转换为向量
 * 用于向量相似度检索
 */
@Service
public class EmbeddingService {

  private final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
    .build();
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${ai.doubao.api-key:}")
  private String apiKey;

  @Value("${ai.doubao.embedding-url:https://ark.cn-beijing.volces.com/api/v3/embeddings}")
  private String embeddingUrl;

  @Value("${ai.doubao.embedding-model:doubao-embedding-vision-251215}")
  private String embeddingModel;

  /**
   * 将文本转换为向量
   *
   * @param text 输入文本
   * @return 向量数组（double[]）
   */
  public double[] generate(String text) {
    if (text == null || text.trim().isEmpty()) {
      return new double[0];
    }

    try {
      // 截断过长文本（嵌入模型通常有 token 限制）
      String truncated = text.length() > 2000 ? text.substring(0, 2000) : text;

      // 构建请求体（多模态格式）
      var body = Map.of(
        "model", embeddingModel,
        "input", List.of(Map.of("type", "text", "text", truncated))
      );

      String bodyJson = mapper.writeValueAsString(body);
      org.slf4j.LoggerFactory.getLogger(EmbeddingService.class).info("嵌入请求: url={}, model={}, body={}", embeddingUrl, embeddingModel, bodyJson);

      Request request = new Request.Builder()
        .url(embeddingUrl)
        .addHeader("Authorization", "Bearer " + apiKey)
        .addHeader("Content-Type", "application/json")
        .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
        .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "无响应体";
          throw new RuntimeException("嵌入 API 调用失败: " + response.code() + " - " + errorBody);
        }
        String responseBody = response.body().string();
        org.slf4j.LoggerFactory.getLogger(EmbeddingService.class).info("嵌入响应: {}", responseBody);
        JsonNode root = mapper.readTree(responseBody);
        JsonNode data = root.path("data");
        if (data.isEmpty()) {
          throw new RuntimeException("嵌入 API 返回空数据: " + responseBody);
        }
        JsonNode embedding = data.path("embedding");

        // 将 JSON 数组转为 double[]
        double[] vector = new double[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
          vector[i] = embedding.get(i).asDouble();
        }
        return vector;
      }
    } catch (Exception e) {
      throw new RuntimeException("生成文本向量失败: " + e.getMessage(), e);
    }
  }

  /**
   * 将向量数组转为 JSON 字符串（用于存储到数据库）
   */
  public String toJson(double[] vector) {
    try {
      return mapper.writeValueAsString(vector);
    } catch (Exception e) {
      throw new RuntimeException("向量序列化失败", e);
    }
  }

  /**
   * 将 JSON 字符串转为向量数组（用于从数据库读取）
   */
  public double[] fromJson(String json) {
    try {
      return mapper.readValue(json, double[].class);
    } catch (Exception e) {
      throw new RuntimeException("向量反序列化失败", e);
    }
  }

  /**
   * 计算两个向量的余弦相似度
   *
   * @param a 向量 a
   * @param b 向量 b
   * @return 相似度值（-1 到 1，越接近 1 越相似）
   */
  public double cosineSimilarity(double[] a, double[] b) {
    if (a.length != b.length || a.length == 0) {
      return 0.0;
    }

    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < a.length; i++) {
      dotProduct += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }

    double denominator = Math.sqrt(normA) * Math.sqrt(normB);
    return denominator == 0.0 ? 0.0 : dotProduct / denominator;
  }
}