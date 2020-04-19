package com.exchange.app;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

class RatesProviderTests {

    private static final String SEK = "SEK";
    private static final String USD = "USD";
    private static final String EUR = "EUR";

    private ForeignExchangeRatesApiClient apiClient;
    private RatesProvider provider;
    private Random random = new Random(System.nanoTime());

    @BeforeEach
    void setUp() {
        apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        provider = new RatesProvider(apiClient);
    }


    @Test
    @DisplayName("For default currency (EUR) returns USD rate")
    void test1() {

        //given
        ExchangeRates exchangeRates = initializeExchangeRates();
        Mockito.when(apiClient.getLatestRates()).thenReturn(exchangeRates);

        //when
        Double rateUSD = provider.getExchangeRateInEUR(Currency.getInstance(USD));

        //then
        assertThat(exchangeRates.get(USD)).isEqualTo(rateUSD);
    }

    @Test
    @DisplayName("For default currency (EUR) returns all rates")
    void test2() {
        //given
        ExchangeRates exchangeRates = initializeExchangeRates();
        Mockito.when(apiClient.getLatestRates()).thenReturn(exchangeRates);

        //when
        Double rateSEK = provider.getExchangeRateInEUR(Currency.getInstance(SEK));
        Double rateUSD = provider.getExchangeRateInEUR(Currency.getInstance(USD));

        //then
        assertAll(
                () -> assertEquals(exchangeRates.get(USD), rateUSD, "USD rate should be included"),
                () -> assertEquals(exchangeRates.get(SEK), rateSEK, "SEK rate should be included")
        );
    }

    @Test
    void shouldReturnCurrencyExchangeRatesForOtherCurrency() {
        //given
        ExchangeRates exchangeRates = initializeExchangeRates();
        List<String> currencies = Arrays.asList(EUR, SEK, USD);

        Mockito.when(apiClient.getLatestRates(anyString())).thenAnswer(
                new Answer<ExchangeRates>() {

                    @Override
                    public ExchangeRates answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object base = invocationOnMock.getArgument(0);
                        if (currencies.contains(base)) {
                            return exchangeRates;
                        } else {
                            throw new CurrencyNotSupportedException("Not supported: " + base);
                        }
                    }
                }
        );

        //when
        Double rate = provider.getExchangeRate(Currency.getInstance(SEK), Currency.getInstance(USD));

        //then
        assertThat(exchangeRates.get(SEK)).isEqualTo(rate);
    }

    @Test
    void shouldThrowExceptionWhenCurrencyNotSupported() {
        //given
        Mockito.when(apiClient.getLatestRates()).thenThrow(new IllegalArgumentException());

        //then
        CurrencyNotSupportedException actual =
                assertThrows(CurrencyNotSupportedException.class,
                        () -> provider.getExchangeRateInEUR(Currency.getInstance("CHF")));

        assertEquals("Currency is not supported: CHF", actual.getMessage());
    }

    @Test
    void shouldGetRatesOnlyOnce() {
        //given
        ForeignExchangeRatesApiClient apiClient = Mockito.mock(ForeignExchangeRatesApiClient.class);
        ExchangeRates exchangeRates = initializeExchangeRates();
        Mockito.when(apiClient.getLatestRates()).thenReturn(exchangeRates);

        RatesProvider provider = new RatesProvider(apiClient);

        //when
        provider.getExchangeRateInEUR(Currency.getInstance(SEK));

        //then
        Mockito.verify(apiClient).getLatestRates();
    }

    private ExchangeRates initializeExchangeRates() {
        Map<String, Double> rates = new HashMap<String, Double>() {};
        rates.put(USD, random.nextDouble());
        rates.put(SEK, random.nextDouble());
        return initializeExchangeRates(EUR, DateTime.now(), rates);
    }

    private ExchangeRates initializeExchangeRates(String base) {
        Map<String, Double> rates = new HashMap<String, Double>() {};
        rates.put(EUR, random.nextDouble());
        rates.put(SEK, random.nextDouble());
        return initializeExchangeRates(base, DateTime.now(), rates);
    }

    private ExchangeRates initializeExchangeRates(String base, DateTime date, Map<String, Double> rates) {
        return new ExchangeRates(base, date, rates);
    }

}