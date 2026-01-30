package module.avs.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("request")
    public HttpServletRequest addRequest(HttpServletRequest request) {
        return request;
    }

    @ModelAttribute("currentUri")
    public String addCurrentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}