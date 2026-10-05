package com.emos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

  @Test
  void modules_are_acyclic() {
    ApplicationModules.of(EmosApplication.class).verify();
  }
}
