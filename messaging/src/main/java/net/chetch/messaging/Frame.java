package net.chetch.messaging;

import java.util.ArrayList;
import java.util.List;

public class Frame {

    public enum FrameSchema {
        NONE,
        SMALL_NO_CHECKSUM,      //FrameSchema = 1 byte, Encoding = 1 byte, Payload size = 1 byte, Payload = max 255 bytes
        SMALL_SIMPLE_CHECKSUM,      //FrameSchema = 1 byte, Encoding = 1 byte, Payload size = 1 byte, Payload = max 255 bytes, Checksum = 1 byte
        MEDIUM_NO_CHECKSUM,         //FrameSchema = 1 byte, Encoding = 1 byte, Payload size = 2 bytes, Payload = max 32K bytes
        MEDIUM_SIMPLE_CHECKSUM      //FrameSchema = 1 byte, Encoding = 1 byte, Payload size = 2 bytes, Payload = max 32K bytes, Checksum = 4 bytes
    }

    public enum FrameError {
        NO_ERROR,
        NO_DIMENSIONS,
        NO_HEADER,
        NO_PAYLOAD,
        INCOMPLETE_DATA,
        NON_VALID_SCHEMA,
        NON_VALID_ENCODING,
        CHECKSUM_FAILED,
        ADD_TIMEOUT,
        NON_VALID_PAYLOAD_SIZE
    }

    public class FrameDimensions{
        public int schema; //nb this is the dimension NOT the schema value
        public int encoding;  //nb this is the dimension NOT the encoding value
        public int payloadSize;
        public int checksum;
        public int payload = -1;

        public FrameDimensions(FrameSchema schema) {
            this.schema = 1; //nb this is the dimension NOT the schema value
            encoding = 1; //nb this is the dimension NOT the encoding value
            switch (schema)
            {
                case SMALL_NO_CHECKSUM:
                case SMALL_SIMPLE_CHECKSUM:
                    payloadSize = 1;
                    checksum = schema == FrameSchema.SMALL_SIMPLE_CHECKSUM ? 1 : 0;
                    break;

                case MEDIUM_NO_CHECKSUM:
                case MEDIUM_SIMPLE_CHECKSUM:
                    payloadSize = 2;
                    checksum = schema == FrameSchema.MEDIUM_SIMPLE_CHECKSUM ? 1 : 0;
                    break;
            }
        }

        public int getSchemaIndex(){ return 0; }
        public int getEncodingIndex(){ return getSchemaIndex() + schema; }
        public int getPayloadSizeIndex() { return getEncodingIndex() + encoding; }
        public int getPayloadIndex(){ return getPayloadSizeIndex() + payloadSize; }
        public int getChecksumIndex() throws Exception {
            if (checksum <= 0) throw new Exception("There is no checksum for this frame schema");
            if (payload <= 0) throw new Exception("Payload dimension has no value");
            return getPayloadIndex() + payload;
        }
        public int getSize() {
            if (payload <= 0) return -1;
            return schema + encoding + payloadSize + payload + checksum;
        }
    }

    public FrameSchema schema;

    public FrameDimensions dimensions;

    public MessageEncoding encoding = MessageEncoding.BYTES_ARRAY;

    private ArrayList<Byte> bytes = new ArrayList<Byte>();


    public Frame(FrameSchema schema, MessageEncoding encoding){
        this.schema = schema;
        this.encoding = encoding;
        this.dimensions = new FrameDimensions(schema);
    }
    public Frame(FrameSchema schema){
        this(schema, MessageEncoding.BYTES_ARRAY);
    }

    public void setPayload(byte[] bytes){
        //basic reset - keep things simple
        this.bytes.clear();
        this.bytes.add((byte)FrameSchema.valueOf(this.schema.toString()).ordinal());
        this.bytes.add((byte)MessageEncoding.valueOf(this.encoding.toString()).ordinal());

        //This is where the work starts ... add the payload size first
        int n = bytes.length;
        for(int i = 0; i < dimensions.payloadSize; i++){
            byte b = (byte)(n & 0xFF);
            this.bytes.add(b);
            n = n >> 8;
        }

        //now add the payload
        for (byte b : bytes) {
            this.bytes.add(b);
        }

        //update the frame dimensions to include the payload size
        dimensions.payload = bytes.length;
    }

    public byte[] getPayload(){

    }
}
