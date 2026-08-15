package com.example.telemetry.avro;

import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates backward-compatible schema evolution.
 * v2 schema adds an optional 'batteryLevel' field with null default.
 * Consumers compiled against v1 (without batteryLevel) can still read v2 messages.
 */
class SchemaEvolutionTest {

    // Simulates the original v1 schema (no batteryLevel field)
    private static final String V1_SCHEMA_JSON = """
            {
              "type": "record",
              "name": "SensorReading",
              "namespace": "com.example.telemetry.avro",
              "fields": [
                {"name": "sensorId", "type": "string"},
                {"name": "temperature", "type": "double"},
                {"name": "humidity", "type": "double"},
                {"name": "timestamp", "type": {"type": "long", "logicalType": "timestamp-millis"}},
                {"name": "location", "type": "string"}
              ]
            }
            """;

    @Test
    void v2SchemaIsBackwardCompatibleWithV1() {
        Schema v1Schema = new Schema.Parser().parse(V1_SCHEMA_JSON);
        Schema v2Schema = SensorReading.getClassSchema();

        // BACKWARD compatibility: new schema (v2) can read data written with old schema (v1)
        SchemaCompatibility.SchemaPairCompatibility result =
                SchemaCompatibility.checkReaderWriterCompatibility(v2Schema, v1Schema);

        assertEquals(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE,
                result.getType(),
                "v2 schema should be backward compatible with v1");
    }

    @Test
    void v2MessageCanBeReadByV1Consumer() throws IOException {
        // Write a v2 message (with batteryLevel)
        SensorReading v2Reading = SensorReading.newBuilder()
                .setSensorId("battery-sensor")
                .setTemperature(25.0)
                .setHumidity(60.0)
                .setTimestamp(Instant.now())
                .setLocation("roof")
                .setBatteryLevel(85.5)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<SensorReading> writer = new SpecificDatumWriter<>(SensorReading.getClassSchema());
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(v2Reading, encoder);
        encoder.flush();
        byte[] v2Bytes = out.toByteArray();

        // Read with v1 schema (simulating a consumer that hasn't upgraded)
        Schema v1Schema = new Schema.Parser().parse(V1_SCHEMA_JSON);
        Schema v2Schema = SensorReading.getClassSchema();

        // Use schema resolution: reader uses v1, writer used v2
        DatumReader<SensorReading> reader = new SpecificDatumReader<>(v2Schema, v1Schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(v2Bytes), null);
        SensorReading readback = reader.read(null, decoder);

        // Core fields should be intact
        assertEquals("battery-sensor", readback.getSensorId().toString());
        assertEquals(25.0, readback.getTemperature(), 0.001);
        assertEquals(60.0, readback.getHumidity(), 0.001);
        assertEquals("roof", readback.getLocation().toString());
    }

    @Test
    void v1MessageCanBeReadByV2Consumer() throws IOException {
        // Simulate a v1 message (no batteryLevel) being read by v2 consumer
        Schema v1Schema = new Schema.Parser().parse(V1_SCHEMA_JSON);

        // Write using v1 schema manually (GenericRecord approach)
        org.apache.avro.generic.GenericRecord v1Record =
                new org.apache.avro.generic.GenericData.Record(v1Schema);
        v1Record.put("sensorId", "wired-sensor");
        v1Record.put("temperature", 22.0);
        v1Record.put("humidity", 45.0);
        v1Record.put("timestamp", Instant.now().toEpochMilli());
        v1Record.put("location", "basement");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<org.apache.avro.generic.GenericRecord> writer =
                new org.apache.avro.generic.GenericDatumWriter<>(v1Schema);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(v1Record, encoder);
        encoder.flush();
        byte[] v1Bytes = out.toByteArray();

        // Read with v2 schema (the current SensorReading class)
        Schema v2Schema = SensorReading.getClassSchema();
        DatumReader<SensorReading> reader = new SpecificDatumReader<>(v1Schema, v2Schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(v1Bytes), null);
        SensorReading readback = reader.read(null, decoder);

        // Core fields intact
        assertEquals("wired-sensor", readback.getSensorId().toString());
        assertEquals(22.0, readback.getTemperature(), 0.001);

        // batteryLevel should default to null (the v1 message didn't have it)
        assertNull(readback.getBatteryLevel());
    }

    @Test
    void v2MessageWithNullBatteryLevel() throws IOException {
        // v2 message without batteryLevel set (uses default null)
        SensorReading reading = SensorReading.newBuilder()
                .setSensorId("wired-sensor")
                .setTemperature(20.0)
                .setHumidity(50.0)
                .setTimestamp(Instant.now())
                .setLocation("datacenter")
                .build();

        assertNull(reading.getBatteryLevel());

        // Roundtrip
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<SensorReading> writer = new SpecificDatumWriter<>(SensorReading.class);
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(reading, encoder);
        encoder.flush();

        DatumReader<SensorReading> reader = new SpecificDatumReader<>(SensorReading.class);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(
                new ByteArrayInputStream(out.toByteArray()), null);
        SensorReading deserialized = reader.read(null, decoder);

        assertNull(deserialized.getBatteryLevel());
        assertEquals("wired-sensor", deserialized.getSensorId().toString());
    }
}
