package searchengine.dto.response;

import lombok.Data;
import org.jsoup.nodes.Element;

@Data
public class Snippet {

    private String site;

    private String siteName;

    private String uri;

    private String title;

    private String snippet;

    private Float relevance;
}
