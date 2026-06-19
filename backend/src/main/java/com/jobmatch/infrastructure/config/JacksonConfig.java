package com.jobmatch.infrastructure.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;

import java.io.IOException;

@Configuration
public class JacksonConfig {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Bean
    public Module customPageModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer((Class) Page.class, (JsonSerializer) new PageSerializer());
        return module;
    }

    static class PageSerializer extends JsonSerializer<Page<?>> {
        @Override
        public void serialize(Page<?> page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            gen.writeArrayFieldStart("content");
            for (Object item : page.getContent()) {
                gen.writeObject(item);
            }
            gen.writeEndArray();

            gen.writeNumberField("totalPages", page.getTotalPages());
            gen.writeNumberField("totalElements", page.getTotalElements());
            gen.writeNumberField("number", page.getNumber());
            gen.writeNumberField("size", page.getSize());
            gen.writeBooleanField("first", page.isFirst());
            gen.writeBooleanField("last", page.isLast());
            gen.writeBooleanField("empty", page.isEmpty());

            gen.writeEndObject();
        }
    }
}