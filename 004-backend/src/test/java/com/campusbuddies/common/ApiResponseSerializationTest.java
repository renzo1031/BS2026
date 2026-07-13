package com.campusbuddies.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class ApiResponseSerializationTest {
    @Test
    void keepsNullDataUnderGlobalNonNullPolicy() throws Exception {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();

        JsonNode json = mapper.readTree(mapper.writeValueAsBytes(ApiResponse.ok()));

        assertThat(json.has("data")).isTrue();
        assertThat(json.get("data").isNull()).isTrue();
    }
}
