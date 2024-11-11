#include <DallasTemperature.h>
#include <OneWire.h>
#include <Wire.h>
#include "rgb_lcd.h"
#include <WiFi.h>
#include "PubSubClient.h"

rgb_lcd lcd;

#define OFF '0' 
#define ON  '1'
#define ST_IDLE 0
#define ST_VERIFICANDO_AGUA 1000
#define ST_VERIFICANDO_CAFE 3000
#define ST_VERIFICANDO_TE 5000
#define ST_VERIFICANDO_AZUCAR 7000
#define ST_READY 9000

#define EV_INTERRUPTOR_ON 10
#define EV_INTERRUPTOR_OFF 120
#define EV_AGUA_CALIENTE 20
#define EV_AGUA_FRIA 30
#define EV_HAY_CAFE 40
#define EV_NO_HAY_CAFE 50
#define EV_HAY_TE 70
#define EV_NO_HAY_TE 80
#define EV_HAY_AZUCAR 90
#define EV_NO_HAY_AZUCAR 100
#define EV_CONTINUE 110

#define TIME 1000000
#define CANT_VERIFICACIONES 5

void mostrarLCD();
void verificar_sensor_temperatura();
void verificar_sensor_distancia_cafe();
void verificar_sensor_distancia_te();
void verificar_sensor_distancia_azucar();
void verificarEncendido();
void verificar_continue();
void fsm();
void tomar_evento();
long leerSensorTemperatura();
long leerSensorUltrasonidoDistancia(int triggerPin, int echoPin);
void actualizar_relee(int estado);
void actualizar_estado_buzzer(int estado);
void mostrarLogs(String mensaje);
void publicar();
void start();

//const char* ssid        = "MovistarFibra-EB0550";
//const char* password    = "HzvL5DuMAkpq8Hvzbhsy";
const char* ssid        = "SO Avanzados";
const char* password    = "SOA.2019";
const char* mqttServer  = "broker.emqx.io";
const char* user_name   = "";
const char* user_pass   = "";

const char * topic_temp  = "/CoffeXpert/temperatura";
const char * topic_cafe = "/CoffeXpert/cafe";
const char * topic_azucar = "/CoffeXpert/azucar";
const char * topic_te = "/CoffeXpert/te";
const char * topic_boton = "/CoffeXpert/boton";
const char * topic_ready = "/CoffeXpert/ready";


const int PinTriggerCafe = 4;
const int PinTriggerTe = 5;
const int PinTriggerAzucar = 18;
const int PinEchoCafe = 19;
const int PinEchoTe = 12;
const int PinEchoAzucar = 14;
const int oneWireBus = 23;
const int PinRelay = 15;
const int PinBuzzer = 26;
const int PinButton = 13;

const int UMBRAL_AGUA = 28;
const int DISTANCIA_UMBRAL = 20;
const float calc_distancia = 0.01723;

int Frecuencia = 1300;
int TiempoBuzzer = 2000; 
int Frecuencia2 = 500;
int TiempoBuzzer2 = 2000; 


int indice = 0;
int estado_actual = 12312;
int evento_actual = 12312;

int tiempoObjetoCafe = 0;
int tiempoObjetoTe = 0;
int tiempoObjetoAzucar = 0;
int distanciaObjetoCafe = 0;
int distanciaObjetoTe = 0;
int distanciaObjetoAzucar = 0;
int temperaturaC = 0;

unsigned long lastDebounceTime = 0;
unsigned long debounceDelay = 500;

bool faltaCafe = false;
bool faltaTe = false;
bool faltaAzucar = false;
bool aguaFria = false;
bool desactivado = false;

bool alarmaAguaFriaActivada = false;
bool alarmaFaltaCafeActivada = false;
bool alarmaFaltaTeActivada = false;
bool alarmaFaltaAzucarActivada = false;

bool mensajeAguaFriaMostrado = false;
bool mensajeFaltaCafeMostrado = false;
bool mensajeFaltaTeMostrado = false;
bool mensajeFaltaAzucarMostrado = false;
bool mensajeReadyMostrado = false;
bool encendido = false;
String line1 = "";

