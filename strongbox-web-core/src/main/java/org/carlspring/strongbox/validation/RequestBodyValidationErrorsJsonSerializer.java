package org.carlspring.strongbox.validation;


import static org.carlspring.strongbox.db.schema.Properties.NAME;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * @author Przemyslaw Fusik
 */
public class RequestBodyValidationErrorsJsonSerializer
        extends JsonSerializer<MultiValueMap<String, String>>
{

    @Override
    public void serialize(final MultiValueMap<String, String> value,
                          final JsonGenerator gen,
                          final SerializerProvider serializers)
            throws IOException
    {
        gen.writeStartArray();
        for (final Map.Entry<String, List<String>> entry : value.entrySet())
        {
            gen.writeStartObject();
            gen.writeFieldName(NAME);
            gen.writeString( entry.getKey());
            gen.writeFieldName("messages");
            gen.writeStartArray();
            for (final String entryValue : entry.getValue())
            {
                gen.writeString(entryValue);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
