import org.apache.commons.io.FileUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Test {




    public static void main(String[] args) throws IOException {

        String fp = "C:\\CYDev\\workspace\\docs\\content\\zh\\docs\\DatabaseReference\\设置.md";
        File file = new File(fp);
        String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(fileContent);
        Document node = Jsoup.parse(renderer.render(document));


        if (node.getElementsByTag("a").size() > 0 && node.getElementsByTag("ul").size() > 0) {
            Element a = node.getElementsByTag("a").first();
            if (a.attr("href").startsWith("#")) {
                node.getElementsByTag("ul").first().remove();
            }
        }
        String textContent = node.toString();

        Elements h1 = node.getElementsByTag("h1");
        System.out.println(h1.text());
        Elements h2 = node.getElementsByTag("h2");
        System.out.println(h2.text());
        Elements h3 = node.getElementsByTag("h3");
        System.out.println(h3.text());


        String tapt = "C:\\CYDev\\workspace\\vs\\tr.html";
        FileWriter writer = new FileWriter(tapt);
        writer.write(textContent);
        writer.close();
    }
}
