package com.dayan.food.controller;

import com.dayan.food.entity.vo.ApiErrorVO;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import com.dayan.food.config.RequestIdFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 审计项 5.8：数据库约束违规映射 409，超大上传映射 413（而非 500）。
 */
class ApiExceptionHandlerTests {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void dataIntegrityConflictMapsTo409() throws NoSuchMethodException {
        ResponseStatus status = ApiExceptionHandler.class
                .getMethod("handleConflict", DataIntegrityViolationException.class, HttpServletRequest.class)
                .getAnnotation(ResponseStatus.class);

        assertEquals(HttpStatus.CONFLICT, status.value());

        HttpServletRequest request = request();
        ApiErrorVO body = handler.handleConflict(new DataIntegrityViolationException("duplicate key"), request);
        assertNotNull(body.message());
        assertEquals("test-request", body.requestId());
    }

    @Test
    void oversizedUploadMapsTo413() throws NoSuchMethodException {
        ResponseStatus status = ApiExceptionHandler.class
                .getMethod("handlePayloadTooLarge", MaxUploadSizeExceededException.class, HttpServletRequest.class)
                .getAnnotation(ResponseStatus.class);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, status.value());

        ApiErrorVO body = handler.handlePayloadTooLarge(new MaxUploadSizeExceededException(1024L), request());
        assertNotNull(body.message());
    }

    @Test
    void responseStatusKeepsStatusAndReturnsStableErrorBody() {
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "同一幂等键不能用于不同内容"), request());

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("REQUEST_CONFLICT", response.getBody().code());
        assertEquals("test-request", response.getBody().requestId());
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(RequestIdFilter.ATTRIBUTE)).thenReturn("test-request");
        return request;
    }
}
