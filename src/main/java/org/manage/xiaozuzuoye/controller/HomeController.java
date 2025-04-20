package org.manage.xiaozuzuoye.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
public class HomeController {
    
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("小组作业管理系统启动成功！");
    }
    
    @GetMapping("/index.html")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("小组作业管理系统启动成功！");
    }
} 