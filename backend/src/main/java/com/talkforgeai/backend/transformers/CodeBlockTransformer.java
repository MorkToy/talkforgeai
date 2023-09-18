package com.talkforgeai.backend.transformers;

import com.talkforgeai.backend.transformers.dto.TransformerContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeBlockTransformer implements Transformer {

    @Override
    public String process(String content, TransformerContext context) {
        String regex = "```([a-z]*)\n([\\s\\S]*?)```";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String lang = matcher.group(1);
            String code = matcher.group(2);

            // HTML-Encode the code
//            code = code.replaceAll("&", "&amp;");
//            code = code.replaceAll("<", "&lt;");
//            code = code.replaceAll(">", "&gt;");
//            code = code.replaceAll("\"", "&quot;");
//            code = code.replaceAll("'", "&#039;");

            String langClass = "";
            if (lang != null && !lang.isEmpty()) {
                langClass = """
                        class="language-%s\"""".formatted(lang);
            }

            String processed = """
                    <pre><code %s>%s</code></pre>""".formatted(langClass, code);

            content = content.replace(matcher.group(), processed);
            System.out.println("Replaced: " + content);
        }

        return content;
    }
}
