package com.interviewradar.common;

public class Utils {
    /**
     * 从 LLM 返回的文本中提取首尾完整的大括号包裹的 JSON 对象或数组
     *
     * @param text 包含 JSON 的原始文本
     * @return 包裹在最外层花括号中的子串，或原始文本
     */
    public static String extractJson(String text) {
        String t = text.trim();
        // 先试试对象
        int oStart = t.indexOf('{');
        int oEnd   = t.lastIndexOf('}');
        if (oStart >= 0 && oEnd > oStart) {
            return t.substring(oStart, oEnd + 1);
        }
        // 再试试数组
        int aStart = t.indexOf('[');
        int aEnd   = t.lastIndexOf(']');
        if (aStart >= 0 && aEnd > aStart) {
            return t.substring(aStart, aEnd + 1);
        }
        return text;
    }

}
