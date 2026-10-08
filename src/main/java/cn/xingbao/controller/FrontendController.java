package cn.xingbao.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller public class FrontendController { @GetMapping({"/admin","/admin/"}) String admin(){return "forward:/admin/index.html";} @GetMapping({"/app","/app/"}) String app(){return "forward:/app/index.html";} }
