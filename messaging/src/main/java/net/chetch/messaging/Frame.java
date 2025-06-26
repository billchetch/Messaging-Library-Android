package net.chetch.messaging;

import net.chetch.messaging.exceptions.FrameException;

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

    public static class FrameDimensions{
        public int schema; //nb this is the dimension NOT the schema value
        public int encoding;  //nb this is the dimension NOT the encoding value
        public int payloadSize; //number of bytes to use to specify payload size
        public int checksum; //number of bytes to reserve for the checksum value
        public int payload = -1; //the actual size of the payload

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

    public interface IFrameCompleteListener{
        void onComplete(byte[] payload);
    }

    public FrameSchema schema;

    public FrameDimensions dimensions;

    public MessageEncoding encoding = MessageEncoding.BYTES_ARRAY;

    public int maxPayload = -1;

    private ArrayList<Byte> bytes = new ArrayList<Byte>();

    private boolean complete = false;

    private IFrameCompleteListener frameCompleteListener;

    public Frame(FrameSchema schema, MessageEncoding encoding, IFrameCompleteListener listener){
        this.schema = schema;
        this.encoding = encoding;
        this.dimensions = new FrameDimensions(schema);
        this.frameCompleteListener = listener;
    }

    public  Frame(FrameSchema schema, MessageEncoding encoding){
        this(schema, encoding, null);
    }
    public Frame(FrameSchema schema){
        this(schema, MessageEncoding.BYTES_ARRAY, null);
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
        byte[] b2r = new byte[dimensions.payload];
        for(int i = 0; i < dimensions.payload; i++){
            b2r[i] = this.bytes.get(dimensions.getPayloadIndex() + i);
        }
        return b2r;
    }

    public byte[] getBytes(boolean addChecksum) throws Exception{
        if (dimensions.payload <= 0) throw new FrameException(FrameError.NO_PAYLOAD);

        byte[] b2r = new byte[dimensions.getSize()];

        if (addChecksum) {
            if(dimensions.checksum <= 0)throw new FrameException(FrameError.NO_DIMENSIONS, "No checksum dimension");
            switch (schema)
            {
                case SMALL_SIMPLE_CHECKSUM:
                case MEDIUM_SIMPLE_CHECKSUM:
                    int sum = 0;
                    int csumIdx = dimensions.getChecksumIndex();
                    for(int i = 0; i < csumIdx; i++){
                        b2r[i] = this.bytes.get(i);
                        sum += (int)(b2r[i] & 0xFF);
                    }
                    for(int i = 0; i < dimensions.checksum; i++){
                        byte b = (byte)(sum & 0xFF);
                        b2r[csumIdx + i] = b;
                        sum = sum >> 8;
                    }
                    break;
            }
        } else {
            for(int i = 0; i < this.bytes.size(); i++){
                b2r[i] = this.bytes.get(i);
            }
        }
        return b2r;
    }

    public byte[] getBytes() throws Exception{ return getBytes(true); }


    //Reading
    private boolean addByte(byte b) throws Exception
    {
        if (complete)
        {
            throw new Exception("Frame already complete");
        }

        if (this.bytes.isEmpty())
        {
            if(b != (byte)FrameSchema.valueOf(this.schema.toString()).ordinal())
            {
                throw new FrameException(FrameError.NON_VALID_SCHEMA, "received " + b + " which does not match this frames schema of " + schema);
            }
            //Console.WriteLine("Frame schema: {0}", b);
        }
        else if(this.bytes.size() == 1)
        {
            if(b!= (byte)MessageEncoding.valueOf(this.encoding.toString()).ordinal())
            {
                throw new FrameException(FrameError.NON_VALID_ENCODING, b + " is not a valid encoding");
            }
            //Console.WriteLine("Encoding: {0}", b);
        }
        else if (this.bytes.size() == dimensions.getPayloadIndex())
        {
            int n = 0;
            for(int i = 0; i < dimensions.payloadSize; i++){
                n += (int)(bytes.get(i + dimensions.getPayloadSizeIndex()) & 0xFF);
            }
            dimensions.payload = n;
            if(dimensions.payload <= 0)
            {   throw new FrameException(FrameError.INCOMPLETE_DATA, "Payload dimensions must be 1 or more");
            }
            else if(maxPayload > 0 && dimensions.payload > maxPayload)
            {
                throw new FrameException(FrameError.NON_VALID_PAYLOAD_SIZE, "Payload size of " + dimensions.payload + " must be less than or equal to max of " + maxPayload);
            }
            //Console.WriteLine("Payload size: {0}", Dimensions.Payload);
        }

        //Console.Write("{0},",b);
        this.bytes.add(b);
        complete = dimensions.payload > 0 && this.bytes.size() == dimensions.getSize();

        return complete;
    }

    public void add(byte[] bytes) throws Exception
    {
        for(byte b : bytes) {
            add(b);
        }
    }

    //Add a byte and capture some stuff
    public void add(byte b) throws Exception
    {
        if(addByte(b)){
            validate();
            if(this.frameCompleteListener != null){
                this.frameCompleteListener.onComplete(getPayload());
            }
            reset();
        }
    }

    public void reset()
    {
        complete = false;
        bytes.clear();
    }

    public void validate() throws Exception
    {
        if (!complete)
        {
            throw new FrameException(Frame.FrameError.INCOMPLETE_DATA);
        }
        if (bytes.size() < dimensions.getPayloadSizeIndex() + dimensions.payload || dimensions.payload == 0)
        {
            throw new FrameException( Frame.FrameError.NO_PAYLOAD);
        }
        if (bytes.size() < dimensions.getPayloadIndex())
        {
            throw new FrameException(Frame.FrameError.NO_HEADER);
        }

        //confirm checksum
        if (dimensions.checksum > 0)
        {
            switch (schema)
            {
                case SMALL_SIMPLE_CHECKSUM:
                case MEDIUM_SIMPLE_CHECKSUM:
                    //calculate the check sum
                    int sum = 0;
                    int csumIdx = dimensions.getChecksumIndex();
                    for(int i = 0; i < csumIdx; i++){
                        sum += (int)(this.bytes.get(i) & 0xFF);
                    }

                    //read the check sum
                    int csum = 0;
                    for(int i = 0; i < dimensions.checksum; i++){
                        csum += (int)(this.bytes.get(i + csumIdx) & 0xFF);
                    }

                    if(sum != csum){
                        String msg = "Supplied checksum " + csum + " != " + sum + " calculated checksum";
                        throw new FrameException(Frame.FrameError.CHECKSUM_FAILED, msg);
                    }
                    break;
            }
        }
    }
}