OneWire oneWire(oneWireBus);
DallasTemperature sensors(&oneWire);
void(* funcReset) (void) = 0;

int port = 1883;
String stMac;
char mac[50];
char clientId[50];
long last_time= millis();

WiFiClient espClient;
PubSubClient client(espClient);

void (*verificar_sensor[CANT_VERIFICACIONES])() = {verificar_sensor_temperatura,verificar_sensor_distancia_cafe, verificar_sensor_distancia_te, verificar_sensor_distancia_azucar, verificar_continue};

void IRAM_ATTR handleButtonPress() {
    unsigned long currentTime = millis();
    if (currentTime - lastDebounceTime > debounceDelay) 
    {
      if (evento_actual == EV_INTERRUPTOR_OFF) 
      {
        evento_actual = EV_INTERRUPTOR_ON;
        desactivado = false;
      } 
      else 
      {
        evento_actual = EV_INTERRUPTOR_OFF;
      }
        lastDebounceTime = currentTime;
    }
}

void setup()
{
  attachInterrupt(PinButton, handleButtonPress, CHANGE);
  start();
}

void start()
{
  pinMode(PinTriggerCafe, OUTPUT);
  pinMode(PinTriggerTe, OUTPUT);
  pinMode(PinTriggerAzucar, OUTPUT);
  pinMode(PinEchoCafe, INPUT);
  pinMode(PinEchoTe, INPUT);
  pinMode(PinEchoAzucar, INPUT);
  pinMode(PinBuzzer, OUTPUT);
  pinMode(PinRelay, OUTPUT);
  pinMode(PinButton, INPUT);
  Serial.begin(300);

  sensors.begin();
  lcd.begin(16, 2);

  wifiConnect();
  client.setServer(mqttServer, port);
  client.setCallback(callback);
  estado_actual = ST_IDLE;
  evento_actual = EV_INTERRUPTOR_OFF;
  indice = 0;
}

void loop()
{
    fsm();
    client.loop();
}

