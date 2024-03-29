package app.vacancies.grabber.utils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupTester {
    public static void main(String[] args) {
        String html = "<html><head><title>Sample Title</title></head>"
                + "<body>"
                + "<p>Sample Content</p>"
                + "<div id='sampleDiv'><a href='www.google.com'>Google</a>"
                + "<h3><a>Sample</a><h3>"
                + "</div>"
                + "</body></html>";
        Document document = Jsoup.parse(html);

        //a with href
        Element link = document.select("a").first();

        System.out.println("Href: " + link.attr("href"));
    }
}
