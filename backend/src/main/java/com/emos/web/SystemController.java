package com.emos.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

  @GetMapping("/health")
  public SystemHealthResponse health() {
    return new SystemHealthResponse("UP");
  }

  public record SystemHealthResponse(String status) {}
}