void fsm()
{
  tomar_evento();

  if (!client.connected()) 
  {
    mqttReconnect();
  }
  delayMicroseconds(TIME);
  switch(estado_actual)
  {
    case ST_IDLE:
      switch(evento_actual)
      {
        case EV_INTERRUPTOR_ON: 
          actualizar_relee(LOW);
          actualizar_estado_buzzer(LOW);
          estado_actual = ST_VERIFICANDO_AGUA;
          desactivado = false;
          publicar();
          mostrarLCD();
        break;
        default:
          break;
      }
      break;
    case ST_VERIFICANDO_AGUA:
      switch(evento_actual)
      {
        case EV_AGUA_FRIA:
          aguaFria = true;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_AGUA;
          actualizar_relee(HIGH);
          actualizar_estado_buzzer(HIGH);
          break;
        case EV_AGUA_CALIENTE:
          aguaFria = false;
          mensajeAguaFriaMostrado = false;
          publicar();
          mostrarLCD();
          actualizar_relee(LOW);
          actualizar_estado_buzzer(LOW);
          estado_actual = ST_VERIFICANDO_CAFE;
          break;
        case EV_INTERRUPTOR_OFF:
          estado_actual = ST_IDLE;
          desactivado = true;
          actualizar_relee(LOW);
          actualizar_estado_buzzer(LOW);
          publicar();
          mostrarLCD();
          break;
        default:
          break;
      }
      break;
    case ST_VERIFICANDO_CAFE:
      switch(evento_actual)
      {
        case EV_NO_HAY_CAFE:
          faltaCafe = true;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_CAFE;
          actualizar_estado_buzzer(HIGH);
          break;
        case EV_HAY_CAFE:
          faltaCafe = false;
          mensajeFaltaCafeMostrado = false;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_TE;
          break;
        case EV_INTERRUPTOR_OFF:
          estado_actual = ST_IDLE;
          desactivado = true;
          publicar();
          mostrarLCD();
          break;
        default:
          break;
      }
      break;
    case ST_VERIFICANDO_TE:
      switch(evento_actual)
      {
        case EV_HAY_TE:
          faltaTe = false;
          mensajeFaltaTeMostrado = false;
          estado_actual =  ST_VERIFICANDO_AZUCAR;
          break;
        case EV_NO_HAY_TE:
          faltaTe = true;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_TE;
          actualizar_estado_buzzer(HIGH);
          break;
        case EV_INTERRUPTOR_OFF:
          desactivado = true;
          estado_actual = ST_IDLE;
          publicar();
          mostrarLCD();
          break;
        default:
          break;
      }
      break;
    case ST_VERIFICANDO_AZUCAR:
      switch(evento_actual)
      {
        case EV_HAY_AZUCAR:
          faltaAzucar = false;
          mensajeFaltaAzucarMostrado = false;
          publicar();
          mostrarLCD();
          estado_actual =  ST_READY;
          break;
        case EV_NO_HAY_AZUCAR:
          faltaAzucar = true;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_AZUCAR;
          actualizar_estado_buzzer(HIGH);
          break;
        case EV_INTERRUPTOR_OFF:
          estado_actual = ST_IDLE;
          desactivado = true;
          publicar();
          mostrarLCD();
          break;
        default:
          break;
      }
      break;
    case ST_READY:
      switch(evento_actual)
      {
        case EV_CONTINUE:
          publicar();
          mostrarLCD();
          actualizar_relee(LOW);
          actualizar_estado_buzzer(LOW);
          alarmaAguaFriaActivada = false;
          alarmaFaltaCafeActivada = false;
          alarmaFaltaTeActivada = false;
          alarmaFaltaAzucarActivada = false;
          break;
        case EV_NO_HAY_AZUCAR:
          mensajeReadyMostrado = false;
          faltaAzucar = true;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_AZUCAR;
          actualizar_estado_buzzer(HIGH);
          break;
        case EV_NO_HAY_TE:
          mensajeReadyMostrado = false;
          faltaTe = true;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_TE;
          actualizar_estado_buzzer(HIGH);
          break;
        case EV_NO_HAY_CAFE:
          mensajeReadyMostrado = false;
          faltaCafe = true;
          publicar();
          mostrarLCD();
          estado_actual = ST_VERIFICANDO_CAFE;
          actualizar_estado_buzzer(HIGH);
          break;
        case EV_AGUA_FRIA:
          mensajeReadyMostrado = false;
          aguaFria = true;
          publicar();
          mostrarLCD();
          actualizar_relee(HIGH);
          actualizar_estado_buzzer(HIGH);
          estado_actual = ST_VERIFICANDO_AGUA;
          break;
        case EV_INTERRUPTOR_OFF:
          mensajeReadyMostrado = false;
          estado_actual = ST_IDLE;
          actualizar_relee(LOW);
          actualizar_estado_buzzer(LOW);
          desactivado = true;
          publicar();
          mostrarLCD();
          break;
        default:
          break;
      }
      break;
    default:
      break;
  }
}

void tomar_evento()
{
  if(evento_actual == EV_INTERRUPTOR_OFF)
    return;
  verificar_sensor[indice]();
  if(evento_actual == EV_AGUA_CALIENTE || evento_actual == EV_HAY_CAFE || evento_actual == EV_HAY_TE || evento_actual == EV_HAY_AZUCAR || evento_actual== EV_CONTINUE)
    {
      indice = ++indice % CANT_VERIFICACIONES;
    } 
}

long leerSensorTemperatura()
{
  sensors.requestTemperatures();
  float temperaturaC = sensors.getTempCByIndex(0);
  return temperaturaC;
}

long leerSensorUltrasonidoDistancia(int triggerPin, int echoPin) 
{
  digitalWrite(triggerPin, LOW);
  digitalWrite(triggerPin, HIGH);
  digitalWrite(triggerPin, LOW);
  return pulseIn(echoPin, HIGH);
}

long leerSensorUltrasonidoDistanciaCafe()
{
  digitalWrite(PinTriggerCafe, LOW);
  delayMicroseconds(2);
  digitalWrite(PinTriggerCafe, HIGH);
  delayMicroseconds(10);
  digitalWrite(PinTriggerCafe, LOW);
  return pulseIn(PinEchoCafe, HIGH);
}

