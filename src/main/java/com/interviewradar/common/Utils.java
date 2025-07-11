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

    public static String extractJsonArray(String text) {
        String t = text.trim();
        int aStart = t.indexOf('[');
        int aEnd   = t.lastIndexOf(']');
        if (aStart >= 0 && aEnd > aStart) {
            return t.substring(aStart, aEnd + 1);
        }
        throw new IllegalArgumentException("文本中没有找到 JSON 数组");
    }

    /**
     * 将 float[] 向量转成 JSON 数组字符串，如 "[0.12, -0.56, …]"
     */
    public static String toJsonArray(float[] vector) {
        if (vector == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
