package no.hvl.peristeri.common.exception;

import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxResponse;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HtmxReswap;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

/**
 * Global exception handler for the application.
 * Handles exceptions thrown by controllers and returns appropriate error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Handles ResourceNotFoundException and returns a 404 Not Found response.
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ModelAndView handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request,
	                                                    HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		logger.error("Resource not found exception: {}", ex.getMessage());
		return buildErrorModelAndView(HttpStatus.NOT_FOUND, ex, request, "Ressursen ble ikke funnet", htmxRequest,
				htmxResponse);
	}

	/**
	 * Handles InvalidParameterException and returns a 400 Bad Request response.
	 */
	@ExceptionHandler(InvalidParameterException.class)
	public ModelAndView handleInvalidParameterException(InvalidParameterException ex, HttpServletRequest request,
	                                                    HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		logger.error("Invalid parameter exception: {}", ex.getMessage());
		return buildErrorModelAndView(HttpStatus.BAD_REQUEST, ex, request, "Ugyldig parameter", htmxRequest,
				htmxResponse);
	}

	/**
	 * Handles BusinessRuleViolationException and returns a 422 Unprocessable Entity response.
	 */
	@ExceptionHandler(BusinessRuleViolationException.class)
	public ModelAndView handleBusinessRuleViolationException(BusinessRuleViolationException ex,
	                                                         HttpServletRequest request,
	                                                         HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		logger.error("Business rule violation exception: {}", ex.getMessage());
		return buildErrorModelAndView(HttpStatus.UNPROCESSABLE_ENTITY, ex, request, "Forretningsregel brutt",
				htmxRequest, htmxResponse);
	}

	/**
	 * Handles all other PeristeriException instances and returns a 500 Internal Server Error response.
	 */
	@ExceptionHandler(PeristeriException.class)
	public ModelAndView handlePeristeriException(PeristeriException ex, HttpServletRequest request,
	                                             HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		logger.error("Peristeri exception: {}", ex.getMessage());
		return buildErrorModelAndView(HttpStatus.INTERNAL_SERVER_ERROR, ex, request, "Intern feil", htmxRequest,
				htmxResponse);
	}

	/**
	 * Handles all other exceptions and returns a 500 Internal Server Error response.
	 */
	@ExceptionHandler(Exception.class)
	public ModelAndView handleException(Exception ex, HttpServletRequest request,
	                                    HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		logger.error("Unexpected exception: {}", ex.getMessage(), ex);
		return buildErrorModelAndView(HttpStatus.INTERNAL_SERVER_ERROR, ex, request, "Uventet feil", htmxRequest,
				htmxResponse);
	}

	/**
	 * Builds a ModelAndView with error information.
	 */
	private ModelAndView buildErrorModelAndView(HttpStatus status, Exception ex, HttpServletRequest request,
	                                            String error,
	                                            HtmxRequest htmxRequest, HtmxResponse htmxResponse) {
		ModelAndView mav = new ModelAndView("error");
		if (htmxRequest.isHtmxRequest()) {
			mav.setViewName("error/error_fragment");
			htmxResponse.setReswap(HtmxReswap.outerHtml());
			htmxResponse.setRetarget("main");
			mav.setStatus(HttpStatus.OK);
		} else {
			mav.setStatus(status);
		}
		mav.addObject("timestamp", LocalDateTime.now());
		mav.addObject("status", status.value());
		mav.addObject("error", error);
		mav.addObject("message", ex.getMessage());
		mav.addObject("path", request.getRequestURI());
		return mav;
	}
}
