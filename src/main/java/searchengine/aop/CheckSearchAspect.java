package searchengine.aop;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import searchengine.exception.IndexingException;
import searchengine.exception.InputException;
import java.util.concurrent.atomic.AtomicBoolean;

@Aspect
@Component
@RequiredArgsConstructor
public class CheckSearchAspect {

    @Setter
    public static AtomicBoolean indexingStatus = new AtomicBoolean(false);


    @Before(value = "@annotation(CheckSearch)")
    public void checkSearch(JoinPoint joinPoint) {
        String query = getString(joinPoint.getArgs()[0]);
        String site = getString(joinPoint.getArgs()[1]);
        if (indexingStatus.get()) {
            throw new IndexingException("Пожалуйста подождите, идёт индексация страниц");
        }
        if (query.isBlank()) {
            throw new InputException("Задан пустой поисковой запрос");
        }
    }

    private String getString(Object arg) {
        if (arg instanceof String) {
            return (String) arg;
        }
        return null;
    }
}
