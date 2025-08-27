package com.krk.poc.crypto.client.dto;

public class EpinResponseDto {
   private String pinBlock;
   private String format;
   private String hsmCmd;
   private String keyRef;
   public EpinResponseDto() {}
   public String getPinBlock() { return pinBlock; }    public void setPinBlock(String pinBlock) { this.pinBlock = pinBlock; }
   public String getFormat() { return format; }        public void setFormat(String format) { this.format = format; }
   public String getHsmCmd() { return hsmCmd; }        public void setHsmCmd(String hsmCmd) { this.hsmCmd = hsmCmd; }
   public String getKeyRef() { return keyRef; }        public void setKeyRef(String keyRef) { this.keyRef = keyRef; }
}