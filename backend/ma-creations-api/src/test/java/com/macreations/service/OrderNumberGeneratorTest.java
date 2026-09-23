package com.macreations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.macreations.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderNumberGeneratorTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void generatesMacPrefixedUniqueLookingNumber() {
        when(orderRepository.existsByOrderNumber(anyString())).thenReturn(false);

        OrderNumberGenerator generator = new OrderNumberGenerator(orderRepository);
        String number = generator.nextOrderNumber();

        assertThat(number).startsWith("MAC-");
        assertThat(number).matches("MAC-\\d{8}-[0-9A-Z]{12}");
    }

    @Test
    void retriesWhenCollisionOccurs() {
        when(orderRepository.existsByOrderNumber(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        OrderNumberGenerator generator = new OrderNumberGenerator(orderRepository);
        String number = generator.nextOrderNumber();

        assertThat(number).startsWith("MAC-");
    }
}
