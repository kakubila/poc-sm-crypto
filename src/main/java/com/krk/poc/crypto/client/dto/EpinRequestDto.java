package com.krk.poc.crypto.client.dto;

public class EpinRequestDto {
   private String pan;
   private String pin;
   private String keySlot;
   private String keyName;
   private String zpkHex;
   public EpinRequestDto() {}
   public EpinRequestDto(String pan, String pin, String keySlot, String keyName, String zpkHex) {
      this.pan = pan; this.pin = pin; this.keySlot = keySlot; this.keyName = keyName; this.zpkHex = zpkHex;
   }
   public String getPan() { return pan; }    public void setPan(String pan) { this.pan = pan; }
   public String getPin() { return pin; }    public void setPin(String pin) { this.pin = pin; }
   public String getKeySlot() { return keySlot; }    public void setKeySlot(String keySlot) { this.keySlot = keySlot; }
   public String getKeyName() { return keyName; }    public void setKeyName(String keyName) { this.keyName = keyName; }
   public String getZpkHex() { return zpkHex; }    public void setZpkHex(String zpkHex) { this.zpkHex = zpkHex; }
}

