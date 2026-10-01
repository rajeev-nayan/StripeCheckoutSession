package org.example.evaluations.evaluation.clients;

import org.example.evaluations.evaluation.dtos.SessionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import java.util.List;

@Component
public class StripePaymentGateway {

    @Value("${stripe.key}")
    public String apiKey;

    public SessionDto createSession(String successUrl, List<Long> amounts, List<String> productNames,
            List<Long> quantities) {
        validateRequest(amounts, productNames, quantities);

        Stripe.apiKey = apiKey;

        try {

            SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                    .setSuccessUrl(successUrl)
                    .setMode(SessionCreateParams.Mode.PAYMENT);

            for (int i = 0; i < amounts.size(); i++) {

                Price price = createPrice(
                        amounts.get(i),
                        productNames.get(i));

                sessionBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(price.getId())
                                .setQuantity(quantities.get(i))
                                .build());
            }

            Session session = Session.create(sessionBuilder.build());

            SessionDto response = new SessionDto();

            response.setUrl(session.getUrl());
            response.setId(session.getId());
            response.setTotal(session.getAmountTotal());
            response.setExpiry(session.getExpiresAt());

            return response;

        } catch (StripeException e) {
            throw new RuntimeException(
                    "Failed to create Stripe checkout session: " + e.getMessage(), e);
        }
    }

    private Price createPrice(Long amount, String productName) {

        try {

            PriceCreateParams params = PriceCreateParams.builder()
                    .setCurrency("usd")
                    .setUnitAmount(amount)
                    .setProductData(
                            PriceCreateParams.ProductData.builder()
                                    .setName(productName)
                                    .build())
                    .build();

            return Price.create(params);

        } catch (StripeException e) {
            throw new RuntimeException(
                    "Failed to create price for product: " + productName, e);
        }
    }

    private void validateRequest(
            List<Long> amounts,
            List<String> productNames,
            List<Long> quantities) {

        if (amounts == null || productNames == null || quantities == null) {
            throw new IllegalArgumentException("Input lists cannot be null");
        }

        if (amounts.size() != productNames.size()
                || amounts.size() != quantities.size()) {
            throw new IllegalArgumentException(
                    "Amounts, productNames and quantities must have the same size");
        }
    }
}
