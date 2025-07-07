package com.interviewradar.llm;

public interface LanguageModel {
    /**
     * 用给定的 prompt（包含模板+变量）向底层大模型发起请求，
     * 返回原始字符串结果。
     */
    String generate(String prompt);
}
