package com.interviewradar.llm;

import dev.langchain4j.model.chat.ChatModel;

 public interface MetadataChatModel extends ChatModel {
    String getModelName();
    String getModelVersion();
}
