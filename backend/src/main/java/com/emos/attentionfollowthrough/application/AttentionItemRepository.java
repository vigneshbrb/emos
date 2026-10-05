package com.emos.attentionfollowthrough.application;

import com.emos.attentionfollowthrough.domain.AttentionItem;

public interface AttentionItemRepository {
  void insertIfAbsent(AttentionItem item);
}
