package cn.xingbao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class XingbaoApplication {
  public static void main(String[] args) { SpringApplication.run(XingbaoApplication.class, args); }
}