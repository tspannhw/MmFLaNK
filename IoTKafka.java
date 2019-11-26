package dev.datainmotion.iot;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
/**
 * data source:   hdfs, kafka
 *
 * data sink:   kafka, hdfs, hbase
 */
public class IoTKafka {

    /**
     *
     */
    public static class NotNullFilter implements FilterFunction<String> {
        @Override
        public boolean filter(String string) throws Exception {
            if ( string == null || string.isEmpty() || string.trim().length() <=0) {
                return false;
            }

            return true;
        }
    }
    /**
     * main
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "princeton0.field.hortonworks.com:9092");
        properties.setProperty("group.id","flinkKafkaGroup");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>("iot", new SimpleStringSchema(), properties);
        DataStream<String> source = env.addSource(consumer).name("Flink IoT Kafka Source");
        // debug to cluster logs   source.print();

        source.filter(new NotNullFilter());

        source.countWindowAll()
        // Kafka Sink
        source.addSink(new FlinkKafkaProducer<>("princeton0.field.hortonworks.com:9092", "iot-result",
                new org.apache.flink.api.common.serialization.SimpleStringSchema())).name("Flink Kafka IoT Result Sink");

        // HDFS Sink
        //                 .forRowFormat(new Path("/tmp/kafka-loader"), new SimpleStringEncoder<String>())

        //                .withBucketAssigner(new DateTimeBucketAssigner())
//        StreamingFileSink<String> sink = StreamingFileSink
//                .forRowFormat(new Path("/tmp/kafka-loader"), new SimpleStringEncoder<String>())
//                .build();
//        source.addSink(sink);

        /**
         *
         * 		source.addSink(
         * 				StreamingFileSink.forBulkFormat(
         * 						Path.fromLocalFile(folder),
         * 						ParquetAvroWriters.forSpecificRecord(Address.class))
         * 				.build());
         */

        env.execute("Flink Streaming Kafka");
    }
}

