package net.chetch.messaging;

public enum MessageEncoding {
    NOT_SET,
    SYSTEM_DEFINED, //the particulars are decided by the system being implemented (e.g. a single byte command)
    XML,
    QUERY_STRING,
    POSITONAL,
    BYTES_ARRAY,
    JSON
}
