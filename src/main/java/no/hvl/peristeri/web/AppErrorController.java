package no.hvl.peristeri.web;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxReswap;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("${server.error.path:${error.path:/error}}")
public class AppErrorController implements ErrorController {

	private final ErrorAttributes errorAttributes;

	@RequestMapping
	public ModelAndView error(HttpServletRequest request, HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		Map<String, Object> attributes = errorAttributes.getErrorAttributes(
				new ServletWebRequest(request),
				ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE)
		);

		int statusCode = (int) attributes.getOrDefault("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
		HttpStatus status = HttpStatus.resolve(statusCode);
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		ModelAndView modelAndView = new ModelAndView(resolveViewName(statusCode, htmxRequest));
		if (htmxRequest.isHtmxRequest()) {
			htmxResponse.setReswap(HtmxReswap.outerHtml());
			htmxResponse.setRetarget("main");
			modelAndView.setStatus(HttpStatus.OK);
		} else {
			modelAndView.setStatus(status);
		}

		modelAndView.addAllObjects(attributes);
		return modelAndView;
	}

	private String resolveViewName(int statusCode, HtmxRequest htmxRequest) {
		if (htmxRequest.isHtmxRequest()) {
			return "error/error_fragment";
		}

		return switch (statusCode) {
			case 400 -> "error/400";
			case 404 -> "error/404";
			case 422 -> "error/422";
			case 500 -> "error/500";
			default -> "error";
		};
	}
}
