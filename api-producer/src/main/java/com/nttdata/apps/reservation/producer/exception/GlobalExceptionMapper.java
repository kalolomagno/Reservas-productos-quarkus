package com.nttdata.apps.reservation.producer.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import com.nttdata.apps.reservation.producer.exception.InvalidClientCredentialsException;

import java.util.LinkedHashMap;
import java.util.Map;

public class GlobalExceptionMapper {

    private static final Logger LOG =
            Logger.getLogger(GlobalExceptionMapper.class);

    @ServerExceptionMapper
    public Response mapExternalProductNotFound(
            ExternalProductNotFoundException exception
    ) {
        return buildResponse(
                Response.Status.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ServerExceptionMapper
    public Response mapReservationNotFound(
            NotFoundException exception
    ) {
        return buildResponse(
                Response.Status.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ServerExceptionMapper
    public Response mapInvalidRequest(
            IllegalArgumentException exception
    ) {
        return buildResponse(
                Response.Status.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ServerExceptionMapper
    public Response mapExternalCatalogError(
            ExternalCatalogException exception
    ) {
        LOG.error("External catalog integration failed.", exception);

        return buildResponse(
                Response.Status.BAD_GATEWAY,
                "The external product catalog is unavailable."
        );
    }

    @ServerExceptionMapper
    public Response mapValidationError(
            ConstraintViolationException exception
    ) {
        Map<String, String> violations = new LinkedHashMap<>();

        exception.getConstraintViolations()
                .forEach(violation -> {
                    String field = getFieldName(
                            violation.getPropertyPath().toString()
                    );

                    violations.put(field, violation.getMessage());
                });

        ApiErrorResponse response = ApiErrorResponse.of(
                Response.Status.BAD_REQUEST,
                "Request validation failed.",
                violations
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
    }

    @ServerExceptionMapper
    public Response mapUnexpectedError(
            Exception exception
    ) {
        LOG.error("Unexpected application error.", exception);

        return buildResponse(
                Response.Status.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred."
        );
    }

    private Response buildResponse(
            Response.Status status,
            String message
    ) {
        return Response.status(status)
                .entity(ApiErrorResponse.of(status, message))
                .build();
    }

    private String getFieldName(String propertyPath) {
        int lastDotIndex = propertyPath.lastIndexOf('.');

        if (lastDotIndex < 0) {
            return propertyPath;
        }

        return propertyPath.substring(lastDotIndex + 1);
    }

    @ServerExceptionMapper
    public Response mapInvalidClientCredentials(
            InvalidClientCredentialsException exception
    ) {
        return buildResponse(
                Response.Status.UNAUTHORIZED,
                exception.getMessage()
        );
    }

}
