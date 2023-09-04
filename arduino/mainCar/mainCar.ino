#include <SPI.h>
#include "RF24.h"
#include <Stepper.h>
#include <ESP32Servo.h>

const int in1 = 32, in2 = 33, in3 = 25, in4 = 26; 
const int stepsPerRevolution = 2048;
Servo cameraServo, reloadServo;
Stepper myStepper(stepsPerRevolution, 27, 12, 14, 13);

RF24 rf24(17,5);
const byte addr[] = "1Node";
boolean isReload = false, isCharge = false, setCameraUp = false, setCameraDown = false;
int cameraAngle = 0, reloadCounter = 0;

void setup() {
  Serial.begin(115200);
  rf24.begin();
  rf24.setChannel(100);  // 設定頻道編號
  rf24.setPALevel(RF24_PA_MIN);
  rf24.setDataRate(RF24_2MBPS);
  rf24.openReadingPipe(1, addr);  // 開啟通道和位址
  rf24.startListening();  // 開始監聽無線廣播
  Serial.println("nRF24L01 ready!");

  pinMode(in1, OUTPUT);
  pinMode(in2, OUTPUT);
  pinMode(in3, OUTPUT);
  pinMode(in4, OUTPUT);
  pinMode(2, OUTPUT); //LED comm status

  digitalWrite(in1, 0);
  digitalWrite(in2, 0);
  digitalWrite(in3, 0);
  digitalWrite(in4, 0);
  digitalWrite(2, 0);
  myStepper.setSpeed(6);
  cameraServo.attach(16);
  cameraServo.write(0);
  delay(100);
  cameraServo.detach();
  reloadServo.attach(4);
  reloadServo.write(0);
  delay(100);
  reloadServo.detach();

  delay(1000);
  pinMode(21, OUTPUT); //fire 
  digitalWrite(21, 0);
  delay(50);
  pinMode(22, OUTPUT); //recharge
  digitalWrite(22, 0);
}

void loop() {
 if (rf24.available()) {
    char msg[32] = "";
    rf24.read(&msg, sizeof(msg));
    Serial.println(msg); // 顯示訊息內容
    operateCommand(msg); 
  }

  if(setCameraUp && cameraAngle<=25 ){
    rotateServo(cameraServo,16, cameraAngle, cameraAngle+1, 80);
    cameraAngle++;
    Serial.println(cameraAngle);
  }else if(setCameraDown && cameraAngle>=0){
    rotateServo(cameraServo,16, cameraAngle, cameraAngle-1, 80);
    cameraAngle--;
  }

  if(reloadCounter>=10){
    myStepper.step(16);
    reloadCounter = 0;
  }
}

void operateCommand(char msg[32]){
  if(strcmp(msg,"leftUp") == 0){
    digitalWrite(in1, 1);
    digitalWrite(in2, 0);
  }else if(strcmp(msg,"leftDown") == 0){
    digitalWrite(in1, 0);
    digitalWrite(in2, 1);
  }else if(strcmp(msg,"leftStop") == 0){
    digitalWrite(in1, 0);
    digitalWrite(in2, 0);
  }else if(strcmp(msg,"rightUp") == 0){
    digitalWrite(in3, 1);
    digitalWrite(in4, 0);
  }else if(strcmp(msg,"rightDown") == 0){
    digitalWrite(in3, 0);
    digitalWrite(in4, 1);
  }else if(strcmp(msg,"rightStop") == 0){
    digitalWrite(in3, 0);
    digitalWrite(in4, 0);
  }else if(strcmp(msg,"cameraUp") == 0){
    setCameraUp = true;
  }else if(strcmp(msg,"cameraDown") == 0){
    setCameraDown = true;
     Serial.println("conduct cameradown");
  }else if(strcmp(msg,"cameraStop") == 0){
    setCameraDown = false;
    setCameraUp = false;
  }else if(strcmp(msg,"leftRotate") == 0){
    myStepper.step(32);
  }else if(strcmp(msg,"rightRotate") == 0){
    myStepper.step(-32);
  }else if(strcmp(msg,"reload") == 0){
    reload();
  }else if(strcmp(msg,"recharge") == 0){
    recharge();
  }else if(strcmp(msg,"fire") == 0){
    fire();
  }else if (strcmp(msg,"BLE connect") == 0) {
    digitalWrite(2, 1);
  }else if (strcmp(msg,"BLE disconnect") == 0) {
    digitalWrite(2, 0);
  }
}

void reload(){
  operateCommand("leftStop");
  operateCommand("rightStop");
  cameraServo.attach(16);
  rotateServo(cameraServo,16,cameraAngle,10,60);
  myStepper.step(stepsPerRevolution/16);
  rotateServo(reloadServo,4,0,180,7);
  rotateServo(reloadServo,4,180,0,7);
  rotateServo(cameraServo,16,10,cameraAngle,100);
  isReload = true;
  reloadCounter++;
}

void recharge(){
  operateCommand("leftStop");
  operateCommand("rightStop");
  digitalWrite(22, 1);
  delay(2500);
  digitalWrite(22, 0);
  isCharge = true;
  delay(100);
}

void fire(){
  if(isCharge && isReload){
    operateCommand("leftStop");
    operateCommand("rightStop");
    digitalWrite(21,1);
    delay(100);
    digitalWrite(21,0);
    isCharge = false;
    isReload = false;
  }
}

void rotateServo(Servo controlServo, int pin, int beforeAngle, int targetAngle, int periodSpeed){
  controlServo.attach(pin);
  delay(10);
  if(beforeAngle<targetAngle){
    for(int i=beforeAngle;i<targetAngle;i++){
     controlServo.write(i);
     delay(periodSpeed);
    }
  }else if(beforeAngle>targetAngle){
    for(int i=beforeAngle;i>targetAngle;i--){
     controlServo.write(i);
     delay(periodSpeed);
    }
  }
  controlServo.detach();
}

