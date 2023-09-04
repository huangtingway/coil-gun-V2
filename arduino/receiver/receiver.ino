#include <SPI.h>
#include "RF24.h"
#include <SoftwareSerial.h>

SoftwareSerial ble_device(5,4);
RF24 rf24(8,7);
const byte addr[] = "1Node";
const char startmsg[] = "start comm";
boolean isConnect = false;

void setup() {
  Serial.begin(115200);
  rf24.begin();
  rf24.setChannel(100);       // 設定頻道編號
  rf24.openWritingPipe(addr); // 設定通道位址
  rf24.setPALevel(RF24_PA_MIN);   // 設定廣播功率
  rf24.setDataRate(RF24_2MBPS); // 設定傳輸速率
  rf24.stopListening();       // 停止偵聽；設定成發射模式
  rf24.write(&startmsg, sizeof(startmsg));
  ble_device.begin(115200);
  pinMode(A0, INPUT); //BLE state
  pinMode(LED_BUILTIN, OUTPUT);
}

void loop() {
  if(Serial.available()){
    ble_device.write(Serial.read());
  }
  if(ble_device.available()){
    String getMsg = ble_device.readStringUntil('*');
    int str_len = getMsg.length() + 1; 
    char sendmsg[str_len];
    getMsg.toCharArray(sendmsg,str_len); 
    rf24.write(&sendmsg, sizeof(sendmsg));
    Serial.println(sendmsg);
  }

  if(analogRead(A0)>730 && !isConnect){
    Serial.println("BLE connect");
    char msg[] = "BLE connect";
    rf24.write(&msg,sizeof(msg));
    digitalWrite(LED_BUILTIN, 1);
    isConnect = true;
  }else if (analogRead(A0)<5 && isConnect) {
    Serial.println("BLE disconnect");
    char msg[] = "BLE disconnect";
    rf24.write(&msg,sizeof(msg));
    digitalWrite(LED_BUILTIN, 0);
    isConnect = false;
  }
}