long leerSensorUltrasonidoDistanciaTe()
{
  digitalWrite(PinTriggerTe, LOW);
  delayMicroseconds(2);
  digitalWrite(PinTriggerTe, HIGH);
  delayMicroseconds(10);
  digitalWrite(PinTriggerTe, LOW);
  return pulseIn(PinEchoTe, HIGH);
}

long leerSensorUltrasonidoDistanciaAzucar()
{
  digitalWrite(PinTriggerAzucar, LOW);
  delayMicroseconds(2);
  digitalWrite(PinTriggerAzucar, HIGH);
  delayMicroseconds(10);
  digitalWrite(PinTriggerAzucar, LOW);
  return pulseIn(PinEchoAzucar, HIGH);
}

void verificar_sensor_temperatura()
{
  long temperatura = leerSensorTemperatura();
  if (temperatura >= UMBRAL_AGUA)
  {
    aguaFria = false;
    evento_actual = EV_AGUA_CALIENTE;
  } 
  else 
  {
    aguaFria = true;
    publicar();
    mostrarLCD();
    evento_actual = EV_AGUA_FRIA;
  }    
}

void verificar_sensor_distancia_cafe()
{
  tiempoObjetoCafe = leerSensorUltrasonidoDistanciaCafe();
  distanciaObjetoCafe = calc_distancia * tiempoObjetoCafe;
  if (distanciaObjetoCafe <= DISTANCIA_UMBRAL){
    faltaCafe = false; 
    evento_actual = EV_HAY_CAFE;
  }
  else
  {
    faltaCafe = true;
    publicar();
    mostrarLCD();
    evento_actual = EV_NO_HAY_CAFE;
  }   
}

void verificar_sensor_distancia_te()
{
  tiempoObjetoTe = leerSensorUltrasonidoDistanciaTe();
  distanciaObjetoTe = calc_distancia * tiempoObjetoTe;
  if (distanciaObjetoTe <= DISTANCIA_UMBRAL)
  {
    faltaTe = false;
    evento_actual = EV_HAY_TE;
  }
  else
  {
    faltaTe = true;
    publicar();
    mostrarLCD();
    evento_actual = EV_NO_HAY_TE;
  }
    
}

void verificar_sensor_distancia_azucar()
{
  tiempoObjetoAzucar = leerSensorUltrasonidoDistanciaAzucar();
  distanciaObjetoAzucar = calc_distancia * tiempoObjetoAzucar;
  if (distanciaObjetoAzucar <= DISTANCIA_UMBRAL)
  {
    faltaAzucar = false;
    evento_actual = EV_HAY_AZUCAR;
  } 
  else
  {
    faltaAzucar = true;
    publicar();
    mostrarLCD();
    evento_actual = EV_NO_HAY_AZUCAR;
  }  
}

void verificar_continue()
{
  evento_actual = EV_CONTINUE;
}

void actualizar_relee(int estado)
{
  digitalWrite(PinRelay, estado);
}

void actualizar_estado_buzzer(int estado)
{
  if (estado == HIGH)
  {
    if (aguaFria && !alarmaAguaFriaActivada)
    {
      tone(PinBuzzer, Frecuencia, TiempoBuzzer);
      alarmaAguaFriaActivada = true;
    }
    else if (faltaCafe && !alarmaFaltaCafeActivada)
    {
      tone(PinBuzzer, Frecuencia, TiempoBuzzer);
      alarmaFaltaCafeActivada = true;
    }
    else if (faltaTe && !alarmaFaltaTeActivada)
    {
      tone(PinBuzzer, Frecuencia, TiempoBuzzer);
      alarmaFaltaTeActivada = true;
    }
    else if (faltaAzucar && !alarmaFaltaAzucarActivada)
    {
      tone(PinBuzzer, Frecuencia, TiempoBuzzer);
      alarmaFaltaAzucarActivada = true;
    }
  }
  else
  {
    noTone(PinBuzzer);
  }
}

