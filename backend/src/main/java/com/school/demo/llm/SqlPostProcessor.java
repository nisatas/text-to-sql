package com.school.demo.llm;

import org.springframework.stereotype.Component;

@Component
public class SqlPostProcessor {

    public String clean(String rawOutput) {
        if (rawOutput == null) {
            return null;
        }

        String cleaned = rawOutput.trim()
                .replace("```sql", "")
                .replace("```", "")
                .trim();

        int selectIndex = cleaned.toLowerCase().indexOf("select");
        if (selectIndex >= 0) {
            cleaned = cleaned.substring(selectIndex);
        }

        int semicolonIndex = cleaned.indexOf(";");
        if (semicolonIndex >= 0) {
            cleaned = cleaned.substring(0, semicolonIndex);
        }

        return cleaned.trim();
    }
}