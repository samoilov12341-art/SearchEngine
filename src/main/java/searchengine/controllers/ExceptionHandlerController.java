package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.response.IndexingResponse;
import searchengine.exception.*;

@RestControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<IndexingResponse> indexingException(IndexingException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new IndexingResponse(false, e.getLocalizedMessage()));
    }

    @ExceptionHandler(InputException.class)
    public ResponseEntity<IndexingResponse> inputException(InputException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new IndexingResponse(false, e.getLocalizedMessage()));
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<IndexingResponse> searchException(SearchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new IndexingResponse(false, e.getLocalizedMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<IndexingResponse> notFoundException(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new IndexingResponse(false, e.getLocalizedMessage()));
    }
}
