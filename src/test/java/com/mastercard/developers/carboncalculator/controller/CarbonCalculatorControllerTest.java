package com.mastercard.developers.carboncalculator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.mastercard.developers.carboncalculator.configuration.ApiConfiguration;
import com.mastercard.developers.carboncalculator.service.AddCardService;
import com.mastercard.developers.carboncalculator.service.EnvironmentalImpactService;
import com.mastercard.developers.carboncalculator.service.PaymentCardService;
import com.mastercard.developers.carboncalculator.service.ServiceProviderService;
import com.mastercard.developers.carboncalculator.service.SupportedParametersService;
import org.junit.jupiter.api.DisplayName;
import com.mastercard.developers.carboncalculator.service.*;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.EngagementServicesApi;
import org.openapitools.client.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import util.Resources;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mastercard.developers.carboncalculator.service.MockData.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class CarbonCalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private Gson gson ;

    @MockBean
    private EnvironmentalImpactService environmentalImpactService;

    @MockBean
    private ApiConfiguration apiConfiguration;

    @MockBean
    private SupportedParametersService supportedParametersService;

    @MockBean
    private PaymentCardService paymentCardService;

    @MockBean
    private AddCardService addCardService;

    @MockBean
    private ServiceProviderService serviceProviderApi;

    @MockBean
    private ServiceProviderConfig serviceProviderConfig;

    @MockBean
    private EngagementService engagementService;

    @MockBean
    private EngagementServicesApi engagementServicesApi;

    @Value("${test.data.bin}")
    String bin;

    @Value("${test.data.card-base-currency}")
    String cardBaseCurrency;

    private static final String CLIENTID = "cNU2Re-v0oKw95zjfs7G60yICaTtQtyEt-vKZrnjd34ea14e";
    private static final String ORIG_CLIENTID = "wfe232Re-v0oKw95zjfs7G60yICaTtQtyEt-vKZrnjd34ea14e";
    private static final String CHANNEL = "CC";
    private static final String CARBON_SCORES = "/demo/carbon-scores";

    ApiException apiException = new ApiException(400, "Bad Request");

    @Test
    void calculateFootprintsWithMccRequest() throws Exception {

        when(environmentalImpactService.calculateFootprints(any())).thenReturn(
                transactionFootprints());
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = objectMapper.writeValueAsString(transactions());

        MvcResult mvcResult =this.mockMvc.perform(post("/demo/transaction-footprints").contentType(
                MediaType.APPLICATION_JSON).content(
              jsonContent)).andExpect(
                status().isOk()).andReturn();


        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }
    
    @Test
    void calculateFootprintsWithAiiaRequest() throws Exception {

        List<TransactionData> mcTransactions = aiiaBasedRequest();

        when(environmentalImpactService.calculateFootprints(any())).thenReturn(
        		aiiaBasedTransactionFootprints());

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = objectMapper.writeValueAsString(mcTransactions);

        MvcResult mvcResult = this.mockMvc.perform(post("/demo/transaction-footprints").contentType(
                MediaType.APPLICATION_JSON).content(
                jsonContent)).andExpect(
                status().isOk()).andReturn();


        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    void calculateFootprintsWithException() throws Exception {

        when(environmentalImpactService.calculateFootprints(any())).thenThrow(
                apiException);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = objectMapper.writeValueAsString(transactions());

        MvcResult mvcResult =this.mockMvc.perform(post("/demo/transaction-footprints").contentType(
                MediaType.APPLICATION_JSON).content(
                jsonContent)).andExpect(
                status().isBadRequest()).andReturn();


        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    @DisplayName("Verify that carbon calculation controller sends proper response in the success scenario when product request and card brand provided")
    void testSuccessResponseWithCardBrandAndProductRequest() throws Exception {
        when(environmentalImpactService.calculateCarbonScoreFootprints(any(), anyString(), anyString(), anyString())).thenReturn(
                getValidMccResponse());

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = objectMapper.writeValueAsString(carbonScoreRequest());

        MvcResult mvcResult =this.mockMvc.perform(post(CARBON_SCORES).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent).header("X-MC-key", "value").header("x-openapi-clientid", CLIENTID)
                        .header("channel", CHANNEL).header("origMcApiClientId",ORIG_CLIENTID))
                .andExpect(status().isOk()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    @DisplayName("Verify Invalid MCC Response")
    void testInvalidMccResponse() throws Exception {
        when(environmentalImpactService.calculateCarbonScoreFootprints(any(), any(), any(), any())).thenReturn(
                getInvalidMccResponse());
        MvcResult mvcResult = this.mockMvc
                .perform(post(CARBON_SCORES).contentType("application/json").header("X-MC-Correlation-ID", " ")
                        .content(Resources.getFileAsString("carbon-score-request-invalid-mcc.json"))
                        .header("X-MC-key", "value").header("x-openapi-clientid", CLIENTID)
                        .header("channel", CHANNEL).header("origMcApiClientId",ORIG_CLIENTID))
                .andExpect(status().isOk()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    @DisplayName("Verify that carbon calculation controller sends Error Response when Web Service throws Exception")
    void testCarbonScoresException() throws Exception {

        when(environmentalImpactService.calculateCarbonScoreFootprints(any(), any(), any(), any())).thenThrow(apiException);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = objectMapper.writeValueAsString(mockCarbonScoreData());
        MvcResult mvcResult =this.mockMvc.perform(post(CARBON_SCORES).contentType(
                                MediaType.APPLICATION_JSON).content(jsonContent)
                        .header("x-openapi-clientid", CLIENTID)
                        .header("channel", CHANNEL).header("origMcApiClientId",ORIG_CLIENTID))
                .andExpect(status().isBadRequest()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    void getSupportedCurrencies() throws Exception {

        when(supportedParametersService.getSupportedCurrencies()).thenReturn(
                currencies());

        MvcResult mvcResult = this.mockMvc
                .perform(get("/demo/supported-currencies"))
                .andExpect(status().isOk()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    void getSupportedCurrenciesException() throws Exception {

        when(supportedParametersService.getSupportedCurrencies()).thenThrow(apiException);

        MvcResult mvcResult = this.mockMvc
                .perform(get("/demo/supported-currencies"))
                .andExpect(status().isBadRequest()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    void getSupportedMerchantCategories() throws Exception {

        when(supportedParametersService.getSupportedMerchantCategories()).thenReturn(
                merchantCategories());

        MvcResult mvcResult = this.mockMvc
                .perform(get("/demo/supported-mccs"))
                .andExpect(status().isOk()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    void getSupportedMerchantCategoriesException() throws Exception {

        when(supportedParametersService.getSupportedMerchantCategories()).thenThrow(
                apiException);

        MvcResult mvcResult = this.mockMvc
                .perform(get("/demo/supported-mccs"))
                .andExpect(status().isBadRequest()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);

    }

    @Test
    void registerPaymentCard() throws Exception {


        when(addCardService.registerPaymentCard(any())).thenReturn(
                paymentCardReference());

        MvcResult mvcResult = this.mockMvc.perform(post("/demo/payment-cards").contentType(
                MediaType.APPLICATION_JSON).content(
                gson.toJson(paymentCard()))).andExpect(
                status().isOk()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }

    @Test
    void registerPaymentCardException() throws Exception {


        when(addCardService.registerPaymentCard(any())).thenThrow(
                apiException);

        MvcResult mvcResult = this.mockMvc.perform(post("/demo/payment-cards").contentType(
                MediaType.APPLICATION_JSON).content(
                gson.toJson(paymentCard()))).andExpect(
                status().isBadRequest()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }

    @Test
    void aggregateTransactionFootprints() throws Exception {

        AggregateSearchCriteria aggregateSearchCriteria = new AggregateSearchCriteria().paymentCardIds(
                Collections.singletonList("paymentCardId")).aggregateType(1);

        when(environmentalImpactService.getPaymentCardAggregateTransactions(CLIENTID, aggregateSearchCriteria,
                CHANNEL, ORIG_CLIENTID)).thenReturn(
                new AggregateTransactionFootprints());

        MvcResult mvcResult2 = this.mockMvc
                .perform(post("/demo/payment-cards/transaction-footprints/aggregates")
                        .contentType(
                                MediaType.APPLICATION_JSON).content(
                                gson.toJson(aggregateSearchCriteria))
                        .header("x-openapi-clientid", CLIENTID)
                        .header("channel", CHANNEL).header("origMcApiClientId",ORIG_CLIENTID)).andExpect(
                        status().isOk()).andReturn();

        String response2 = mvcResult2.getResponse().getContentAsString();
        assertNotNull(response2);
    }

    @Test
    void aggregateTransactionFootprintsException() throws Exception {

        AggregateSearchCriteria aggregateSearchCriteria = new AggregateSearchCriteria().paymentCardIds(
                Collections.singletonList("paymentCardId")).aggregateType(1);

        when(environmentalImpactService.getPaymentCardAggregateTransactions(CLIENTID, aggregateSearchCriteria,
                CHANNEL, ORIG_CLIENTID)).thenThrow(
                new ApiException(400, "Bad Request"));

        MvcResult mvcResult2 = this.mockMvc
                .perform(post("/demo/payment-cards/transaction-footprints/aggregates")
                        .contentType(
                                MediaType.APPLICATION_JSON).content(
                                gson.toJson(aggregateSearchCriteria))
                        .header("x-openapi-clientid", CLIENTID)
                        .header("channel", CHANNEL).header("origMcApiClientId",ORIG_CLIENTID)).andExpect(
                        status().isBadRequest()).andReturn();

        String response2 = mvcResult2.getResponse().getContentAsString();
        assertNotNull(response2);
    }


    @Test
    void historicalTransactionFootprints() throws Exception {
        when(paymentCardService.getPaymentCardTransactionHistory("paymentCardId", "2020-09-19",
                "2020-10-01", 0,
                1)).thenReturn(
                new HistoricalTransactionFootprints());
        MvcResult mvcResult3 = this.mockMvc
                .perform(get("/demo/historical/{paymentcard_id}/transaction-footprints".replace("{paymentcard_id}",
                        "paymentCardId"))
                        .params(prepareRequestParams()))
                .andExpect(status().isOk()).andReturn();

        String response3 = mvcResult3.getResponse().getContentAsString();
        assertNotNull(response3);
    }

    @Test
    void historicalTransactionFootprintsException() throws Exception {
        when(paymentCardService.getPaymentCardTransactionHistory("paymentCardId", "2020-09-19",
                "2020-10-01", 0,
                1)).thenThrow(apiException);
        MvcResult mvcResult3 = this.mockMvc
                .perform(get("/demo/historical/{paymentcard_id}/transaction-footprints".replace("{paymentcard_id}",
                        "paymentCardId"))
                        .params(prepareRequestParams()))
                .andExpect(status().isBadRequest()).andReturn();

        String response3 = mvcResult3.getResponse().getContentAsString();
        assertNotNull(response3);
    }

    @Test
    void getServiceProvider() throws Exception {
        when(serviceProviderApi.getServiceProvider()).thenReturn(serviceProvider());

        MvcResult mvcResult = this.mockMvc
                .perform(get("/demo/service-providers"))
                .andExpect(status().isOk()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }

    @Test
    void getServiceProviderException() throws Exception {
        when(serviceProviderApi.getServiceProvider()).thenThrow(apiException);

        MvcResult mvcResult = this.mockMvc
                .perform(get("/demo/service-providers"))
                .andExpect(status().isBadRequest()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }

    @Test
    void deletePaymentCardsException() throws Exception {
        doThrow(apiException).when(paymentCardService).deletePaymentCards(any());

        this.mockMvc.perform(post("/demo/payment-card-deletions").contentType(
                MediaType.APPLICATION_JSON).content(
                gson.toJson(listPaymentCards()))).andExpect(
                status().isBadRequest()).andReturn();
    }

    @Test
    void deletePaymentCards() throws Exception {
        doNothing().when(paymentCardService).deletePaymentCards(any());

        this.mockMvc.perform(post("/demo/payment-card-deletions").contentType(
                MediaType.APPLICATION_JSON).content(
                gson.toJson(listPaymentCards()))).andExpect(
                status().isAccepted()).andReturn();
    }

    @Test
    void deletePaymentCard() throws Exception {
        doNothing().when(paymentCardService).deletePaymentCard(anyString(), anyString(), anyString(), anyString());

        this.mockMvc.perform(delete("/demo/service-providers/payment-cards/{paymentcard_id}"
                .replace("{paymentcard_id}", "paymentCardId")).header("x-openapi-clientid", CLIENTID)
                .header("channel", CHANNEL).header("origMcApiClientId",ORIG_CLIENTID))
                .andExpect(status().isAccepted()).andReturn();
    }

    @Test
    void deletePaymentCardException() throws Exception {
        doThrow(apiException).when(paymentCardService).deletePaymentCard(anyString(), anyString(), anyString(), anyString());

        this.mockMvc.perform(delete("/demo/service-providers/payment-cards/{paymentcard_id}"
                        .replace("{paymentcard_id}", "paymentCardId")).header("x-openapi-clientid", CLIENTID)
                        .header("channel", CHANNEL).header("origMcApiClientId",ORIG_CLIENTID))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    void updateServiceProvider() throws Exception {
        ServiceProviderConfig providerConfig = new ServiceProviderConfig();
        providerConfig.setCustomerName("customerName");
        providerConfig.setSupportedAccountRange("5425");
        when(serviceProviderApi.updateServiceProvider(providerConfig)).thenReturn(serviceProvider());

        MvcResult mvcResult = this.mockMvc
                .perform(put("/demo/service-providers").contentType(MediaType.APPLICATION_JSON)
                        .content(gson.toJson(providerConfig)))
                .andExpect(status().isOk()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }

    @Test
    void updateServiceProviderException() throws Exception {
        ServiceProviderConfig providerConfig = new ServiceProviderConfig();
        providerConfig.setCustomerName("customerName");
        providerConfig.setSupportedAccountRange("5425");
        when(serviceProviderApi.updateServiceProvider(providerConfig)).thenThrow(apiException);

        MvcResult mvcResult = this.mockMvc
                .perform(put("/demo/service-providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(providerConfig)))
                .andExpect(status().isBadRequest()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }


    private LinkedMultiValueMap<String, String> prepareRequestParams() {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("from_date", "2020-09-19");
        requestParams.add("to_date", "2020-10-01");
        requestParams.add("offset", "0");
        requestParams.add("limit", "1");
        return requestParams;
    }
    
    @Test
    void bulkRegistrationPaymentCards() throws Exception {


        when(addCardService.registerBatchPaymentCardsServiceProvider(listPaymentCardReference())).thenReturn(
                batchPaymentEnrollment());

        MvcResult mvcResult = this.mockMvc.perform(post("/demo/service-providers/payment-cards").contentType(
                MediaType.APPLICATION_JSON).content(
                gson.toJson(listPaymentCardReference())))
                .andExpect(status().isOk()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }

    @Test
    void bulkRegistrationPaymentCardsException() throws Exception {
        when(addCardService.registerBatchPaymentCardsServiceProvider(listPaymentCardReference())).thenThrow(
                apiException);

        MvcResult mvcResult = this.mockMvc.perform(post("/demo/service-providers/payment-cards").contentType(
                        MediaType.APPLICATION_JSON).content(
                        gson.toJson(listPaymentCardReference())))
                .andExpect(status().isBadRequest()).andReturn();

        String response = mvcResult.getResponse().getContentAsString();
        assertNotNull(response);
    }

    private CarbonScoreDetails getInvalidMccResponse(){
        CarbonScoreDetails scoreResponse = new CarbonScoreDetails();

        List<TransactionFootprintDetails> transactionDetailList = new ArrayList<>();
        TransactionFootprintDetails carbonScore = new TransactionFootprintDetails();
        carbonScore.setCarbonEmissionInGrams(null);
        carbonScore.setCarbonEmissionInOunces(null);
        carbonScore.setId("1");
        carbonScore.setMcc("1234");
        carbonScore.setCardBrand("MA");
        carbonScore.setCategory(null);
        carbonScore.setScoreStatus("FAILURE");
        carbonScore.setErrorCode("INVALID-MCC");
        carbonScore.setErrorMessage("Given merchant category code is not valid.");
        carbonScore.setScoreReference(null);
        transactionDetailList.add(carbonScore);
        TransactionFootprintDetails carbonScore1 = new TransactionFootprintDetails();
        carbonScore1.setCarbonEmissionInGrams(null);
        carbonScore1.setCarbonEmissionInOunces(null);
        carbonScore1.setId("2");
        carbonScore1.setMcc("5678");
        carbonScore1.setCardBrand("MA");
        carbonScore1.setCategory(null);
        carbonScore1.setScoreStatus("FAILURE");
        carbonScore1.setScoreReference(null);
        transactionDetailList.add(carbonScore1);
        scoreResponse.setTransactionFootprints(transactionDetailList);
        return scoreResponse;
    }

    private CarbonScoreDetails getValidMccResponse(){
        CarbonScoreDetails scoreResponse = new CarbonScoreDetails();

        List<TransactionFootprintDetails> transactionDetailList = new ArrayList<>();
        TransactionFootprintDetails carbonScore = new TransactionFootprintDetails();
        carbonScore.setCarbonEmissionInGrams(new BigDecimal(2582.11));
        carbonScore.setCarbonEmissionInOunces(new BigDecimal(91.08));
        carbonScore.setId("1");
        carbonScore.setMcc("3000");
        carbonScore.setCardBrand("MA");
        carbonScore.setScoreStatus("SUCCESS");
        carbonScore.setScoreReference("MCC");
        transactionDetailList.add(carbonScore);
        scoreResponse.setTransactionFootprints(transactionDetailList);
        return scoreResponse;
    }

    public static ScoreRequestDetails mockCarbonScoreData() {
        ScoreRequestDetails scoreRequestDetails = new ScoreRequestDetails();
        TransactionDetails transactionDetails  = new TransactionDetails();
        transactionDetails.mcc("3000").id("DVsJNvdSMX").amount(
                new Amount().currencyCode("USD").value(new BigDecimal(150)));
        scoreRequestDetails.addTransactionsItem(transactionDetails);
        return scoreRequestDetails;
    }
}
