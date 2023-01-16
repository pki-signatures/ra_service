package com.ivan.ra.service.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.data.domain.PageImpl;

import java.io.IOException;

@JsonComponent
public class PageImplJacksonSerializer extends JsonSerializer<PageImpl<?>> {

    @Override
    public void serialize(PageImpl page, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("content", page.getContent());
        jsonGenerator.writeBooleanField("is_first_page", page.isFirst());
        jsonGenerator.writeBooleanField("is_last_page", page.isLast());
        jsonGenerator.writeNumberField("total_pages", page.getTotalPages());
        jsonGenerator.writeNumberField("total_elements", page.getTotalElements());

        jsonGenerator.writeNumberField("page_size", page.getSize());
        jsonGenerator.writeNumberField("page_number", page.getNumber());

        jsonGenerator.writeEndObject();
    }
}

