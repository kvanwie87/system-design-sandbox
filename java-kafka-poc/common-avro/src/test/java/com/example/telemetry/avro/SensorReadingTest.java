package com.example.telemetry.avro;

import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SensorReadingTest {

    @Test
    void shouldSerializeAndDeserializeRoundtrip() throws IOException {
        SensorReading original = SensorReading.newBuilder()
                .setSensorId("sensor-001")
                .setTemperature(23.5)
                .setHumidity(65.2)
                .setTimestamp(Instant.now())
                .setLocation("warehouse-A")
                .build();

        // Serialize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<SensorReading> writer = new SpecificDatumWriter<>(SensorReading.class);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(original, encoder);
        encoder.flush();
        byte[] bytes = out.toByteArray();

        // Deserialize
        DatumReader<SensorReading> reader = new SpecificDatumReader<>(SensorReading.class);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(bytes), null);
        SensorReading deserialized = reader.read(null, decoder);

        // Assert roundtrip
        assertEquals(original.getSensorId(), deserialized.getSensorId());
        assertEquals(original.getTemperature(), deserialized.getTemperature(), 0.001);
        assertEquals(original.getHumidity(), deserialized.getHumidity(), 0.001);
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
        assertEquals(original.getLocation(), deserialized.getLocation());
    }

    @Test
    void shouldCreateWithAllFields() {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

        SensorReading reading = SensorReading.newBuilder()
                .setSensorId("temp-sensor-42")
                .setTemperature(98.6)
                .setHumidity(30.0)
                .setTimestamp(now)
                .setLocation("server-room-B2")
                .setBatteryLevel(72.5)
                .build();

        assertNotNull(reading);
        assertEquals("temp-sensor-42", reading.getSensorId().toString());
        assertEquals(98.6, reading.getTemperature(), 0.001);
        assertEquals(30.0, reading.getHumidity(), 0.001);
        assertEquals(now, reading.getTimestamp());
        assertEquals("server-room-B2", reading.getLocation().toString());
        assertEquals(72.5, reading.getBatteryLevel(), 0.001);
    }

    @Test
    void shouldDefaultBatteryLevelToNull() {
        SensorReading reading = SensorReading.newBuilder()
                .setSensorId("wired-sensor")
                .setTemperature(20.0)
                .setHumidity(50.0)
                .setTimestamp(Instant.now())
                .setLocation("datacenter")
                .build();

        assertNull(reading.getBatteryLevel());
    }
}
