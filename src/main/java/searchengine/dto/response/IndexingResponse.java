package searchengine.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexingResponse {

    private Boolean result;

    private String error;

    private Integer count;

    private List<Snippet> data;

    public IndexingResponse(Boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public IndexingResponse(Boolean result) {
        this.result = result;
    }

    public IndexingResponse(Boolean result, Integer count, List<Snippet> data) {
        this.result = result;
        this.count = count;
        this.data = data;
    }
}
