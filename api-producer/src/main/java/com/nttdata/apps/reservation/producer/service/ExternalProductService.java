package com.nttdata.apps.reservation.producer.service;

import com.nttdata.apps.reservation.producer.client.DummyJsonProductClient;
import com.nttdata.apps.reservation.producer.client.dto.DummyJsonProductResponse;
import com.nttdata.apps.reservation.producer.exception.ExternalCatalogException;
import com.nttdata.apps.reservation.producer.exception.ExternalProductNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class ExternalProductService {

    private final DummyJsonProductClient dummyJsonProductClient;

    public ExternalProductService(
            @RestClient DummyJsonProductClient dummyJsonProductClient
    ) {
        this.dummyJsonProductClient = dummyJsonProductClient;
    }

    public ExternalProduct getAvailableProduct(
            Integer externalProductId,
            Integer requestedQuantity
    ) {
        DummyJsonProductResponse product = getProduct(externalProductId);

        validateStock(product, requestedQuantity);

        return new ExternalProduct(
                product.id(),
                product.title(),
                product.stock()
        );
    }

    private DummyJsonProductResponse getProduct(Integer externalProductId) {
        try {
            return dummyJsonProductClient.findProductById(externalProductId);

        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null
                    && exception.getResponse().getStatus() == 404) {

                throw new ExternalProductNotFoundException(externalProductId);
            }

            throw new ExternalCatalogException(
                    "The external product catalog could not process the request.",
                    exception
            );

        } catch (ProcessingException exception) {
            throw new ExternalCatalogException(
                    "The external product catalog is unavailable.",
                    exception
            );
        }
    }

    private void validateStock(
            DummyJsonProductResponse product,
            Integer requestedQuantity
    ) {
        if (product == null || product.id() == null || product.title() == null) {
            throw new ExternalCatalogException(
                    "The external catalog returned an invalid product response."
            );
        }

        if (product.stock() == null || product.stock() < requestedQuantity) {
            throw new IllegalArgumentException(
                    "The requested quantity exceeds the available product stock."
            );
        }
    }
}