void mostrarLCD() {
  if(!desactivado){
    if(aguaFria && !mensajeAguaFriaMostrado)
    {
      lcd.clear();
      line1 = "Agua fria";
      mensajeAguaFriaMostrado= true;
      mensajeReadyMostrado = false;
      lcd.setCursor(0, 0);
      lcd.print(line1);
    }

    if(faltaCafe && !mensajeFaltaCafeMostrado) 
    {
      line1 = "NO HAY CAFE";
      lcd.clear();
      mensajeFaltaCafeMostrado = true;
      mensajeReadyMostrado = false;
      lcd.setCursor(0, 0);
      lcd.print(line1);
    }

    if(faltaTe && !mensajeFaltaTeMostrado) 
    {
      line1 = "NO HAY TE";
      lcd.clear();
      mensajeFaltaTeMostrado = true;
      mensajeReadyMostrado = false;
      lcd.setCursor(0, 0);
      lcd.print(line1);
    }

    if(faltaAzucar && !mensajeFaltaAzucarMostrado) 
    {
      line1 = "NO HAY AZUCAR";
      lcd.clear();
      mensajeFaltaAzucarMostrado = true;
      mensajeReadyMostrado = false;
      lcd.setCursor(0, 0);
      lcd.print(line1);
    }

    if(!aguaFria && !faltaCafe && !faltaTe && !faltaAzucar && !mensajeReadyMostrado){
      lcd.clear();
      line1 = "Listo para usar";
      mensajeReadyMostrado = true;
      lcd.setCursor(0, 0);
      lcd.print(line1);
    }
  }
  else
  {
    lcd.clear();
    line1 = "Desactivado";
    mensajeAguaFriaMostrado = false;
    mensajeFaltaCafeMostrado = false;
    mensajeFaltaTeMostrado = false;
    mensajeFaltaAzucarMostrado = false;
    mensajeReadyMostrado = false;
    lcd.setCursor(0, 0);
    lcd.print(line1);
  }
}

void wifiConnect()  
{
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) 
  {
    delay(500);
    Serial.print(".");
  }
}

//Funcion que reconecta el cliente, si pierde la conexion
void mqttReconnect() 
{
    if(!client.connected())
    {
      long r = random(1000);
      sprintf(clientId, "clientId-%ld", r);
      if (client.connect(clientId,user_name,user_pass)) 
	    {
        client.subscribe(topic_boton);
      } 
      else 
	    {
        delay(5000);
      }
  }
}

void callback(char* topic, byte* message, unsigned int length) 
{
  char cMessage=char(*message);

  Serial.print("Se recibio mensaje en el topico: ");
  Serial.println(topic);
  Serial.println(topic_boton);
  Serial.print("Mensaje Recibido: ");
  Serial.println(cMessage);
  Serial.println();
  
  if(strcmp(topic,topic_boton) == 0)
   {
    if (cMessage== OFF)
      {
      evento_actual = EV_INTERRUPTOR_OFF;
      encendido = false; 
      }
    else
      {
        evento_actual = EV_INTERRUPTOR_ON;
        estado_actual = ST_VERIFICANDO_AGUA;
        encendido = true;
        desactivado = false;
      }
   }

}
 
void verificarEncendido()
{
  if(encendido)
    evento_actual = EV_INTERRUPTOR_ON;
}

void publicar()
{
 const char * charMsgToSend;

  if(aguaFria && !mensajeAguaFriaMostrado)
    {
    charMsgToSend = "Agua Fria";
    client.publish(topic_temp,charMsgToSend);
    }
   if(faltaCafe && !mensajeFaltaCafeMostrado)
   {
    charMsgToSend = "Falta Cafe";
    client.publish(topic_cafe,charMsgToSend);
   }
   if(faltaTe && !mensajeFaltaTeMostrado)
   {
    charMsgToSend = "Falta Te";
    client.publish(topic_te,charMsgToSend);
   }
  if(faltaAzucar && !mensajeFaltaAzucarMostrado)
   {
    charMsgToSend = "Falta Azucar";
    client.publish(topic_azucar,charMsgToSend);
   }
   if(!aguaFria && !faltaCafe && !faltaTe && !faltaAzucar && !mensajeReadyMostrado)
   {
    charMsgToSend = "Ready";
    client.publish(topic_ready,charMsgToSend);
   }
}